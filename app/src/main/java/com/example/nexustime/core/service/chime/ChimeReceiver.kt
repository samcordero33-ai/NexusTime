package com.example.nexustime.core.service.chime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
 *
 * Como lee DataStore (asíncrono), usa [goAsync] para alargar la ventana de
 * entrega y cerrar el [PendingResult] con `finish()` cuando la corrutina termina.
 */
class ChimeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.action == ACTION_CHIME && NexusSettings.isChimeEnabled()) {
                    TtsManager.speak(
                        text = ChimeText.currentAnnouncement(),
                        flush = false,
                    )
                }

                // Reagenda (o cancela, si está apagado) el próximo anuncio.
                ChimeScheduler.scheduleNext(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHIME = "com.example.nexustime.action.CHIME"
    }
}
