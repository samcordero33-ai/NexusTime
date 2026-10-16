package com.example.nexustime.core.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Eventos de estado que pueden generar un aviso parlante de carga.
 *
 * Cada uno lleva su clave de preferencia y su frase por defecto: así el receptor
 * resuelve la frase con una sola llamada y la edición de textos (Ajustes) en una
 * versión futura solo tiene que escribir en esa clave.
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

/**
 * Preferencias de NexusTime.
 *
 * Usa [SharedPreferences] y no DataStore a propósito: el consumidor principal es
 * un `BroadcastReceiver`, y `onReceive()` necesita leer el interruptor de forma
 * **síncrona** dentro de su ventana de ejecución. DataStore es asíncrono por
 * diseño y obligaría a bloquearse o a propagar el resultado con una corrutina que
 * el receiver no puede esperar.
 *
 * Para la UI sí se exponen [StateFlow]s, reconstruidos desde el listener de
 * preferencias, de modo que Compose siga reactivo sin cambiar el almacén.
 *
 * Es singleton (sin Hilt en esta fase): la instancia vive mientras vive el
 * proceso y no retiene Context, solo el SharedPreferences del Application.
 */
object NexusSettings {

    private const val PREFS_NAME = "nexustime_settings"

    // --- Alertas de carga ---
    private const val KEY_POWER_ALERTS = "power_alerts_enabled"
    private const val KEY_BATTERY_LOW_THRESHOLD = "battery_low_threshold_percent"

    // --- Anuncio horario (chime) ---
    private const val KEY_CHIME_ENABLED = "chime_enabled"
    private const val KEY_CHIME_INTERVAL = "chime_interval_minutes"
    private const val KEY_CHIME_TEMPLATE = "chime_phrase_template"

    /** Valor inicial: los avisos parlantes vienen activados. */
    const val DEFAULT_POWER_ALERTS = true

    /** Nivel de batería (en %) a partir del cual se considera crítica. */
    const val DEFAULT_BATTERY_LOW_THRESHOLD = 15

    /** El anuncio horario es opt-in: viene apagado. */
    const val DEFAULT_CHIME_ENABLED = false

    /** Intervalos permitidos para el anuncio horario, en minutos. */
    val CHIME_INTERVALS = listOf(30, 60)
    const val DEFAULT_CHIME_INTERVAL = 60

    /** Plantilla del anuncio. `{hora}` se sustituye por la hora hablada. */
    const val CHIME_HOUR_PLACEHOLDER = "{hora}"
    const val DEFAULT_CHIME_TEMPLATE = "Son las $CHIME_HOUR_PLACEHOLDER"

    private lateinit var prefs: SharedPreferences

    /*
     * El listener de SharedPreferences debe guardarse en un campo: si no, el
     * recolector de basura lo libera y las preferencias dejan de notificar.
     */
    @Suppress("unused")
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val _powerAlertsEnabled = MutableStateFlow(DEFAULT_POWER_ALERTS)
    private val _chimeEnabled = MutableStateFlow(DEFAULT_CHIME_ENABLED)
    private val _chimeInterval = MutableStateFlow(DEFAULT_CHIME_INTERVAL)

    /** Estado reactivo del interruptor "Alertas de Carga parlantes". */
    val powerAlertsEnabled: StateFlow<Boolean> = _powerAlertsEnabled.asStateFlow()

    /** Estado reactivo del interruptor del anuncio horario. */
    val chimeEnabled: StateFlow<Boolean> = _chimeEnabled.asStateFlow()

    /** Intervalo reactivo del anuncio horario, en minutos. */
    val chimeInterval: StateFlow<Int> = _chimeInterval.asStateFlow()

