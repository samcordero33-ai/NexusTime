package com.example.nexustime.core.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.PowerAlertEvent
import com.example.nexustime.core.service.tts.TtsManager

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
 * `onReceive()` es síncrono y de muy corta duración: aquí solo se decide qué frase
 * se lanza. El habla es asíncrona ([TtsManager.speak] encola), no bloquea la entrega.
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
        // Interruptor de Ajustes apagado -> silencio total.
        if (!NexusSettings.isPowerAlertsEnabled()) return

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                fullAnnounced = false
                speak(PowerAlertEvent.CONNECTED)
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                lowAnnounced = false
                speak(PowerAlertEvent.DISCONNECTED)
            }

            Intent.ACTION_BATTERY_CHANGED -> handleBatteryLevel(intent)

            else -> Unit
        }
    }

    private fun handleBatteryLevel(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0)
        if (level < 0 || scale <= 0) return

        val percent = level * 100 / scale
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0

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

    private fun speak(event: PowerAlertEvent) {
        TtsManager.speak(
            text = NexusSettings.phrase(event),
            flush = true,
        )
    }

    companion object {

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
