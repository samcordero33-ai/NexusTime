package com.example.nexustime.core.service.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Envoltorio compartido sobre [TextToSpeech] para toda NexusTime.
 *
 * El motor tarda unos milisegundos en arrancar (a veces más si acaba de
 * instalarse la app). Cualquier texto pedido antes de que responda `onInit` se
 * encola y se reproduce en cuanto el motor queda listo: así un evento de corriente
 * que llegue justo al abrir la app no se pierde.
 *
 * Se mantiene UN único `TextToSpeech` ligado al Application en lugar de crear uno
 * por BroadcastReceiver: instanciarlo y destruirlo en cada evento es caro y produce
 * recortes de audio.
 *
 * Es la base de voz que también usarán el chime horario (v0.2), las alarmas (v0.5)
 * y el reporte de clima (v0.4).
 */
object TtsManager {

    private const val TAG = "NexusTts"
    private const val UTTERANCE_ID = "nexustime"

    /** Español neutro. Si el dispositivo no lo trae, se cae al idioma por defecto. */
    private val locale = Locale("es")

    private var tts: TextToSpeech? = null
    private var isReady = false
    private val pending = ArrayDeque<String>()

    /**
     * Crea el motor una sola vez. Idempotente: llamarlo varias veces no duplica
     * el TextToSpeech.
     */
    fun initialize(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status -> onInitialized(status) }
    }

    @Synchronized
    private fun onInitialized(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech no disponible (status=$status)")
            return
        }

        if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        }
        engine.setPitch(1.0f)
        engine.setSpeechRate(1.0f)

        isReady = true
        while (pending.isNotEmpty()) {
            engine.speak(pending.removeFirst(), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
        }
    }

    /**
     * Habla [text].
     *
     * @param flush si es `true` corta lo que esté sonando (bueno para avisos que
     *              deben oíse completos); si es `false` se añade a la cola.
     */
    @Synchronized
    fun speak(text: String, flush: Boolean = false) {
        if (text.isBlank()) return

        val engine = tts
        if (engine == null || !isReady) {
            if (flush) pending.clear()
            pending.addLast(text)
            return
        }

        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        engine.speak(text, mode, null, UTTERANCE_ID)
    }

    /** Detiene la voz actual y descarta lo encolado. */
    @Synchronized
    fun stop() {
        pending.clear()
        tts?.stop()
    }

    /** Libera el motor. Solo tiene sentido si el proceso va a terminar. */
    @Synchronized
    fun shutdown() {
        pending.clear()
        isReady = false
        tts?.shutdown()
        tts = null
    }
}
