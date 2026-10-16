package com.example.nexustime.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/*
 * Las ocho direcciones alrededor del centro, más el centro, en unidades
 * enteras de desplazamiento. El orden recorre el contorno en sentido horario,
 * de modo que el movimiento es continuo y no "salta" de un extremo a otro.
 */
private val BurnInSteps: List<Pair<Int, Int>> = listOf(
    0 to 0,
    1 to 0,
    1 to 1,
    0 to 1,
    -1 to 1,
    -1 to 0,
    -1 to -1,
    0 to -1,
    1 to -1,
)

/**
 * Micro-desplazamiento periódico para mitigar el **burn-in** de pantallas OLED.
 *
 * Un reloj de mesa deja los mismos píxeles encendidos durante horas; sobre OLED
 * eso termina quemando la imagen. Mover el contenido unos pocos píxeles cada
 * cierto tiempo reparte el desgaste sin que el ojo lo note.
 *
 * Devuelve un [IntOffset] en píxeles que cambia cada [intervalMillis], siguiendo
 * el patrón de [BurnInSteps]. La magnitud va en [maxShift] (por defecto 3 dp),
 * suficiente para repartir el desgaste y a la vez imperceptible.
 *
 * El desplazamiento se aplica con `Modifier.offset { ... }` (versión lambda), que
 * solo recalcula posición en la fase de layout y no fuerza recomposición.
 */
@Composable
fun rememberBurnInOffset(
    intervalMillis: Long = 120_000L,
    maxShift: Dp = 3.dp,
): IntOffset {
    val density = LocalDensity.current
    val maxShiftPx = with(density) { maxShift.roundToPx() }

    var index by remember { mutableStateOf(0) }

    LaunchedEffect(intervalMillis, maxShiftPx) {
        if (maxShiftPx <= 0) return@LaunchedEffect
        while (true) {
            delay(intervalMillis)
            index = (index + 1) % BurnInSteps.size
        }
    }

    val (dx, dy) = BurnInSteps[index]
    return IntOffset(x = dx * maxShiftPx, y = dy * maxShiftPx)
}
