package com.example.nexustime.core.service.chime

import com.example.nexustime.core.data.local.NexusSettings
import java.time.LocalTime
import java.util.Locale

/**
 * Construye el texto hablado del anuncio horario.
 *
 * La hora se escribe en formato **hablado** ("2 en punto", "2 y 15") en vez de
 * "2:00": el motor de voz puede leer los dos puntos dígito a dígito ("dos cero
 * cero"), mientras que "en punto / y quince" suena natural en español.
 */
object ChimeText {

    /** Anuncio completo para la hora actual, aplicando la plantilla del usuario. */
    suspend fun currentAnnouncement(): String {
        val template = NexusSettings.chimePhraseTemplate()
        return template.replace(NexusSettings.CHIME_HOUR_PLACEHOLDER, spokenTime(LocalTime.now()))
    }

    private fun spokenTime(time: LocalTime): String {
        val hour24 = time.hour
        val hour12 = when (val h = hour24 % 12) {
            0 -> 12
            else -> h
        }
        val period = when {
            hour24 < 12 -> "de la mañana"
            hour24 < 20 -> "de la tarde"
            else -> "de la noche"
        }
        val hourPart = if (time.minute == 0) {
            "$hour12 en punto"
        } else {
            "$hour12 y ${time.minute}"
        }
        return String.format(Locale.getDefault(), "%s %s", hourPart, period)
    }
}
