package com.example.nexustime.core.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/*
 * Delegado de DataStore a nivel de fichero: debe ser `by preferencesDataStore`
 * sobre una propiedad de extensión de Context, y solo puede declararse UNA vez
 * por nombre en todo el proceso. El nombre es el del fichero en disco.
 */
private val Context.nexusDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "nexustime_settings")

/**
 * Eventos de estado que pueden generar un aviso parlante de carga.
 *
 * Cada uno lleva su clave de preferencia y su frase por defecto: así el receptor
 * resuelve la frase con una sola llamada y la edición de textos (Ajustes) solo
 * tiene que escribir en esa clave.
 */
enum class PowerAlertEvent(
    val prefKey: String,
    val defaultPhrase: String,
) {
    CONNECTED(
        prefKey = "phrase_power_connected",
        defaultPhrase = "Cargador conectado. Iniciando carga.",
    ),
    DISCONNECTED(
        prefKey = "phrase_power_disconnected",
        defaultPhrase = "Desconectado de la corriente.",
    ),
    BATTERY_LOW(
        prefKey = "phrase_battery_low",
        defaultPhrase = "Batería baja. Conecta el cargador.",
    ),
    BATTERY_FULL(
        prefKey = "phrase_battery_full",
        defaultPhrase = "Batería completa.",
    ),
}

/** Formato del reloj de la zona central. */
enum class HourFormat {
    /** Sigue la preferencia del sistema (12 h / 24 h). */
    SYSTEM,

    /** Fuerza 12 h con meridiano. */
    H12,

    /** Fuerza 24 h. */
    H24,
}

/** Política de encendido de la pantalla. */
enum class ScreenOnMode {
    /** La pantalla nunca se apaga mientras la app está en primer plano. */
    ALWAYS,

    /** La pantalla solo se mantiene encendida mientras el dispositivo carga. */
    CHARGING_ONLY,
}

/**
 * Preferencias de NexusTime sobre **Jetpack DataStore (Preferences)**.
 *
 * DataStore es la fuente de verdad de v1.0: sustituye a SharedPreferences y da
 * garantías de atomicidad y consistencia en disco que el fichero XML no daba.
 *
 * El matiz importante es que DataStore es **asíncrono por diseño**, mientras que
 * un `BroadcastReceiver.onReceive()` es síncrono y de ventana muy corta. Por eso
 * la API se parte en dos caras:
 *
 *  * **Reactiva ([StateFlow])** para la UI: se construyen con `stateIn` sobre
 *    `dataStore.data` y solo se materializan al ser accedidas (son `lazy`), ya
 *    que `initialize()` debe haber corrido antes de tocar el store.
 *  * **Suspend** para los receptores: `isPowerAlertsEnabled()`, `phrase()`, etc.
 *    Se leen con `data.first()`. Los receivers las invocan dentro de
 *    `BroadcastReceiver.goAsync()`, que extiende la ventana de entrega para
 *    poder terminar la lectura antes de `finish()`.
 *
 * Es singleton (sin Hilt): la instancia vive mientras vive el proceso y solo
 * retiene el DataStore del Application.
 */
object NexusSettings {

    // --- Claves de DataStore ---
    private val KEY_POWER_ALERTS = booleanPreferencesKey("power_alerts_enabled")
    private val KEY_BATTERY_LOW_THRESHOLD = intPreferencesKey("battery_low_threshold_percent")

    private val KEY_CHIME_ENABLED = booleanPreferencesKey("chime_enabled")
    private val KEY_CHIME_INTERVAL = intPreferencesKey("chime_interval_minutes")
    private val KEY_CHIME_TEMPLATE = stringPreferencesKey("chime_phrase_template")

    private val KEY_HOUR_FORMAT = stringPreferencesKey("hour_format")
    private val KEY_SCREEN_ON_MODE = stringPreferencesKey("screen_on_mode")

    // --- Valores por defecto ---
    const val DEFAULT_POWER_ALERTS = true
    const val DEFAULT_BATTERY_LOW_THRESHOLD = 15
    const val DEFAULT_CHIME_ENABLED = false

    /** Intervalos permitidos para el anuncio horario, en minutos. */
    val CHIME_INTERVALS = listOf(30, 60)
    const val DEFAULT_CHIME_INTERVAL = 60

    /** Plantilla del anuncio. `{hora}` se sustituye por la hora hablada. */
    const val CHIME_HOUR_PLACEHOLDER = "{hora}"
    const val DEFAULT_CHIME_TEMPLATE = "Son las $CHIME_HOUR_PLACEHOLDER"

    val DEFAULT_HOUR_FORMAT = HourFormat.SYSTEM
    val DEFAULT_SCREEN_ON_MODE = ScreenOnMode.ALWAYS

    private lateinit var dataStore: DataStore<Preferences>

    /*
     * Alcance propio para las escrituras y para el `stateIn`. Es un singleton
     * de proceso: `SupervisorJob` evita que un fallo en un flujo cancele a los
     * demás, e IO es el dispatcher adecuado para disco.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /*
     * Flujos reactivos para la UI.
     *
     * Son `lazy` a propósito: los inicializa la primera vez que se leen, que ya
     * es DESPUÉS de `initialize()` (el Application lo llama en `onCreate` antes
     * de cualquier otro componente). Si fueran eagerness, la construcción del
     * singleton tocaría `dataStore` antes de estar inicializado y reventaría.
     */
    val powerAlertsEnabled: StateFlow<Boolean> by lazy {
        preferenceFlow(KEY_POWER_ALERTS, DEFAULT_POWER_ALERTS)
    }

