package com.example.nexustime.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos de la barra lateral de NexusTime.
 *
 * El mapa es de una sola pantalla con tres zonas; aquí no hay back stack porque
 * la app es un dashboard de mesa. La selección solo decide qué ocupa la zona
 * central: el panel derecho ("TU DÍA") permanece visible en todos los destinos.
 */
enum class NexusDestination(
    val label: String,
    val icon: ImageVector,
    val contentDescription: String,
) {
    CLOCK(
        label = "Reloj",
        icon = Icons.Outlined.AccessTime,
        contentDescription = "Reloj",
    ),
    AGENDA(
        label = "Agenda",
        icon = Icons.AutoMirrored.Outlined.EventNote,
        contentDescription = "Agenda",
    ),
    NEWS(
        label = "Noticias",
        icon = Icons.Outlined.Newspaper,
        contentDescription = "Noticias del día",
    ),
    REMINDERS(
        label = "Avisos",
        icon = Icons.Outlined.Alarm,
        contentDescription = "Recordatorios programados",
    ),
    SETTINGS(
        label = "Ajustes",
        icon = Icons.Outlined.Settings,
        contentDescription = "Ajustes",
    ),
}
