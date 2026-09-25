package com.example.nexustime.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nexustime.core.ui.components.ComingSoon

/**
 * Pantalla de ajustes.
 *
 * v0.1: todavía no hay preferencias que exponer (el motor de voz, el chime y
 * el clima llegan más adelante). Se maqueta el destino para que la barra
 * lateral no tenga accesos muertos.
 */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
) {
    ComingSoon(
        title = "Ajustes",
        description = "Las preferencias de NexusTime llegarán en la v0.2.",
        modifier = modifier,
    )
}
