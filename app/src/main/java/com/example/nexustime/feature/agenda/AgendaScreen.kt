package com.example.nexustime.feature.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexustime.core.ui.components.NexusPanel
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusTextSecondary
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.core.ui.theme.NexusWhite

/**
 * Punto de entrada de la pantalla central de Agenda.
 *
 * Comparte el mismo [AgendaViewModel] que el panel derecho (mismo
 * ViewModelStoreOwner), así no se consulta el calendario dos veces.
 */
@Composable
fun AgendaRoute(
    modifier: Modifier = Modifier,
    viewModel: AgendaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AgendaScreen(state = state, modifier = modifier)
}

/**
 * Zona central en el destino Agenda: vista ampliada de los eventos de hoy.
 *
 * Mantiene el mismo lenguaje visual que el panel derecho pero con más
 * respiración, para que la lista se lea de un vistazo en la tablet.
 */
@Composable
fun AgendaScreen(
    state: AgendaUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBlack)
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        Text(
            text = "Agenda",
            style = MaterialTheme.typography.headlineMedium,
            color = NexusWhite,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tus eventos de hoy",
            style = MaterialTheme.typography.bodyMedium,
            color = NexusTextSecondary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Hoy",
            subtitle = "Eventos del calendario",
            headerAccent = NexusSkyBlue,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            AgendaEventsContent(
                state = state,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun AgendaScreenPreview() {
    NexusTimeTheme {
        AgendaScreen(state = AgendaUiState())
    }
}
