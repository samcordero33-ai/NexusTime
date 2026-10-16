package com.example.nexustime

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import com.example.nexustime.core.ui.components.NexusNavRail
import com.example.nexustime.core.ui.navigation.NexusDestination
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.feature.agenda.AgendaPanel
import com.example.nexustime.feature.agenda.AgendaPanelWidth
import com.example.nexustime.feature.agenda.AgendaRoute
import com.example.nexustime.feature.clock.ClockRoute
import com.example.nexustime.feature.news.NewsRoute
import com.example.nexustime.feature.reminder.ReminderRoute
import com.example.nexustime.feature.settings.SettingsRoute

/**
 * Layout raíz de NexusTime, pensado para tablets en horizontal.
 *
 * Tres zonas de izquierda a derecha:
 *  a) Barra lateral de navegación (Reloj / Agenda / Ajustes).
 *  b) Zona central: cambia de destino con una transición suave.
 *  c) Panel derecho: tarjeta "TU DÍA", visible en todos los destinos.
 *
 * La selección se guarda en memoria: la app es un dashboard de mesa, así que
 * no hay back stack ni navegación con estado persistido.
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

        // b) Zona central: transición suave entre destinos.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    // El sentido del deslizamiento sigue el orden de la barra:
                    // avanzar desliza hacia la izquierda, retroceder hacia la derecha.
                    val forward = targetState.ordinal > initialState.ordinal
                    val offset = if (forward) 1 else -1

                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it * offset / 6 }) togetherWith
                        (fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { -it * offset / 6 })
                },
                label = "destination",
            ) { target ->
                when (target) {
                    NexusDestination.CLOCK -> ClockRoute()
                    NexusDestination.AGENDA -> AgendaRoute()
                    NexusDestination.NEWS -> NewsRoute()
                    NexusDestination.REMINDERS -> ReminderRoute()
                    NexusDestination.SETTINGS -> SettingsRoute()
                }
            }
        }

        // c) Panel derecho: "TU DÍA", siempre presente.
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
