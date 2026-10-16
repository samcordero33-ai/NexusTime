package com.example.nexustime.core.service.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.reminder.ReminderKind
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receptor de los recordatorios programados.
 *
 * Entradas:
 *  * [ACTION_REMINDER] — la alarma que dispara [ReminderScheduler]. Suena el
 *    recordatorio (voz o audio) y **rearma** la siguiente ocurrencia.
 *  * `ACTION_BOOT_COMPLETED` / `ACTION_MY_PACKAGE_REPLACED` — tras reiniciar o
 *    actualizar, las alarmas se pierden: aquí se reprograman todas.
 *
 * Declarado en el manifiesto para que el `PendingIntent` pueda despertar el
 * proceso con la app cerrada.
 *
 * Como lee DataStore (asíncrono), usa [goAsync] para cerrar el `PendingResult`
 * con `finish()` al terminar la corrutina.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        Log.d(TAG, "Recordatorio: action=${intent.action}")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_REMINDER -> fire(appContext, intent)
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    -> ReminderScheduler.rescheduleAll(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun fire(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id < 0) return

        val reminder = NexusSettings.currentReminders().firstOrNull { it.id == id }
        if (reminder == null) {
            Log.w(TAG, "Recordatorio $id no encontrado (¿borrado?)")
            return
        }
        if (!reminder.enabled) {
            Log.d(TAG, "Recordatorio $id desactivado; se ignora")
            return
        }

        when (reminder.kind) {
            ReminderKind.VOICE -> {
                if (reminder.message.isNotBlank()) {
                    TtsManager.speak(reminder.message, flush = true)
                }
            }

            ReminderKind.AUDIO -> {
                val uri = reminder.audioUri
                if (uri.isNullOrBlank()) {
                    Log.w(TAG, "Recordatorio $id sin archivo de audio")
                } else {
                    AudioPlayer.play(context, uri)
                }
            }
        }

        // Rearma la siguiente ocurrencia (mañana, porque las horas son futuras).
        ReminderScheduler.reschedule(context, reminder)
    }

    companion object {
        private const val TAG = "NexusReminder"

        const val ACTION_REMINDER = "com.example.nexustime.action.REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_TIME_INDEX = "time_index"
    }
}
