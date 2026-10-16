package com.example.nexustime.core.service.chime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.nexustime.core.data.local.NexusSettings
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Planifica el próximo anuncio horario con [AlarmManager].
 *
 * Se usa una alarma de un solo disparo que se **reprograma** tras cada aviso
 * (ver [ChimeReceiver]) en lugar de una alarma repetitiva: así el intervalo se
 * recalcula sobre la hora real del reloj (en punto / y media) y respeta el
 * cambio de horario, sin acumular deriva.
 *
 * Si la app no puede agendar alarmas exactas (Android 12+ sin el permiso
 * concedido), cae a una alarma inexacta en vez de fallar: el anuncio puede
 * llegar con unos minutos de margen, pero nunca se pierde.
 */
object ChimeScheduler {

    private const val REQUEST_CODE = 2001

    /**
     * (Re)programa el siguiente anuncio según las preferencias actuales.
     * Si el chime está apagado, cancela cualquier alarma pendiente.
     *
     * Es `suspend` porque lee DataStore (asíncrono). Los llamadores lo invocan
     * dentro de una corrutina: el Application, la actividad o `goAsync()` del
     * propio receptor.
     */
    suspend fun scheduleNext(context: Context) {
        val intervalMinutes = NexusSettings.chimeIntervalMinutes()
        if (!NexusSettings.isChimeEnabled() || intervalMinutes <= 0) {
            cancel(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAtMillis = nextTriggerMillis(intervalMinutes)
        val pendingIntent = chimePendingIntent(context)

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    /** Cancela el anuncio pendiente, si lo hay. */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = chimePendingIntent(context)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Próximo instante alineado al reloj: el siguiente múltiplo del intervalo
     * dentro del día local (p. ej. con 60 min -> la próxima hora en punto).
     */
    private fun nextTriggerMillis(intervalMinutes: Int): Long {
        val now = LocalDateTime.now()
        val minuteOfDay = now.hour * 60 + now.minute
        val nextMinuteOfDay = ((minuteOfDay / intervalMinutes) + 1) * intervalMinutes
        return now.toLocalDate()
            .atStartOfDay()
            .plusMinutes(nextMinuteOfDay.toLong())
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun chimePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ChimeReceiver::class.java)
            .setAction(ChimeReceiver.ACTION_CHIME)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
