package com.example.nexustime.feature.agenda

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexustime.core.data.calendar.CalendarEvent
import com.example.nexustime.core.data.calendar.CalendarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Estado de la agenda.
 *
 * `permissionGranted` viaja aparte de `events` para que la UI pueda distinguir
 * tres casos distintos: cargando, sin permiso (con su propia llamada a la
 * acción) y vacío real ("no hay eventos hoy").
 */
data class AgendaUiState(
    val loading: Boolean = true,
    val permissionGranted: Boolean = false,
    val events: List<CalendarEvent> = emptyList(),
)

/**
 * ViewModel de la agenda ("TU DÍA").
 *
 * Comparte la misma instancia entre el panel derecho y la pantalla de Agenda:
 * `viewModel()` usa el Activity como ViewModelStoreOwner, de modo que ambas
 * llamadas resuelven este mismo objeto y leen un único estado.
 */
class AgendaViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AgendaUiState())
    val state: StateFlow<AgendaUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    /**
     * Relee el calendario.
     *
     * Se invoca también al volver a `ON_RESUME`: así, tras conceder el permiso
     * desde el diálogo del sistema, la lista se rellena sin que el usuario
     * tenga que tocar nada más.
     */
    fun refresh() {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)

            val granted = CalendarManager.hasReadPermission(context)
            val events = if (granted) {
                withContext(Dispatchers.IO) { CalendarManager.todayEvents(context) }
            } else {
                emptyList()
            }

            _state.value = AgendaUiState(
                loading = false,
                permissionGranted = granted,
                events = events,
            )
        }
    }
}
