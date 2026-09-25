package com.example.nexustime.feature.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexustime.core.ui.theme.ClockSecondsTextStyle
import com.example.nexustime.core.ui.theme.ClockTimeTextStyle
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.core.ui.theme.NexusWhite
import com.example.nexustime.core.ui.theme.SectionLabelTextStyle

/**
 * Punto de entrada de la feature: conecta el [ClockViewModel] con la UI.
 * Mantener el ViewModel aquí (y no dentro de [ClockScreen]) deja la pantalla
 * previewable y testeable sin necesidad de ciclo de vida.
 */
@Composable
fun ClockRoute(
    modifier: Modifier = Modifier,
    viewModel: ClockViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ClockScreen(uiState = uiState, modifier = modifier)
}

/**
 * Zona central: display del reloj gigante.
 *
 * Hora en blanco puro, segundero en azul eléctrico (#1E88E5) y la zona horaria
 * como estado en celeste (#80D8FF), todo sobre negro OLED (#000000).
 */
@Composable
fun ClockScreen(
    uiState: ClockUiState,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = NexusBlack,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = uiState.hourMinute,
                    style = ClockTimeTextStyle,
                    color = NexusWhite,
                )
                Text(
                    text = ":${uiState.seconds}",
                    style = ClockSecondsTextStyle,
                    color = NexusBlue,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Acento en azul eléctrico bajo el display.
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(2.dp)
                    .background(NexusBlue),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = uiState.date,
                style = SectionLabelTextStyle,
                color = NexusWhite,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = uiState.timeZoneId,
                style = MaterialTheme.typography.labelLarge,
                color = NexusSkyBlue,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun ClockScreenPreview() {
    NexusTimeTheme {
        ClockScreen(
            uiState = ClockUiState(
                hourMinute = "21:47",
                seconds = "32",
                date = "Jueves, 25 de septiembre",
                timeZoneId = "America/Mexico_City",
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
