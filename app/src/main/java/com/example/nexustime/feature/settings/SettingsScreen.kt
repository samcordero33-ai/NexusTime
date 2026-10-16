package com.example.nexustime.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.nexustime.core.data.local.HourFormat
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.ScreenOnMode
import com.example.nexustime.core.ui.components.NexusPanel
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusBorder
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusSelectionIndicator
import com.example.nexustime.core.ui.theme.NexusSilver
import com.example.nexustime.core.ui.theme.NexusSurface
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
    val chimeEnabled by viewModel.chimeEnabled.collectAsStateWithLifecycle()
    val chimeInterval by viewModel.chimeInterval.collectAsStateWithLifecycle()
    val hourFormat by viewModel.hourFormat.collectAsStateWithLifecycle()
    val screenOnMode by viewModel.screenOnMode.collectAsStateWithLifecycle()

    SettingsScreen(
        powerAlertsEnabled = powerAlertsEnabled,
        onPowerAlertsChanged = viewModel::setPowerAlertsEnabled,
        onPreviewPowerAlert = viewModel::previewPowerAlert,
        chimeEnabled = chimeEnabled,
        onChimeEnabledChanged = viewModel::setChimeEnabled,
        chimeInterval = chimeInterval,
        onChimeIntervalChanged = viewModel::setChimeInterval,
        onPreviewChime = viewModel::previewChime,
        hourFormat = hourFormat,
        onHourFormatChanged = viewModel::setHourFormat,
        screenOnMode = screenOnMode,
        onScreenOnModeChanged = viewModel::setScreenOnMode,
        modifier = modifier,
    )
}

/**
 * Panel de Ajustes.
 *
 * v1.0: alertas de carga, anuncio horario, formato de hora y política de pantalla.
 * El resto de preferencias (frases editables, clima) se van sumando en posteriores
 * entregas sobre este mismo contenedor.
 */
@Composable
fun SettingsScreen(
    powerAlertsEnabled: Boolean,
    onPowerAlertsChanged: (Boolean) -> Unit,
    onPreviewPowerAlert: () -> Unit,
    chimeEnabled: Boolean,
    onChimeEnabledChanged: (Boolean) -> Unit,
    chimeInterval: Int,
    onChimeIntervalChanged: (Int) -> Unit,
    onPreviewChime: () -> Unit,
    hourFormat: HourFormat,
    onHourFormatChanged: (HourFormat) -> Unit,
    screenOnMode: ScreenOnMode,
    onScreenOnModeChanged: (ScreenOnMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Text(
            text = "Ajustes",
            style = MaterialTheme.typography.headlineMedium,
            color = NexusWhite,
        )

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Formato de hora",
            subtitle = "Cómo se muestra el reloj",
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionChip(
                    label = "Sistema",
                    selected = hourFormat == HourFormat.SYSTEM,
                    onClick = { onHourFormatChanged(HourFormat.SYSTEM) },
                )
                OptionChip(
                    label = "12 h",
                    selected = hourFormat == HourFormat.H12,
                    onClick = { onHourFormatChanged(HourFormat.H12) },
                )
                OptionChip(
                    label = "24 h",
                    selected = hourFormat == HourFormat.H24,
                    onClick = { onHourFormatChanged(HourFormat.H24) },
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Pantalla",
            subtitle = "Cuándo debe quedarse encendida",
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionChip(
                    label = "Siempre",
                    selected = screenOnMode == ScreenOnMode.ALWAYS,
                    onClick = { onScreenOnModeChanged(ScreenOnMode.ALWAYS) },
                )
                OptionChip(
                    label = "Solo al cargar",
                    selected = screenOnMode == ScreenOnMode.CHARGING_ONLY,
                    onClick = { onScreenOnModeChanged(ScreenOnMode.CHARGING_ONLY) },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Con \"Solo al cargar\", la pantalla se apaga con normalidad " +
                    "cuando el dispositivo funciona con batería.",
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Alertas de carga",
            subtitle = "Corriente y batería por voz",
            modifier = Modifier.fillMaxWidth(),
        ) {
            SwitchRow(
                title = "Alertas de Carga parlantes",
                description = "Anuncia la conexión/desconexión del cargador y " +
                    "cuando la batería está crítica o completa.",
                checked = powerAlertsEnabled,
                onCheckedChange = onPowerAlertsChanged,
            )

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = onPreviewPowerAlert) {
                Text(text = "Probar aviso de carga")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Anuncio horario",
            subtitle = "La hora en voz alta",
            modifier = Modifier.fillMaxWidth(),
        ) {
            SwitchRow(
                title = "Anunciar la hora por voz",
                description = "Lee la hora en voz alta a intervalos regulares.",
                checked = chimeEnabled,
                onCheckedChange = onChimeEnabledChanged,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "INTERVALO",
                style = MaterialTheme.typography.labelSmall,
                color = NexusTextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NexusSettings.CHIME_INTERVALS.forEach { minutes ->
                    OptionChip(
                        label = "Cada $minutes min",
                        selected = chimeInterval == minutes,
                        onClick = { onChimeIntervalChanged(minutes) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            TextButton(onClick = onPreviewChime) {
                Text(text = "Probar anuncio de hora")
            }
        }
    }
}

/** Fila reutilizable: título + descripción a la izquierda, interruptor a la derecha. */
@Composable
private fun SwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = NexusWhite,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTextSecondary,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Estado activo en Azul Eléctrico #1E88E5 sobre Negro OLED.
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
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
}

/** Chip de selección única reutilizable para las opciones de Ajustes. */
@Composable
private fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = NexusSurface,
            labelColor = NexusTextSecondary,
            selectedContainerColor = NexusSelectionIndicator,
            selectedLabelColor = NexusBlue,
        ),
    )
}

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun SettingsScreenPreview() {
    NexusTimeTheme {
        SettingsScreen(
            powerAlertsEnabled = true,
            onPowerAlertsChanged = {},
            onPreviewPowerAlert = {},
            chimeEnabled = true,
            onChimeEnabledChanged = {},
            chimeInterval = 60,
            onChimeIntervalChanged = {},
            onPreviewChime = {},
            hourFormat = HourFormat.SYSTEM,
            onHourFormatChanged = {},
            screenOnMode = ScreenOnMode.ALWAYS,
            onScreenOnModeChanged = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
