package com.example.nexustime.feature.clock

import android.app.Application
import android.text.format.DateFormat
import androidx.lifecycle.AndroidViewModel
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
 * La hora, los segundos y el meridiano (AM/PM) viajan separados para que la UI
 * pueda darles tamaños distintos sin tener que parsear la cadena.
 */
data class ClockUiState(
    val hourMinute: String = "--:--",
    val seconds: String = "00",
    val amPm: String = "",
    val date: String = "",
    val timeZoneId: String = "",
)

/**
 * Reloj de la zona central.
 *
 * Emite una vez por segundo y se alinea con el cambio real de segundo en vez de
 * dormir 1 s a ciegas: así el display no acumula deriva al cabo de unas horas.
 *
 * El formato 12/24 h no es una preferencia propia sino la del sistema
 * (`DateFormat.is24HourFormat`), que es lo que un usuario de tablet espera.
 */
class ClockViewModel(application: Application) : AndroidViewModel(application) {

    private val use24Hour: Boolean = DateFormat.is24HourFormat(application)

    private val hourMinuteFormatter = DateTimeFormatter.ofPattern(
        if (use24Hour) "HH:mm" else "h:mm",
        Locale.getDefault(),
    )
    private val secondsFormatter = DateTimeFormatter.ofPattern("ss", Locale.getDefault())
    private val amPmFormatter = DateTimeFormatter.ofPattern("a", Locale.getDefault())
    private val dateFormatter =
        DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.getDefault())

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
        // En 24 h no se muestra meridiano.
        amPm = if (use24Hour) "" else format(amPmFormatter),
        date = format(dateFormatter).replaceFirstChar { it.uppercaseChar() },
        timeZoneId = ZoneId.systemDefault().id,
    )
}
