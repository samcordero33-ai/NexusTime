package com.example.nexustime.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nexustime.core.data.local.HourFormat
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.PowerAlertEvent
import com.example.nexustime.core.data.local.ScreenOnMode
import com.example.nexustime.core.service.chime.ChimeText
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de Ajustes.
 *
 * No introduce estado propio: expone directamente los [StateFlow] de
 * [NexusSettings]. Así los interruptores reflejan la fuente de verdad y no un
 * espejo que pueda desincronizarse de los valores que luego leen los receptores.
 *
 * Los *setters* de [NexusSettings] son `suspend` (DataStore es asíncrono), así
 * que se escriben dentro de [viewModelScope]: la corrutina se cancela sola si el
 * ViewModel muere y nunca toca la UI desde un hilo que no sea el principal.
 *
 * La (re)programación de la alarma del chime NO se hace aquí: se reacciona a los
 * cambios de preferencia desde `MainActivity`, que sí tiene el `Context` y el
 * ciclo de vida necesarios. Aquí solo se persiste el valor.
 */
class SettingsViewModel : ViewModel() {

    val powerAlertsEnabled: StateFlow<Boolean> = NexusSettings.powerAlertsEnabled
    val chimeEnabled: StateFlow<Boolean> = NexusSettings.chimeEnabled
    val chimeInterval: StateFlow<Int> = NexusSettings.chimeInterval
    val hourFormat: StateFlow<HourFormat> = NexusSettings.hourFormat
    val screenOnMode: StateFlow<ScreenOnMode> = NexusSettings.screenOnMode

    fun setPowerAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch { NexusSettings.setPowerAlertsEnabled(enabled) }
    }

    fun setChimeEnabled(enabled: Boolean) {
        viewModelScope.launch { NexusSettings.setChimeEnabled(enabled) }
    }

    fun setChimeInterval(minutes: Int) {
        viewModelScope.launch { NexusSettings.setChimeIntervalMinutes(minutes) }
    }

    fun setHourFormat(format: HourFormat) {
        viewModelScope.launch { NexusSettings.setHourFormat(format) }
    }

    fun setScreenOnMode(mode: ScreenOnMode) {
        viewModelScope.launch { NexusSettings.setScreenOnMode(mode) }
    }

    /**
     * Emite la frase de la corriente para poder probar el motor de voz desde la UI.
     *
     * Independiente del interruptor: sirve justamente para verificar que se oye
     * bien antes de dejar los avisos activados.
     */
    fun previewPowerAlert() {
        viewModelScope.launch {
            TtsManager.speak(
                text = NexusSettings.phrase(PowerAlertEvent.CONNECTED),
                flush = true,
            )
        }
    }

    /** Emite el anuncio de la hora actual para probar el chime sin esperar la alarma. */
    fun previewChime() {
        viewModelScope.launch {
            TtsManager.speak(
                text = ChimeText.currentAnnouncement(),
                flush = true,
            )
        }
    }
}
