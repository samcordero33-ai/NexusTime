package com.example.nexustime.core.service.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.reminder.Reminder
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Planifica los recordatorios con [AlarmManager].
 *
 * Cada recordatorio tiene una **alarma por hora** del día; al dispararse, el
 * [ReminderReceiver] vuelve a llamar aquí para rearmar la siguiente ocurrencia.
 * Se usan alarmas de un único disparo (no repetitivas) para respetar el cambio de
 * horario y no acumular deriva, igual que el chime horario.
 *
 * El `PendingIntent` se identifica con un *request code* derivado del id del
 * recordatorio y del índice de la hora, de modo que cada (recordatorio, hora) es
 * único y se puede cancelar de forma selectiva.
 */
object ReminderScheduler {

    private const val TAG = "NexusReminder"

    /**
     * Base para los request codes, para no chocar con el chime horario (2001).
     * El rango útil es `[REQUEST_BASE, REQUEST_BASE + 2^20 * 16)`.
     */
    private const val REQUEST_BASE = 100_000

    /**
     * (Re)programa TODOS los recordatorios. Se llama al arrancar la app
     * ([com.example.nexustime.NexusTimeApplication]) y tras un reinicio.
     */
    suspend fun rescheduleAll(context: Context) {
        val reminders = NexusSettings.currentReminders()
        Log.d(TAG, "Reprogramando ${reminders.size} recordatorios")
        reminders.forEach { cancel(context, it) }
        reminders.filter { it.enabled }.forEach { schedule(context, it) }
    }

    /** Reprograma un único recordatorio (al crear/editar/activar/desactivar). */
    suspend fun reschedule(context: Context, reminder: Reminder) {
        cancel(context, reminder)
        if (reminder.enabled) schedule(context, reminder)
    }

    /** Cancela todas las alarmas de un recordatorio (al borrarlo o desactivarlo). */
    fun cancel(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        reminder.timesOfDay.indices.forEach { index ->
            val pendingIntent = alarmPendingIntent(context, reminder.id, index)
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun schedule(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val exact = canScheduleExact(alarmManager)

        reminder.timesOfDay.forEachIndexed { index, minuteOfDay ->
            val triggerAtMillis = nextOccurrenceMillis(minuteOfDay)
            val pendingIntent = alarmPendingIntent(context, reminder.id, index)

            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                // Sin permiso de alarmas exactas: cae a inexacta, no se pierde.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }

            Log.d(TAG, "Recordatorio ${reminder.id} hora #$index -> $triggerAtMillis (exact=$exact)")
        }
    }

    /**
     * Próximo instante (millis) para una hora del día.
     *
     * Si la hora ya pasó hoy, se agenda para mañana: el resultado es siempre
     * futuro, que es lo que [AlarmManager] necesita.
     */
    private fun nextOccurrenceMillis(minuteOfDay: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)

        var target = now.toLocalDate()
            .atStartOfDay(zone)
            .plusMinutes(minuteOfDay.toLong())

        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return target.toInstant().toEpochMilli()
    }

    private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    private fun alarmPendingIntent(
        context: Context,
        reminderId: Long,
        timeIndex: Int,
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMINDER
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_TIME_INDEX, timeIndex)
        }

        /*
         * Request code único por (recordatorio, hora). Se recorta el id a 20 bits
         * y se le dejan 4 bits al índice (hasta 16 horas por recordatorio), que
         * es más que suficiente y mantiene el valor dentro de Int.
         */
        val requestCode = REQUEST_BASE +
            ((reminderId and 0xFFFFF).toInt() * 16) +
            (timeIndex % 16)

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
