package com.example.nexustime.core.data.reminder

import org.json.JSONArray
import org.json.JSONObject

/** Naturaleza del recordatorio: texto hablado o un audio del dispositivo. */
enum class ReminderKind {
    /** Se lee un texto con [com.example.nexustime.core.service.tts.TtsManager]. */
    VOICE,

    /** Se reproduce un archivo de audio (MP3) elegido por el usuario. */
    AUDIO,
}

/**
 * Recordatorio programado.
 *
 * Un mismo recordatorio puede sonar **varias veces al día**: [timesOfDay] guarda
 * cada hora como minutos desde medianoche (0..1439), de modo que el *scheduler*
 * agenda una alarma por cada hora.
 */
data class Reminder(
    val id: Long,
    val label: String,
    val kind: ReminderKind,
    /** Texto a leer cuando [kind] es [ReminderKind.VOICE]. */
    val message: String,
    /** URI del audio cuando [kind] es [ReminderKind.AUDIO]; `null` en otro caso. */
    val audioUri: String?,
    /** Horas del día en minutos (0..1439), sin duplicados y ordenadas. */
    val timesOfDay: List<Int>,
    val enabled: Boolean,
) {
    /** Etiqueta legible para la lista (cae al texto o al archivo si falta). */
    val displayName: String
        get() = when {
            label.isNotBlank() -> label
            kind == ReminderKind.VOICE && message.isNotBlank() -> message
            else -> "Recordatorio"
        }
}

/**
 * Serializa la lista de recordatorios a/desde JSON para DataStore.
 *
 * DataStore Preferences solo guarda tipos primitivos y `String`, así que la lista
 * se codifica como un array JSON. Se usa `org.json` (incluido en el SDK) para no
 * añadir kotlinx.serialization solo por este campo.
 *
 * El decodificador es **tolerante**: si el JSON está corrupto o a medias, devuelve
 * una lista vacía en lugar de tumbar la lectura de preferencias.
 */
object ReminderCodec {

    fun encode(reminders: List<Reminder>): String {
        val array = JSONArray()
        reminders.forEach { reminder ->
            val obj = JSONObject().apply {
                put("id", reminder.id)
                put("label", reminder.label)
                put("kind", reminder.kind.name)
                put("message", reminder.message)
                put("audioUri", reminder.audioUri ?: JSONObject.NULL)
                put("times", JSONArray(reminder.timesOfDay))
                put("enabled", reminder.enabled)
            }
            array.put(obj)
        }
        return array.toString()
    }

    fun decode(raw: String?): List<Reminder> {
        if (raw.isNullOrBlank()) return emptyList()

        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(obj.toReminder())
                }
            }
        } catch (_: Exception) {
            // Preferencia ilegible: se ignora y se sigue con la lista vacía.
            emptyList()
        }
    }

    private fun JSONObject.toReminder(): Reminder {
        val kind = runCatching { ReminderKind.valueOf(getString("kind")) }
            .getOrDefault(ReminderKind.VOICE)

        val timesArray = optJSONArray("times") ?: JSONArray()
        val times = buildList {
            for (j in 0 until timesArray.length()) add(timesArray.getInt(j))
        }.filter { it in 0..1439 }.distinct().sorted()

        val rawUri = optString("audioUri", "")
        return Reminder(
            id = optLong("id", 0L),
            label = optString("label", ""),
            kind = kind,
            message = optString("message", ""),
            audioUri = rawUri.takeIf { it.isNotBlank() && it != "null" },
            timesOfDay = times,
            enabled = optBoolean("enabled", true),
        )
    }
}
