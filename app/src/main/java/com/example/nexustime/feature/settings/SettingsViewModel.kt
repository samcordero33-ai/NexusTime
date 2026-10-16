package com.example.nexustime.feature.settings

import androidx.lifecycle.ViewModel
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.PowerAlertEvent
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.flow.StateFlow

/**
 * Estado de la pantalla de Ajustes.
 *
 * No introduce estado propio: expone directamente el [StateFlow] de
 * [NexusSettings]. Así el interruptor refleja la fuente de verdad y no un espejo
 * que pueda desincronizarse del valor que luego lee el receiver.
 */
class SettingsViewModel : ViewModel() {

    val powerAlertsEnabled: StateFlow<Boolean> = NexusSettings.powerAlertsEnabled

    fun setPowerAlertsEnabled(enabled: Boolean) {
        NexusSettings.setPowerAlertsEnabled(enabled)
    }

    /**
     * Emite la frase de la corriente para poder probar el motor de voz desde la UI.
     *
     * Independiente del interruptor: sirve justamente para verificar que se oye
     * bien antes de dejar los avisos activados.
     */
    fun previewVoice() {
        TtsManager.speak(
            text = NexusSettings.phrase(PowerAlertEvent.CONNECTED),
            flush = true,
        )
    }
}
