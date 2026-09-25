package com.example.nexustime.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Destinos de la barra lateral de NexusTime.
 *
 * En v0.1 solo CLOCK tiene implementación real; el resto son accesos rápidos
 * que muestran un estado "próximamente" hasta que lleguen sus módulos.
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
    ALARMS(
        label = "Alarmas",
        icon = Icons.Outlined.Alarm,
        contentDescription = "Alarmas",
    ),
    SETTINGS(
        label = "Ajustes",
        icon = Icons.Outlined.Settings,
        contentDescription = "Ajustes",
    ),
}
