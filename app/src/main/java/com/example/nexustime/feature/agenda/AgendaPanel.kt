package com.example.nexustime.feature.agenda

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nexustime.core.ui.components.NexusPanel
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusTextSecondary
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import com.example.nexustime.core.ui.theme.NexusWhite

/** Ancho del panel derecho (zona de la agenda). */
val AgendaPanelWidth = 320.dp

/**
 * Panel derecho: contenedor de la futura tarjeta "TU DÍA".
 *
 * En v0.1 solo se maqueta el contenedor con borde plata / metal (#C8C8C8) y un
 * estado vacío. La conexión con el calendario del dispositivo llega en la v0.2;
 * aquí no se lee ningún dato todavía.
 */
@Composable
fun AgendaPanel(
    modifier: Modifier = Modifier,
) {
    NexusPanel(
        title = "Tu día",
        subtitle = "Agenda del día",
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.EventNote,
                contentDescription = null,
                tint = NexusSkyBlue,
                modifier = Modifier.size(44.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sin eventos",
                style = MaterialTheme.typography.titleLarge,
                color = NexusWhite,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "La agenda se conectará en la v0.2",
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTextSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun AgendaPanelPreview() {
    NexusTimeTheme {
        AgendaPanel(modifier = Modifier.fillMaxWidth())
    }
}