    /** Único punto de inicialización; se llama desde el Application. */
    fun initialize(context: Context) {
        if (::prefs.isInitialized) return

        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        _powerAlertsEnabled.value = prefs.getBoolean(KEY_POWER_ALERTS, DEFAULT_POWER_ALERTS)
        _chimeEnabled.value = prefs.getBoolean(KEY_CHIME_ENABLED, DEFAULT_CHIME_ENABLED)
        _chimeInterval.value = prefs.getInt(KEY_CHIME_INTERVAL, DEFAULT_CHIME_INTERVAL)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                KEY_POWER_ALERTS ->
                    _powerAlertsEnabled.value =
                        prefs.getBoolean(KEY_POWER_ALERTS, DEFAULT_POWER_ALERTS)

                KEY_CHIME_ENABLED ->
                    _chimeEnabled.value =
                        prefs.getBoolean(KEY_CHIME_ENABLED, DEFAULT_CHIME_ENABLED)

                KEY_CHIME_INTERVAL ->
                    _chimeInterval.value =
                        prefs.getInt(KEY_CHIME_INTERVAL, DEFAULT_CHIME_INTERVAL)

                else -> Unit
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        this.listener = listener
    }

    // --- Alertas de carga -----------------------------------------------------

    /**
     * Lectura síncrona para el `BroadcastReceiver`.
     *
     * Antes de [initialize] devuelve el valor por defecto para no tumbar la app
     * en previews o si el proceso arranca por otro camino.
     */
    fun isPowerAlertsEnabled(): Boolean =
        if (::prefs.isInitialized) prefs.getBoolean(KEY_POWER_ALERTS, DEFAULT_POWER_ALERTS)
        else DEFAULT_POWER_ALERTS

    fun setPowerAlertsEnabled(enabled: Boolean) {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean(KEY_POWER_ALERTS, enabled).apply()
        _powerAlertsEnabled.value = enabled
    }

    /** Umbral de batería crítica (en %), leído de forma síncrona por el receptor. */
    fun batteryLowThreshold(): Int =
        if (::prefs.isInitialized) {
            prefs.getInt(KEY_BATTERY_LOW_THRESHOLD, DEFAULT_BATTERY_LOW_THRESHOLD)
        } else {
            DEFAULT_BATTERY_LOW_THRESHOLD
        }

    /** Frase a emitir para [event]: la guardada, o la por defecto si nunca se editó. */
    fun phrase(event: PowerAlertEvent): String {
        if (!::prefs.isInitialized) return event.defaultPhrase
        return prefs.getString(event.prefKey, event.defaultPhrase) ?: event.defaultPhrase
    }

    // --- Anuncio horario (chime) ---------------------------------------------

    /** Lectura síncrona usada por el receptor del chime. */
    fun isChimeEnabled(): Boolean =
        if (::prefs.isInitialized) prefs.getBoolean(KEY_CHIME_ENABLED, DEFAULT_CHIME_ENABLED)
        else DEFAULT_CHIME_ENABLED

    fun setChimeEnabled(enabled: Boolean) {
        if (!::prefs.isInitialized) return
        prefs.edit().putBoolean(KEY_CHIME_ENABLED, enabled).apply()
        _chimeEnabled.value = enabled
    }

    /** Intervalo de anuncio (minutos), leído de forma síncrona por el planificador. */
    fun chimeIntervalMinutes(): Int {
        if (!::prefs.isInitialized) return DEFAULT_CHIME_INTERVAL
        val stored = prefs.getInt(KEY_CHIME_INTERVAL, DEFAULT_CHIME_INTERVAL)
        return if (stored in CHIME_INTERVALS) stored else DEFAULT_CHIME_INTERVAL
    }

    fun setChimeIntervalMinutes(minutes: Int) {
        if (!::prefs.isInitialized) return
        if (minutes !in CHIME_INTERVALS) return
        prefs.edit().putInt(KEY_CHIME_INTERVAL, minutes).apply()
        _chimeInterval.value = minutes
    }

    /** Plantilla editable del anuncio (con [CHIME_HOUR_PLACEHOLDER]). */
    fun chimePhraseTemplate(): String =
        if (::prefs.isInitialized) {
            prefs.getString(KEY_CHIME_TEMPLATE, DEFAULT_CHIME_TEMPLATE) ?: DEFAULT_CHIME_TEMPLATE
        } else {
            DEFAULT_CHIME_TEMPLATE
        }

    fun setChimePhraseTemplate(template: String) {
        if (!::prefs.isInitialized) return
        prefs.edit().putString(KEY_CHIME_TEMPLATE, template).apply()
    }
}
