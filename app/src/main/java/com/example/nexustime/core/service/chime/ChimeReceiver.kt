package com.example.nexustime.core.service.chime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.service.tts.TtsManager

/**
 * Receptor del anuncio horario (chime).
 *
 * Tiene dos entradas:
 *  * [ACTION_CHIME] — la alarma que dispara [ChimeScheduler]. Anuncia la hora.
 *  * `ACTION_BOOT_COMPLETED` / `ACTION_MY_PACKAGE_REPLACED` — tras un reinicio o
 *    una actualización, las alarmas se pierden: aquí se vuelven a programar.
 *
 * Está declarado en el manifiesto (no dinámico) porque el `PendingIntent` de
 * [ChimeScheduler] debe poder despertar el proceso aunque la app esté cerrada.
 *
 * Tras cada disparo se reprograma el siguiente anuncio, de modo que toda la
 * cadena se mantiene con una única alarma viva a la vez.
 */
class ChimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CHIME && NexusSettings.isChimeEnabled()) {
            TtsManager.speak(
                text = ChimeText.currentAnnouncement(),
                flush = false,
            )
        }

        // Reagenda (o cancela, si está apagado) el próximo anuncio.
        ChimeScheduler.scheduleNext(context)
    }

    companion object {
        const val ACTION_CHIME = "com.example.nexustime.action.CHIME"
    }
}
