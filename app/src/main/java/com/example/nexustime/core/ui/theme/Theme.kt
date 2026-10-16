package com.example.nexustime.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/*
 * NexusTime es un reloj de mesa: se usa en mesitas de noche y escritorios, con
 * luz baja. Por eso la app es unicamente oscura y no reacciona al tema del
 * sistema: un fondo claro arruinaria el proposito del producto y el ahorro de
 * bateria de las pantallas OLED.
 *
 * Mapeo de la paleta oficial al esquema de Material 3:
 *   background      -> Negro OLED    (fondo base)
 *   onBackground    -> Blanco Puro   (textos principales)
 *   primary         -> Azul Electrico
 *   secondary       -> Celeste
 *   surface/outline -> Plata / Metal
 *
 * Nota: `surface` queda mapeado al token Plata porque es el color oficial de
 * "bordes y tarjetas". En la UI las tarjetas se pintan con [NexusSurface]
 * (plata translucida sobre OLED) y borde plata, de modo que el metal enmarque
 * el contenido sin tapar el negro de fondo.
 */
private val NexusTimeColorScheme = darkColorScheme(
    primary = NexusBlue,
    onPrimary = NexusWhite,
    primaryContainer = NexusBlue.copy(alpha = 0.18f),
    onPrimaryContainer = NexusSkyBlue,

    secondary = NexusSkyBlue,
    onSecondary = NexusBlack,
    secondaryContainer = NexusSkyBlue.copy(alpha = 0.14f),
    onSecondaryContainer = NexusSkyBlue,

    tertiary = NexusSkyBlue,
    onTertiary = NexusBlack,

    background = NexusBlack,
    onBackground = NexusWhite,

    /*
     * `surface`/`surfaceVariant` se mapean a los tokens oscuros (plata translúcida
     * sobre OLED), no al metal puro: los componentes Material que dependen de
     * ellos (diálogos, campos de texto, TimePicker) necesitan un fondo oscuro y
     * texto claro para leerse sobre la interfaz OLED. El metal puro se reserva a
     * bordes/tarjetas, que se pintan con los tokens NexusSurface/NexusBorder.
     */
    surface = NexusSurfaceHighlight,
    onSurface = NexusWhite,
    surfaceVariant = NexusSurfaceHighlight,
    onSurfaceVariant = NexusTextSecondary,
    surfaceTint = Color.Transparent,

    outline = NexusSilver,
    outlineVariant = NexusSilver,
)

/**
 * Tokens semánticos de NexusTime. Son atajos a la paleta oficial para que los
 * composables no tengan que recordar cuándo usar primario, secundario o plata.
 */
@Immutable
object NexusTimeTokens {
    /** Fondo de cualquier pantalla. */
    val background: Color get() = NexusBlack

    /** Relleno de tarjetas y paneles. */
    val card: Color get() = NexusSurface

    /** Borde de tarjetas y paneles. */
    val cardBorder: Color get() = NexusBorder

    /** Texto principal. */
    val textPrimary: Color get() = NexusWhite

    /** Texto de apoyo. */
    val textSecondary: Color get() = NexusTextSecondary

    /** Acento primario (azul eléctrico). */
    val accent: Color get() = NexusBlue

    /** Estado / widgets (celeste). */
    val status: Color get() = NexusSkyBlue
}

object NexusTimeTheme {
    /** Acento primario resuelto dentro de la composición. */
    val accent: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.primary

    /** Estado / widgets resuelto dentro de la composición. */
    val status: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.secondary
}

@Composable
fun NexusTimeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NexusTimeColorScheme,
        typography = NexusTimeTypography,
        content = content,
    )
}
