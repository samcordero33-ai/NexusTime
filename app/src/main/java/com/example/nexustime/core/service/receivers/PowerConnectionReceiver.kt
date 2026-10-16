package com.example.nexustime.core.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.PowerAlertEvent
import com.example.nexustime.core.service.tts.TtsManager

/**
 * Avisos parlantes de corriente y batería.
 *
 * Escucha cuatro protegidos del sistema:
 *  * [Intent.ACTION_POWER_CONNECTED]    -> "Cargador conectado. Iniciando carga."
 *  * [Intent.ACTION_POWER_DISCONNECTED] -> "Desconectado de la corriente."
 *  * [Intent.ACTION_BATTERY_LOW]        -> batería crítica (el SO la dispara hacia el ~15%)
 *  * [Intent.ACTION_BATTERY_FULL]       -> carga al 100% mientras sigue conectada
 *
 * Los cuatro son *protected broadcasts*: solo el sistema puede enviarlos, así que
 * el filtro no puede ser suplantado por otra app. Se registran **de forma dinámica**
 * desde la actividad (ver `MainActivity`), nunca en el manifiesto, porque Android 8+
 * ignora los receivers implícitos declarados ahí en segundo plano.
 *
 * `onReceive()` es síncrono y de muy corta duración: aquí solo se decide qué frase
 * se lanza. El habla es asíncrona ([TtsManager.speak] encola), no bloquea la entrega
 * del broadcast.
 */
class PowerConnectionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Interruptor de Ajustes apagado -> silencio total.
        if (!NexusSettings.isPowerAlertsEnabled()) return

        val event = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> PowerAlertEvent.CONNECTED
            Intent.ACTION_POWER_DISCONNECTED -> PowerAlertEvent.DISCONNECTED
            Intent.ACTION_BATTERY_LOW -> PowerAlertEvent.BATTERY_LOW
            Intent.ACTION_BATTERY_FULL -> PowerAlertEvent.BATTERY_FULL
            else -> return
        }

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
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_FULL)
        }
    }
}
