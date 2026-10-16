package com.example.nexustime.feature.agenda

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexustime.core.ui.components.NexusPanel
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusTimeTheme

/** Ancho del panel derecho (zona de la agenda). */
val AgendaPanelWidth = 340.dp

/**
 * Panel derecho: tarjeta "TU DÍA".
 *
 * Lee los eventos de hoy del calendario del dispositivo vía [AgendaViewModel].
 * Se refresca al volver a `ON_RESUME` para que, tras conceder el permiso desde
 * el diálogo del sistema, la lista se rellene sola.
 */
@Composable
fun AgendaPanel(
    modifier: Modifier = Modifier,
    viewModel: AgendaViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    NexusPanel(
        title = "Tu día",
        subtitle = "Agenda del día",
        headerAccent = NexusSkyBlue,
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        AgendaEventsContent(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        )
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun AgendaPanelPreview() {
    NexusTimeTheme {
        AgendaPanel(modifier = Modifier.fillMaxWidth())
    }
}
