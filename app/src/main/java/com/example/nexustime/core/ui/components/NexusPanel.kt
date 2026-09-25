package com.example.nexustime.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nexustime.core.ui.theme.NexusBorder
import com.example.nexustime.core.ui.theme.NexusDivider
import com.example.nexustime.core.ui.theme.NexusSurface
import com.example.nexustime.core.ui.theme.NexusTextTertiary
import com.example.nexustime.core.ui.theme.NexusWhite
import com.example.nexustime.core.ui.theme.SectionLabelTextStyle

/**
 * Contenedor base de NexusTime: relleno plata translúcida sobre negro OLED y
 * borde en plata / metal (#C8C8C8), que es el token oficial de "bordes y
 * tarjetas".
 *
 * El orden de los modificadores importa: primero el relleno y después el borde,
 * para que el borde quede dibujado encima.
 */
@Composable
fun NexusPanel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    headerAccent: Color = NexusWhite,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .background(color = NexusSurface, shape = shape)
            .border(width = 1.dp, color = NexusBorder, shape = shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = SectionLabelTextStyle,
            color = headerAccent,
        )

        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTextTertiary,
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NexusDivider),
        )
        Spacer(modifier = Modifier.height(14.dp))

        content()
    }
}
