package com.example.nexustime.feature.reminder

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexustime.core.data.reminder.Reminder
import com.example.nexustime.core.data.reminder.ReminderKind
import com.example.nexustime.core.ui.components.NexusPanel
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusBorder
import com.example.nexustime.core.ui.theme.NexusSelectionIndicator
import com.example.nexustime.core.ui.theme.NexusSilver
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusSurface
import com.example.nexustime.core.ui.theme.NexusSurfaceHighlight
import com.example.nexustime.core.ui.theme.NexusTextSecondary
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.core.ui.theme.NexusWhite
import java.util.Locale

/**
 * Punto de entrada de la pantalla central de Recordatorios.
 */
@Composable
fun ReminderRoute(
    modifier: Modifier = Modifier,
    viewModel: ReminderViewModel = viewModel(),
) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    ReminderScreen(
        reminders = reminders,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onToggle = viewModel::setEnabled,
        modifier = modifier,
    )
}

/**
 * Pantalla de Recordatorios programados.
 *
 * Arriba, un formulario para crear uno nuevo: etiqueta, tipo (voz o audio),
 * contenido y una o varias horas del día. Abajo, la lista de los ya programados
 * con interruptor para activar/desactivar y botón de borrado.
 *
 * El archivo de audio se elige con el *file picker* del sistema (SAF) y se guarda
 * su URI con **permiso de lectura persistente**, para poder reproducirlo más tarde
 * aunque el proceso se reinicie.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(
    reminders: List<Reminder>,
    onSave: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    var label by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(ReminderKind.VOICE) }
    var message by remember { mutableStateOf("") }
    var audioUri by remember { mutableStateOf<String?>(null) }
    var audioName by remember { mutableStateOf<String?>(null) }
    var times by remember { mutableStateOf<List<Int>>(emptyList()) }
    var showTimePicker by remember { mutableStateOf(false) }

    val audioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            // Permiso persistente: sobrevive a reinicios del proceso.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            audioUri = uri.toString()
            audioName = uri.lastPathSegment ?: uri.toString()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NexusBlack)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 28.dp),
    ) {
        Text(
            text = "Recordatorios",
            style = MaterialTheme.typography.headlineMedium,
            color = NexusWhite,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Avisos por voz o audio a las horas que elijas",
            style = MaterialTheme.typography.bodyMedium,
            color = NexusTextSecondary,
        )

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Nuevo recordatorio",
            subtitle = "Se repetirá cada día en las horas indicadas",
            headerAccent = NexusSkyBlue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Etiqueta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "TIPO",
                style = MaterialTheme.typography.labelSmall,
                color = NexusTextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OptionChip(
                    label = "Voz (texto)",
                    selected = kind == ReminderKind.VOICE,
                    onClick = { kind = ReminderKind.VOICE },
                )
                OptionChip(
                    label = "Audio (MP3)",
                    selected = kind == ReminderKind.AUDIO,
                    onClick = { kind = ReminderKind.AUDIO },
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (kind == ReminderKind.VOICE) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Oración a leer en voz alta") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { audioPicker.launch(arrayOf("audio/*")) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NexusBlue,
                            contentColor = NexusWhite,
                        ),
                    ) {
                        Text(text = "Elegir archivo MP3")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = audioName ?: "Ningún archivo seleccionado",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (audioName == null) NexusTextSecondary else NexusWhite,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "HORAS",
                style = MaterialTheme.typography.labelSmall,
                color = NexusTextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                times.forEach { minute ->
                    FilterChip(
                        selected = true,
                        onClick = { times = times - minute },
                        label = { Text(text = formatMinute(minute)) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Quitar hora",
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = NexusSelectionIndicator,
                            labelColor = NexusBlue,
                            selectedContainerColor = NexusSelectionIndicator,
                            selectedLabelColor = NexusBlue,
                        ),
                    )
                }

                TextButton(onClick = { showTimePicker = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Añadir hora")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SaveButton(
                enabled = canSave(kind, message, audioUri) && times.isNotEmpty(),
                onClick = {
                    onSave(
                        Reminder(
                            id = System.currentTimeMillis(),
                            label = label.trim(),
                            kind = kind,
                            message = message.trim(),
                            audioUri = audioUri,
                            timesOfDay = times.distinct().sorted(),
                            enabled = true,
                        ),
                    )
                    // Limpia el formulario tras guardar.
                    label = ""
                    message = ""
                    audioUri = null
                    audioName = null
                    times = emptyList()
                },
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        NexusPanel(
            title = "Programados",
            subtitle = "Recordatorios activos",
            headerAccent = NexusSkyBlue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (reminders.isEmpty()) {
                Text(
                    text = "Todavía no hay recordatorios",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTextSecondary,
                )
            } else {
                reminders.forEach { reminder ->
                    ReminderCard(
                        reminder = reminder,
                        onToggle = { onToggle(reminder, it) },
                        onDelete = { onDelete(reminder) },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(text = "Añadir hora") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val minute = timeState.hour * 60 + timeState.minute
                        times = (times + minute).distinct().sorted()
                        showTimePicker = false
                    },
                ) {
                    Text(text = "Añadir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(text = "Cancelar")
                }
            },
        )
    }
}

/** Botón de guardar con estilo de acento. */
@Composable
private fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = NexusBlue,
            contentColor = NexusWhite,
        ),
    ) {
        Text(text = "Guardar recordatorio")
    }
}

/** Tarjeta de un recordatorio programado. */
@Composable
private fun ReminderCard(
    reminder: Reminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = NexusSurface, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = NexusWhite,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(if (reminder.kind == ReminderKind.VOICE) "Voz · " else "Audio · ")
                        append(reminder.timesOfDay.joinToString(" · ") { formatMinute(it) })
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTextSecondary,
                )
            }

            Switch(
                checked = reminder.enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = NexusBlue,
                    checkedThumbColor = NexusWhite,
                    checkedBorderColor = NexusBlue,
                    uncheckedTrackColor = NexusSurfaceHighlight,
                    uncheckedThumbColor = NexusSilver,
                    uncheckedBorderColor = NexusBorder,
                ),
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Eliminar recordatorio",
                    tint = NexusTextSecondary,
                )
            }
        }
    }
}

/** Chip de selección única reutilizable. */
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

/** `true` si el contenido del recordatorio es válido según su tipo. */
private fun canSave(
    kind: ReminderKind,
    message: String,
    audioUri: String?,
): Boolean = when (kind) {
    ReminderKind.VOICE -> message.isNotBlank()
    ReminderKind.AUDIO -> !audioUri.isNullOrBlank()
}

/** Minutos desde medianoche -> "HH:mm". */
private fun formatMinute(minuteOfDay: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", minuteOfDay / 60, minuteOfDay % 60)

@Preview(showBackground = true, widthDp = 900, heightDp = 800)
@Composable
private fun ReminderScreenPreview() {
    NexusTimeTheme {
        ReminderScreen(
            reminders = listOf(
                Reminder(
                    id = 1L,
                    label = "Medicación",
                    kind = ReminderKind.VOICE,
                    message = "Es la hora de la medicación",
                    audioUri = null,
                    timesOfDay = listOf(8 * 60, 20 * 60),
                    enabled = true,
                ),
            ),
            onSave = {},
            onDelete = {},
            onToggle = { _, _ -> },
        )
    }
}
