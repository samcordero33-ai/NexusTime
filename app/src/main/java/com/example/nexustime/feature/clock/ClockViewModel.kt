package com.example.nexustime.feature.clock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Estado de la pantalla de reloj.
 *
 * La hora y los segundos viajan separados para que la UI pueda darles tamaños
 * distintos sin tener que parsear la cadena.
 */
data class ClockUiState(
    val hourMinute: String = "--:--",
    val seconds: String = "00",
    val date: String = "",
    val timeZoneId: String = "",
)

/**
 * Reloj de la zona central.
 *
 * Emite una vez por segundo y se alinea con el cambio real de segundo en vez de
 * dormir 1 s a ciegas: así el display no acumula deriva al cabo de unas horas.
 */
class ClockViewModel : ViewModel() {

    private val hourMinuteFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val secondsFormatter = DateTimeFormatter.ofPattern("ss", Locale.getDefault())
    private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.getDefault())

    val uiState: StateFlow<ClockUiState> = tickerFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = LocalDateTime.now().toClockUiState(),
        )

    private fun tickerFlow() = flow {
        while (true) {
            emit(LocalDateTime.now().toClockUiState())
            val millisToNextSecond = 1_000L - (System.currentTimeMillis() % 1_000L)
            delay(millisToNextSecond)
        }
    }

    private fun LocalDateTime.toClockUiState(): ClockUiState = ClockUiState(
        hourMinute = format(hourMinuteFormatter),
        seconds = format(secondsFormatter),
        date = format(dateFormatter).replaceFirstChar { it.uppercaseChar() },
        timeZoneId = ZoneId.systemDefault().id,
    )
}
