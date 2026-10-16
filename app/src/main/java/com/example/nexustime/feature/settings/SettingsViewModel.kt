package com.example.nexustime.feature.settings

import androidx.lifecycle.ViewModel
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.PowerAlertEvent
import com.example.nexustime.core.service.chime.ChimeText
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado de la pantalla de Ajustes.
 *
 * No introduce estado propio: expone directamente los [StateFlow] de
 * [NexusSettings]. Así los interruptores reflejan la fuente de verdad y no un
 * espejo que pueda desincronizarse de los valores que luego leen los receptores.
 *
 * La (re)programación de la alarma del chime NO se hace aquí: se reacciona a los
 * cambios de preferencia desde `MainActivity`, que sí tiene el `Context` y el
 * ciclo de vida necesarios. Aquí solo se persiste el valor.
 */
class SettingsViewModel : ViewModel() {

    val powerAlertsEnabled: StateFlow<Boolean> = NexusSettings.powerAlertsEnabled
    val chimeEnabled: StateFlow<Boolean> = NexusSettings.chimeEnabled
    val chimeInterval: StateFlow<Int> = NexusSettings.chimeInterval

    fun setPowerAlertsEnabled(enabled: Boolean) {
        NexusSettings.setPowerAlertsEnabled(enabled)
    }

    fun setChimeEnabled(enabled: Boolean) {
        NexusSettings.setChimeEnabled(enabled)
    }

    fun setChimeInterval(minutes: Int) {
        NexusSettings.setChimeIntervalMinutes(minutes)
    }

    /**
     * Emite la frase de la corriente para poder probar el motor de voz desde la UI.
     *
     * Independiente del interruptor: sirve justamente para verificar que se oye
     * bien antes de dejar los avisos activados.
     */
    fun previewPowerAlert() {
        TtsManager.speak(
            text = NexusSettings.phrase(PowerAlertEvent.CONNECTED),
            flush = true,
        )
    }

    /** Emite el anuncio de la hora actual para probar el chime sin esperar la alarma. */
    fun previewChime() {
        TtsManager.speak(
            text = ChimeText.currentAnnouncement(),
            flush = true,
        )
    }
}
