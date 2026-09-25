package com.example.nexustime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nexustime.core.ui.components.ComingSoon
import com.example.nexustime.core.ui.components.NexusNavRail
import com.example.nexustime.core.ui.navigation.NexusDestination
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.feature.agenda.AgendaPanel
import com.example.nexustime.feature.agenda.AgendaPanelWidth
import com.example.nexustime.feature.clock.ClockRoute
import com.example.nexustime.feature.settings.SettingsRoute

/**
 * Layout raíz de NexusTime, pensado para tablets en horizontal.
 *
 * Tres zonas de izquierda a derecha:
 *  a) Barra lateral de navegación (anchos fijos, accesos rápidos).
 *  b) Zona central: el reloj gigante; ocupa el espacio restante.
 *  c) Panel derecho: contenedor de la futura tarjeta "TU DÍA".
 *
 * La selección de destino se guarda en memoria: en v0.1 no hay navegación con
 * back stack porque la app es un dashboard de una sola pantalla.
 */
@Composable
fun NexusTimeApp(
    modifier: Modifier = Modifier,
) {
    var destination by remember { mutableStateOf(NexusDestination.CLOCK) }
    val destinations = remember { NexusDestination.entries }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBlack),
    ) {
        // a) Barra lateral de navegación.
        NexusNavRail(
            destinations = destinations,
            selectedDestination = destination,
            onDestinationSelected = { destination = it },
        )

        // b) Zona central.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            when (destination) {
                NexusDestination.CLOCK -> ClockRoute()
                NexusDestination.ALARMS -> ComingSoon(
                    title = "Alarmas",
                    description = "El motor de alarmas llegará en la v0.2.",
                )
                NexusDestination.SETTINGS -> SettingsRoute()
            }
        }

        // c) Panel derecho.
        AgendaPanel(
            modifier = Modifier.width(AgendaPanelWidth),
        )
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800)
@Composable
private fun NexusTimeAppPreview() {
    NexusTimeTheme {
        NexusTimeApp()
    }
}
