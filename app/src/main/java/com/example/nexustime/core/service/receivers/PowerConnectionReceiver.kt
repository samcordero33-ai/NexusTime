package com.example.nexustime.core.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.PowerAlertEvent
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Avisos parlantes de corriente y batería.
 *
 * Escucha tres *protected broadcasts* del sistema (solo el sistema puede enviarlos,
 * así que el filtro no puede ser suplantado por otra app):
 *  * [Intent.ACTION_POWER_CONNECTED]    -> "Cargador conectado. Iniciando carga."
 *  * [Intent.ACTION_POWER_DISCONNECTED] -> "Desconectado de la corriente."
 *  * [Intent.ACTION_BATTERY_CHANGED]    -> nivel y estado de la batería.
 *
 * Sobre `ACTION_BATTERY_CHANGED` (un *sticky broadcast* que llega al registrarse y
 * en cada cambio de batería) se detectan los dos avisos de nivel:
 *  * **Batería crítica** cuando el nivel baja al umbral configurado (por defecto 15%).
 *  * **Carga completa** al 100% (`BATTERY_STATUS_FULL`) mientras sigue conectada.
 *
 * Se usa `ACTION_BATTERY_CHANGED` y no las acciones `BATTERY_LOW`/`BATTERY_FULL`
 * porque `Intent.ACTION_BATTERY_FULL` **no existe** en el SDK y el umbral de
 * `ACTION_BATTERY_LOW` lo decide el fabricante; así el 15% es explícito.
 *
 * El sistema solo entrega `ACTION_BATTERY_CHANGED` a receptores registrados de
 * forma **dinámica** (ver `MainActivity`); declararlo en el manifiesto no funciona.
 *
 * **DataStore es asíncrono**, así que `onReceive()` no puede leer las preferencias
 * de forma síncrona. Se usa [goAsync], que alarga la ventana de entrega del
 * broadcast y devuelve un [PendingResult] que hay que cerrar con `finish()`. La
 * lectura se hace en una corrutina sobre `Dispatchers.IO`; el habla es además
 * asíncrona ([TtsManager.speak] encola), así que nada bloquea el hilo principal.
 */
class PowerConnectionReceiver : BroadcastReceiver() {

    /*
     * Pestillos para no repetir el mismo aviso en cada emisión de
     * ACTION_BATTERY_CHANGED (que llega muchas veces). Se rearman cuando la
     * condición deja de cumplirse o al cambiar el estado de la corriente.
     */
    private var lowAnnounced = false
    private var fullAnnounced = false

    override fun onReceive(context: Context, intent: Intent) {
        // Log de captura: confirma que el evento de energía llega al receptor.
        Log.d(TAG, "Evento de energía recibido: action=${intent.action}")
        // Alarga la ventana de entrega para poder leer DataStore y decidir.
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context.applicationContext, intent)
            } finally {
                // Siempre se cierra, haya habido excepción o no: si no, el
                // sistema espera el tiempo máximo y registra un ANR de broadcast.
                pendingResult.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        // Interruptor de Ajustes apagado -> silencio total.
        if (!NexusSettings.isPowerAlertsEnabled()) {
            Log.d(TAG, "Alertas de carga desactivadas; se ignora $intent.action")
            return
        }

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d(TAG, "Cargador CONECTADO")
                fullAnnounced = false
                speak(PowerAlertEvent.CONNECTED)
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG, "Cargador DESCONECTADO")
                lowAnnounced = false
                speak(PowerAlertEvent.DISCONNECTED)
            }

            Intent.ACTION_BATTERY_CHANGED -> handleBatteryLevel(intent)

            else -> Unit
        }
    }

    private suspend fun handleBatteryLevel(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        if (level < 0 || scale <= 0) return

        val percent = level * 100 / scale
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0

        Log.d(TAG, "Batería: $percent% (status=$status, plugged=$plugged)")

        // Carga completa: estado FULL del sistema o 100% con el cargador puesto.
        val full = plugged && (status == BatteryManager.BATTERY_STATUS_FULL || percent >= 100)
        if (full) {
            if (!fullAnnounced) {
                fullAnnounced = true
                speak(PowerAlertEvent.BATTERY_FULL)
            }
        } else {
            fullAnnounced = false
        }

        // Batería crítica por debajo del umbral configurado.
        val threshold = NexusSettings.batteryLowThreshold()
        if (percent in 1..threshold) {
            if (!lowAnnounced) {
                lowAnnounced = true
                speak(PowerAlertEvent.BATTERY_LOW)
            }
        } else if (percent > threshold) {
            lowAnnounced = false
        }
    }

    private suspend fun speak(event: PowerAlertEvent) {
        TtsManager.speak(
            text = NexusSettings.phrase(event),
            flush = true,
        )
    }

    companion object {

        private const val TAG = "NexusPower"

        /**
         * Filtro por llamada.
         *
         * Se crea un [IntentFilter] nuevo en cada registro: los filtros son
         * mutables y compartirlos entre ciclos invita a duplicar acciones.
         */
        fun intentFilter(): IntentFilter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
    }
}
