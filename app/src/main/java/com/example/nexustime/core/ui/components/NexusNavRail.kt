package com.example.nexustime.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nexustime.core.ui.navigation.NexusDestination
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusDivider
import com.example.nexustime.core.ui.theme.NexusSelectionIndicator
import com.example.nexustime.core.ui.theme.NexusSilver
import com.example.nexustime.core.ui.theme.NexusTextSecondary

/** Ancho de la barra lateral (zona izquierda del layout horizontal). */
val NexusNavRailWidth = 88.dp

/**
 * Barra lateral de navegación (zona izquierda del layout horizontal).
 *
 * Seleccionado -> azul eléctrico; sin seleccionar -> plata / metal.
 * El fondo es negro OLED continuo con el resto de la pantalla; el límite con la
 * zona central se marca con una línea plata.
 */
@Composable
fun NexusNavRail(
    destinations: List<NexusDestination>,
    selectedDestination: NexusDestination,
    onDestinationSelected: (NexusDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(NexusNavRailWidth)
            .fillMaxHeight()
            .background(NexusBlack),
    ) {
        NavigationRail(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            containerColor = Color.Transparent,
        ) {
            destinations.forEach { destination ->
                NavigationRailItem(
                    selected = destination == selectedDestination,
                    onClick = { onDestinationSelected(destination) },
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.contentDescription,
                        )
                    },
                    label = { Text(text = destination.label) },
                    alwaysShowLabel = true,
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = NexusBlue,
                        selectedTextColor = NexusBlue,
                        indicatorColor = NexusSelectionIndicator,
                        unselectedIconColor = NexusSilver,
                        unselectedTextColor = NexusTextSecondary,
                    ),
                )
            }
        }

        // Línea plata que separa la barra lateral de la zona central.
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(1.dp)
                .background(NexusDivider),
        )
    }
}
