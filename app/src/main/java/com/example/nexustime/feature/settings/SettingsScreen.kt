package com.example.nexustime.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexustime.core.ui.components.NexusPanel
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusBorder
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusSilver
import com.example.nexustime.core.ui.theme.NexusSurfaceHighlight
import com.example.nexustime.core.ui.theme.NexusTextSecondary
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.core.ui.theme.NexusWhite

/**
 * Punto de entrada de la pantalla: conecta el [SettingsViewModel] con la UI.
 * Deja la pantalla previewable sin ciclo de vida.
 */
@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(),
) {
    val powerAlertsEnabled by viewModel.powerAlertsEnabled.collectAsStateWithLifecycle()

    SettingsScreen(
        powerAlertsEnabled = powerAlertsEnabled,
        onPowerAlertsChanged = viewModel::setPowerAlertsEnabled,
        onPreviewVoice = viewModel::previewVoice,
        modifier = modifier,
    )
}

/**
 * Panel de Ajustes.
 *
 * v0.3: interruptor de "Alertas de Carga parlantes". El resto de preferencias
 * (frases editables, chime horario, clima) se van sumando en posteriores
 * entregas sobre este mismo contenedor.
 */
@Composable
fun SettingsScreen(
    powerAlertsEnabled: Boolean,
    onPowerAlertsChanged: (Boolean) -> Unit,
    onPreviewVoice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBlack)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            color = NexusWhite,
        )

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Alertas de carga",
            subtitle = "Corriente y batería por voz",
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alertas de Carga parlantes",
                        style = MaterialTheme.typography.titleLarge,
                        color = NexusWhite,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Anuncia la conexión/desconexión del cargador y " +
                            "cuando la batería está crítica o completa.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTextSecondary,
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Estado activo en Azul Eléctrico #1E88E5 sobre Negro OLED.
                Switch(
                    checked = powerAlertsEnabled,
                    onCheckedChange = onPowerAlertsChanged,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = NexusBlue,
                        checkedThumbColor = NexusWhite,
                        checkedBorderColor = NexusBlue,
                        uncheckedTrackColor = NexusSurfaceHighlight,
                        uncheckedThumbColor = NexusSilver,
                        uncheckedBorderColor = NexusBorder,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = onPreviewVoice) {
                Text(text = "Probar aviso de voz")
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun SettingsScreenPreview() {
    NexusTimeTheme {
        SettingsScreen(
            powerAlertsEnabled = true,
            onPowerAlertsChanged = {},
            onPreviewVoice = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
