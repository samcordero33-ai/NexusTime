package com.example.nexustime.feature.reminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.reminder.Reminder
import com.example.nexustime.core.service.reminder.ReminderScheduler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla de Recordatorios.
 *
 * Expone la lista reactiva de [NexusSettings.reminders] y centraliza las
 * mutaciones: cada alta/edición/baja se persiste en DataStore y dispara la
 * reprogramación de alarmas ([ReminderScheduler]) para que el cambio surta
 * efecto sin reiniciar.
 *
 * Necesita `Application` (por eso es [AndroidViewModel]): el planificador exige
 * un `Context` de aplicación.
 */
class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    val reminders: StateFlow<List<Reminder>> = NexusSettings.reminders

    /**
     * Inserta o actualiza un recordatorio y reprograma sus alarmas.
     *
     * Se identifica por [Reminder.id]: si existe se reemplaza, si no se añade.
     */
    fun save(reminder: Reminder) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val current = NexusSettings.currentReminders().toMutableList()
            val index = current.indexOfFirst { it.id == reminder.id }
            if (index >= 0) current[index] = reminder else current.add(reminder)

            NexusSettings.setReminders(current)
            ReminderScheduler.reschedule(context, reminder)
        }
    }

    /** Elimina un recordatorio y cancela sus alarmas. */
    fun delete(reminder: Reminder) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            val current = NexusSettings.currentReminders().filterNot { it.id == reminder.id }
            NexusSettings.setReminders(current)
            ReminderScheduler.cancel(context, reminder)
        }
    }

    /** Activa/desactiva un recordatorio sin borrarlo. */
    fun setEnabled(reminder: Reminder, enabled: Boolean) {
        save(reminder.copy(enabled = enabled))
    }
}
