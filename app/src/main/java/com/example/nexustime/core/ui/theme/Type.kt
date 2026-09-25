package com.example.nexustime.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Escala tipográfica de NexusTime.
 *
 * La app se ve a distancia (mesa de noche / escritorio), así que el display es
 * deliberadamente enorme y de peso fino: mucho tamaño, poca masa de tinta.
 */
val NexusTimeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Thin,
        fontSize = 96.sp,
        lineHeight = 96.sp,
        letterSpacing = (-3).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Thin,
        fontSize = 72.sp,
        lineHeight = 72.sp,
        letterSpacing = (-2).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Light,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 3.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 2.sp,
    ),
)

/**
 * Estilo del reloj gigante.
 *
 * `tnum` (tabular numbers) fija el ancho de cada dígito: sin él, el Display
 * "salta" de un lado a otro cada vez que cambia el número.
 */
val ClockTimeTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Thin,
    fontSize = 128.sp,
    lineHeight = 128.sp,
    letterSpacing = (-4).sp,
    fontFeatureSettings = "tnum",
)

/**
 * Estilo del segundero: acompaña al reloj gigante en acento azul eléctrico.
 */
val ClockSecondsTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.Light,
    fontSize = 44.sp,
    lineHeight = 44.sp,
    letterSpacing = 0.sp,
    fontFeatureSettings = "tnum",
)

/**
 * Estilo de etiquetas de sección (por ejemplo "TU DÍA"): versalitas espaciadas.
 */
val SectionLabelTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 15.sp,
    lineHeight = 20.sp,
    letterSpacing = 4.sp,
)
