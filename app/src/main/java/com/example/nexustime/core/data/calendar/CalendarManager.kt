package com.example.nexustime.core.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId

/**
 * Evento puntual del calendario del dispositivo, normalizado para la UI.
 *
 * Se queda con lo mínimo que muestra la tarjeta "TU DÍA": título, inicio y fin
 * (en millis epoch) y si es de todo el día. El resto de metadatos del
 * calendario no aportan a un reloj de mesa y solo acoplarian la UI al proveedor.
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val calendarName: String?,
)

/**
 * Lector de los eventos de HOY del calendario del dispositivo.
 *
 * Es un `object` sin estado (sin Hilt en esta fase): recibe el Context del
 * ViewModel y no lo retiene. Todas las consultas al ContentResolver se hacen en
 * `Dispatchers.IO`, que es quien decide el `AgendaViewModel`.
 */
object CalendarManager {

    /** Proyección mínima; el orden de [query] respeta este array. */
    private val PROJECTION = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
    )

    private val SORT_ORDER = "${CalendarContract.Instances.BEGIN} ASC"

    /**
     * `true` si la app ya tiene READ_CALENDAR concedido.
     *
     * El permiso es en tiempo de ejecución (no basta con declararlo en el
     * manifiesto), así que el panel tiene que distinguir "sin permiso" de "sin
     * eventos" para no mostrar un vacío engañoso.
     */
    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Eventos de hoy, ordenados por hora de inicio.
     *
     * Devuelve vacío (sin lanzar) si no hay permiso: la UI decide qué mostrar.
     * Se protege además contra [SecurityException] porque el sistema puede
     * revocar el permiso entre el chequeo y la consulta.
     */
    fun todayEvents(context: Context): List<CalendarEvent> {
        if (!hasReadPermission(context)) return emptyList()

        val zone = ZoneId.systemDefault()
        val today = Instant.now().atZone(zone).toLocalDate()
        val dayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Instances exige las dos marcas de tiempo ancladas a la URI.
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            ContentUris.appendId(this, dayStart)
            ContentUris.appendId(this, dayEnd)
        }.build()

        val events = mutableListOf<CalendarEvent>()

        try {
            context.contentResolver.query(
                uri,
                PROJECTION,
                null,
                null,
                SORT_ORDER,
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(PROJECTION[0])
                val titleIndex = cursor.getColumnIndexOrThrow(PROJECTION[1])
                val beginIndex = cursor.getColumnIndexOrThrow(PROJECTION[2])
                val endIndex = cursor.getColumnIndexOrThrow(PROJECTION[3])
                val allDayIndex = cursor.getColumnIndexOrThrow(PROJECTION[4])
                val calendarIndex = cursor.getColumnIndexOrThrow(PROJECTION[5])

                while (cursor.moveToNext()) {
                    val title = cursor.getString(titleIndex)
                    events += CalendarEvent(
                        id = cursor.getLong(idIndex),
                        title = if (title.isNullOrBlank()) "Sin título" else title,
                        startMillis = cursor.getLong(beginIndex),
                        endMillis = cursor.getLong(endIndex),
                        allDay = cursor.getInt(allDayIndex) != 0,
                        calendarName = cursor.getString(calendarIndex),
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permiso revocado en caliente: preferimos el estado vacío a un crash.
            return emptyList()
        }

        return events
    }
}
