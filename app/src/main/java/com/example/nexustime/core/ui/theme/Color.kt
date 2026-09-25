package com.example.nexustime.core.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * PALETA OFICIAL NEXUSTIME v0.1
 * ---------------------------------------------------------------------------
 * Estos cinco valores son la fuente de verdad del diseno. Ningun composable
 * debe declarar un color literal: se consume siempre desde aqui.
 *
 *  Fondo base (Background) .............. Negro OLED ..... #000000
 *  Textos principales (OnBackground) .... Blanco Puro .... #FFFFFF
 *  Acentos primarios (Primary) ......... Azul Electrico . #1E88E5
 *  Widgets y estado (Secondary) ........ Celeste ........ #80D8FF
 *  Bordes y tarjetas (Surface) ......... Plata / Metal .. #C8C8C8
 */

/** Fondo base de toda la aplicacion. */
val NexusBlack = Color(0xFF000000)

/** Color de los textos principales sobre el fondo OLED. */
val NexusWhite = Color(0xFFFFFFFF)

/** Acento primario: acciones, indicadores, segundero. */
val NexusBlue = Color(0xFF1E88E5)

/** Acento secundario: widgets y informacion de estado. */
val NexusSkyBlue = Color(0xFF80D8FF)

/** Plata / metal: bordes y tarjetas. */
val NexusSilver = Color(0xFFC8C8C8)

/*
 * DERIVADOS
 * Son la plata oficial compuesta sobre el negro OLED. Se usan para rellenos de
 * tarjeta y jerarquia de texto sin salirse de la paleta: el mismo #C8C8C8 con
 * distinta opacidad sobre #000000.
 */

/** Relleno de tarjeta / panel (plata al 7% sobre OLED). */
val NexusSurface = NexusSilver.copy(alpha = 0.07f)

/** Relleno de elemento resaltado (plata al 13% sobre OLED). */
val NexusSurfaceHighlight = NexusSilver.copy(alpha = 0.13f)

/** Borde de tarjeta: plata / metal. */
val NexusBorder = NexusSilver.copy(alpha = 0.45f)

/** Borde enfatizado (elemento activo o focus). */
val NexusBorderStrong = NexusSilver.copy(alpha = 0.90f)

/** Separadores. */
val NexusDivider = NexusSilver.copy(alpha = 0.22f)

/** Texto secundario (plata al 80% sobre OLED). */
val NexusTextSecondary = NexusSilver.copy(alpha = 0.80f)

/** Texto terciario / deshabilitado (plata al 55% sobre OLED). */
val NexusTextTertiary = NexusSilver.copy(alpha = 0.55f)

/** Fondo del indicador de seleccion en la barra lateral. */
val NexusSelectionIndicator = NexusBlue.copy(alpha = 0.18f)
