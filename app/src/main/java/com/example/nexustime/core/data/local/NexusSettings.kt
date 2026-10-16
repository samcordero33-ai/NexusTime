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
 * Para la UI sí se expone un [StateFlow], reconstruido desde el listener de
 * preferencias, de modo que Compose siga reactivo sin cambiar el almacén.
 *
 * Es singleton (sin Hilt en esta fase): la instancia vive mientras vive el
 * proceso y no retiene Context, solo el SharedPreferences del Application.
 */
object NexusSettings {

    private const val PREFS_NAME = "nexustime_settings"
    private const val KEY_POWER_ALERTS = "power_alerts_enabled"

    /** Valor inicial: los avisos parlantes vienen activados. */
    const val DEFAULT_POWER_ALERTS = true

    private lateinit var prefs: SharedPreferences

    /*
     * El listener de SharedPreferences debe guardarse en un campo: si no, el
     * recolector de basura lo libera y las preferencias dejan de notificar.
     */
    @Suppress("unused")
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private val _powerAlertsEnabled = MutableStateFlow(DEFAULT_POWER_ALERTS)

    /** Estado reactivo del interruptor "Alertas de Carga parlantes". */
    val powerAlertsEnabled: StateFlow<Boolean> = _powerAlertsEnabled.asStateFlow()

    /** Único punto de inicialización; se llama desde [NexusTimeApplication]. */
    fun initialize(context: Context) {
        if (::prefs.isInitialized) return

        prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        _powerAlertsEnabled.value = prefs.getBoolean(KEY_POWER_ALERTS, DEFAULT_POWER_ALERTS)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_POWER_ALERTS) {
                _powerAlertsEnabled.value =
                    prefs.getBoolean(KEY_POWER_ALERTS, DEFAULT_POWER_ALERTS)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        this.listener = listener
    }

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

    /** Frase a emitir para [event]: la guardada, o la por defecto si nunca se editó. */
    fun phrase(event: PowerAlertEvent): String {
        if (!::prefs.isInitialized) return event.defaultPhrase
        return prefs.getString(event.prefKey, event.defaultPhrase) ?: event.defaultPhrase
    }
}
