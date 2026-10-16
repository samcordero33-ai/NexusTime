package com.example.nexustime.feature.clock

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nexustime.core.ui.components.rememberBurnInOffset
import com.example.nexustime.core.ui.theme.ClockSecondsTextStyle
import com.example.nexustime.core.ui.theme.ClockTimeTextStyle
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusBlue
import com.example.nexustime.core.ui.theme.NexusGlow
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
 * Zona central: display del reloj gigante con aura de neón.
 *
 * El glow se construye en dos capas, ambas derivadas del celeste oficial
 * (#80D8FF): un degradado radial que late suavemente detrás de los dígitos y un
 * `Shadow` sobre el propio texto. Todo sobre negro OLED (#000000).
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
        // Micro-desplazamiento periódico para mitigar el burn-in del OLED.
        val burnInOffset = rememberBurnInOffset()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { burnInOffset },
            contentAlignment = Alignment.Center,
        ) {
            // Aura: degradado radial animado detrás del display.
            ClockAura(modifier = Modifier.align(Alignment.Center))

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
                        style = ClockTimeTextStyle.copy(shadow = NeonGlow),
                        color = NexusWhite,
                    )
                    Text(
                        text = ":${uiState.seconds}",
                        style = ClockSecondsTextStyle.copy(shadow = NeonGlowBlue),
                        color = NexusBlue,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
                    if (uiState.amPm.isNotBlank()) {
                        Text(
                            text = uiState.amPm,
                            style = MaterialTheme.typography.titleLarge,
                            color = NexusSkyBlue,
                            modifier = Modifier.padding(start = 12.dp, bottom = 28.dp),
                        )
                    }
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

                Spacer(modifier = Modifier.height(28.dp))

                // Indicadores en miniatura: batería / carga y estado del chime.
                ClockStatusIndicators()
            }
        }
    }
}

/**
 * Aura de neón: degradado radial que respira detrás de los dígitos.
 *
 * La opacidad oscila entre 10% y 28% en ~2.8 s: lo suficiente para percibir el
 * "pulso" sin resultar molesto en una mesita de noche.
 */
@Composable
private fun ClockAura(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "clockAura")
    val alpha by transition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "clockAuraAlpha",
    )

    Box(
        modifier = modifier
            .size(width = 760.dp, height = 420.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(NexusGlow.copy(alpha = alpha), Color.Transparent),
                ),
            ),
    )
}

/*
 * Glow del texto: un `Shadow` celeste muy difuso sobre los dígitos en blanco y
 * azul. A diferencia de Modifier.blur(), funciona en todos los niveles de API
 * soportados (minSdk 26), porque lo rasteriza el propio framework de texto.
 */
private val NeonGlow = Shadow(
    color = NexusSkyBlue.copy(alpha = 0.75f),
    blurRadius = 26f,
)

private val NeonGlowBlue = Shadow(
    color = NexusBlue.copy(alpha = 0.85f),
    blurRadius = 22f,
)

@Preview(showBackground = true, widthDp = 900, heightDp = 600)
@Composable
private fun ClockScreenPreview() {
    NexusTimeTheme {
        ClockScreen(
            uiState = ClockUiState(
                hourMinute = "21:47",
                seconds = "32",
                amPm = "",
                date = "Jueves, 25 de septiembre",
                timeZoneId = "America/Mexico_City",
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