    val chimeEnabled: StateFlow<Boolean> by lazy {
        preferenceFlow(KEY_CHIME_ENABLED, DEFAULT_CHIME_ENABLED)
    }

    val chimeInterval: StateFlow<Int> by lazy {
        preferenceFlow(KEY_CHIME_INTERVAL, DEFAULT_CHIME_INTERVAL)
    }

    val hourFormat: StateFlow<HourFormat> by lazy {
        flow { emitAll(dataStore.data.map { parseHourFormat(it[KEY_HOUR_FORMAT]) }) }
            .stateIn(scope, SharingStarted.Eagerly, DEFAULT_HOUR_FORMAT)
    }

    val screenOnMode: StateFlow<ScreenOnMode> by lazy {
        flow { emitAll(dataStore.data.map { parseScreenOnMode(it[KEY_SCREEN_ON_MODE]) }) }
            .stateIn(scope, SharingStarted.Eagerly, DEFAULT_SCREEN_ON_MODE)
    }

    /** [StateFlow] genérico sobre una clave de DataStore con su default. */
    private fun <T> preferenceFlow(key: Preferences.Key<T>, default: T): StateFlow<T> =
        flow { emitAll(dataStore.data.map { prefs -> prefs[key] ?: default }) }
            .stateIn(scope, SharingStarted.Eagerly, default)

    /** Único punto de inicialización; se llama desde el Application. */
    fun initialize(context: Context) {
        if (::dataStore.isInitialized) return
        dataStore = context.applicationContext.nexusDataStore
    }

    private fun requireStore(): DataStore<Preferences> {
        check(::dataStore.isInitialized) {
            "NexusSettings.initialize(context) debe llamarse antes de usar las preferencias."
        }
        return dataStore
    }

    // --- Lectores suspend (para receivers y planificador) --------------------

    /** Lee el interruptor de alertas de carga dentro de `goAsync()`. */
    suspend fun isPowerAlertsEnabled(): Boolean =
        requireStore().data.first()[KEY_POWER_ALERTS] ?: DEFAULT_POWER_ALERTS

    /** Umbral de batería crítica (en %). */
    suspend fun batteryLowThreshold(): Int =
        requireStore().data.first()[KEY_BATTERY_LOW_THRESHOLD] ?: DEFAULT_BATTERY_LOW_THRESHOLD

    /** Frase a emitir para [event]: la guardada, o la por defecto si nunca se editó. */
    suspend fun phrase(event: PowerAlertEvent): String {
        val key = stringPreferencesKey(event.prefKey)
        return requireStore().data.first()[key] ?: event.defaultPhrase
    }

    /** Lee el interruptor del anuncio horario dentro de `goAsync()`. */
    suspend fun isChimeEnabled(): Boolean =
        requireStore().data.first()[KEY_CHIME_ENABLED] ?: DEFAULT_CHIME_ENABLED

    /** Intervalo de anuncio (minutos), validado contra [CHIME_INTERVALS]. */
    suspend fun chimeIntervalMinutes(): Int {
        val stored = requireStore().data.first()[KEY_CHIME_INTERVAL] ?: DEFAULT_CHIME_INTERVAL
        return if (stored in CHIME_INTERVALS) stored else DEFAULT_CHIME_INTERVAL
    }

    /** Plantilla editable del anuncio (con [CHIME_HOUR_PLACEHOLDER]). */
    suspend fun chimePhraseTemplate(): String =
        requireStore().data.first()[KEY_CHIME_TEMPLATE] ?: DEFAULT_CHIME_TEMPLATE

    // --- Escritores (suspend, atómicos sobre DataStore) ----------------------

    suspend fun setPowerAlertsEnabled(enabled: Boolean) {
        requireStore().edit { it[KEY_POWER_ALERTS] = enabled }
    }

    suspend fun setChimeEnabled(enabled: Boolean) {
        requireStore().edit { it[KEY_CHIME_ENABLED] = enabled }
    }

    suspend fun setChimeIntervalMinutes(minutes: Int) {
        if (minutes !in CHIME_INTERVALS) return
        requireStore().edit { it[KEY_CHIME_INTERVAL] = minutes }
    }

    suspend fun setHourFormat(format: HourFormat) {
        requireStore().edit { it[KEY_HOUR_FORMAT] = format.name }
    }

    suspend fun setScreenOnMode(mode: ScreenOnMode) {
        requireStore().edit { it[KEY_SCREEN_ON_MODE] = mode.name }
    }

    // --- Utilidades de parseo ---

    private fun parseHourFormat(raw: String?): HourFormat =
        HourFormat.entries.firstOrNull { it.name == raw } ?: DEFAULT_HOUR_FORMAT

    private fun parseScreenOnMode(raw: String?): ScreenOnMode =
        ScreenOnMode.entries.firstOrNull { it.name == raw } ?: DEFAULT_SCREEN_ON_MODE
}
