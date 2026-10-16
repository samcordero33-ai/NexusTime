package com.example.nexustime.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nexustime.core.data.calendar.CalendarEvent
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusBorder
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusSurface
import com.example.nexustime.core.ui.theme.NexusTextSecondary
import com.example.nexustime.core.ui.theme.NexusTextTertiary
import com.example.nexustime.core.ui.theme.NexusWhite
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Contenido compartido de la agenda.
 *
 * Lo usan tanto el panel derecho ("TU DÍA") como la pantalla central de Agenda,
 * con distinta densidad: así los tres estados (cargando / sin permiso / vacío)
 * se comportan igual en ambos lugares sin duplicar lógica.
 */

private val EventTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())

/**
 * Decide qué mostrar según el estado de la agenda.
 *
 * El orden importa: primero la carga, después el permiso y solo al final el
 * vacío, para no flashar el mensaje de "sin eventos" mientras se consulta.
 */
@Composable
fun AgendaEventsContent(
    state: AgendaUiState,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading -> AgendaStateMessage(
            text = "Cargando agenda…",
            modifier = modifier,
        )

        !state.permissionGranted -> AgendaStateMessage(
            icon = Icons.Outlined.Lock,
            text = "Concede acceso al calendario para ver tus eventos",
            modifier = modifier,
        )

        state.events.isEmpty() -> AgendaStateMessage(
            text = "No hay eventos programados para hoy",
            modifier = modifier,
        )

        else -> LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = state.events, key = { it.id }) { event ->
                AgendaEventRow(event = event)
            }
        }
    }
}

/** Fila de evento: hora a la izquierda, título y calendario a la derecha. */
@Composable
private fun AgendaEventRow(
    event: CalendarEvent,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = NexusSurface, shape = shape)
            .border(width = 1.dp, color = NexusBorder, shape = shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Columna de hora: ancho fijo para que las filas queden alineadas.
        Column(modifier = Modifier.width(66.dp)) {
            if (event.allDay) {
                Text(
                    text = "Todo el día",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusSkyBlue,
                )
            } else {
                Text(
                    text = formatEventTime(event.startMillis),
                    style = MaterialTheme.typography.titleLarge,
                    color = NexusBlue,
                )
                if (event.endMillis > event.startMillis) {
                    Text(
                        text = formatEventTime(event.endMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusTextTertiary,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyLarge,
                color = NexusWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!event.calendarName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = event.calendarName,
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusTextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Estado centrado (cargando / sin permiso / vacío).
 *
 * Dibuja una caja de altura mínima para que el panel derecho no "salte" al
 * alternar entre estados.
 */
@Composable
private fun AgendaStateMessage(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon ?: Icons.AutoMirrored.Outlined.EventNote,
                contentDescription = null,
                tint = if (icon == null) NexusSkyBlue else NexusTextTertiary,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Hora de un evento en formato 24 h ("14:30"), en la zona horaria del sistema. */
private fun formatEventTime(millis: Long): String =
    Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(EventTimeFormatter)
