package com.example.nexustime.core.service.reminder

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log

/**
 * Reproductor de audio para los recordatorios de tipo [ReminderKind.AUDIO][com.example.nexustime.core.data.reminder.ReminderKind.AUDIO].
 *
 * Mantiene UN único [MediaPlayer] de proceso (por el mismo motivo que
 * [com.example.nexustime.core.service.tts.TtsManager]): crear y destruir uno por
 * evento es caro. Al terminar o fallar, se libera solo.
 *
 * El [Uri] apunta a un documento elegido con el *file picker* del sistema; la app
 * conserva el permiso de lectura persistente (`takePersistableUriPermission`), por
 * eso [MediaPlayer.setDataSource] puede abrirlo aunque el proceso se haya reiniciado.
 */
object AudioPlayer {

    private const val TAG = "NexusAudio"

    private var player: MediaPlayer? = null

    /** Reproduce [uriString], cortando cualquier reproducción anterior. */
    @Synchronized
    fun play(context: Context, uriString: String) {
        stop()

        try {
            val uri = Uri.parse(uriString)
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setOnCompletionListener {
                    Log.d(TAG, "Audio de recordatorio finalizado")
                    stop()
                }
                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "Error de MediaPlayer (what=$what, extra=$extra)")
                    stop()
                    true
                }
                prepare()
                start()
            }
            player = mediaPlayer
            Log.d(TAG, "Reproduciendo recordatorio: $uriString")
        } catch (e: Exception) {
            // Fuente inaccesible, formato no soportado, permiso revocado, etc.
            Log.w(TAG, "No se pudo reproducir el audio del recordatorio", e)
            stop()
        }
    }

    /** Detiene y libera el reproductor actual, si lo hay. */
    @Synchronized
    fun stop() {
        player?.let { mp ->
            runCatching { if (mp.isPlaying) mp.stop() }
            runCatching { mp.reset() }
            runCatching { mp.release() }
        }
        player = null
    }
}
