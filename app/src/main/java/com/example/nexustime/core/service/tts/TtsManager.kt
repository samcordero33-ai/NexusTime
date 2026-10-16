package com.example.nexustime.core.service.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * Envoltorio compartido sobre [TextToSpeech] para toda NexusTime.
 *
 * El motor tarda unos milisegundos en arrancar (a veces más si acaba de
 * instalarse la app o el dispositivo está ocupado). Cualquier texto pedido antes
 * de que responda `onInit` se **encola** y se reproduce en cuanto el motor queda
 * listo: así un evento de corriente o un recordatorio que llegue justo al abrir
 * la app no se pierde.
 *
 * Si el motor falla al inicializar (`status != SUCCESS`), se **reintenta** solo
 * unas pocas veces con un pequeño retraso, en vez de quedarse mudo para siempre.
 * Un fallo de `onInit` es transitorio (el servicio de TTS puede estar
 * arrancando), así que el reintento es la política correcta.
 *
 * Se mantiene UN único `TextToSpeech` ligado al Application en lugar de crear uno
 * por BroadcastReceiver: instanciarlo y destruirlo en cada evento es caro y produce
 * recortes de audio.
 *
 * Es la base de voz del chime horario, los avisos de carga, la lectura de noticias
 * y los recordatorios.
 */
object TtsManager {

    private const val TAG = "NexusTts"
    private const val UTTERANCE_ID = "nexustime"

    /** Nº máximo de intentos de inicialización antes de darse por vencido. */
    private const val MAX_INIT_ATTEMPTS = 3

    /** Retraso entre reintentos de inicialización. */
    private const val RETRY_DELAY_MS = 2_000L

    /** Español neutro. Si el dispositivo no lo trae, se cae al idioma por defecto. */
    private val locale = Locale("es")

    private val handler = Handler(Looper.getMainLooper())

    /** Contexto de aplicación; se guarda para poder reintentar desde [speak]. */
    private var appContext: Context? = null

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var initAttempts = 0
    private val pending = ArrayDeque<String>()

    /**
     * Crea el motor una sola vez. Idempotente: llamarlo varias veces no duplica
     * el TextToSpeech. Si ya se alcanzó el máximo de intentos, no hace nada.
     */
    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (tts != null || initAttempts >= MAX_INIT_ATTEMPTS) return
        initAttempts++
        Log.d(TAG, "Inicializando TextToSpeech (intento $initAttempts/$MAX_INIT_ATTEMPTS)")
        tts = TextToSpeech(appContext) { status -> onInitialized(status) }
    }

    @Synchronized
    private fun onInitialized(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "TextToSpeech no disponible (status=$status); reintentando")
            tts?.shutdown()
            tts = null
            scheduleRetry()
            return
        }

        val engine = tts ?: return

        if (engine.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            engine.language = locale
        }
        engine.setPitch(1.0f)
        engine.setSpeechRate(1.0f)

        isReady = true
        initAttempts = 0
        Log.d(TAG, "TextToSpeech listo; vaciando cola (${pending.size} en espera)")

        while (pending.isNotEmpty()) {
            engine.speak(pending.removeFirst(), TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID)
        }
    }

    /** Reintenta la inicialización tras un pequeño retraso, si quedan intentos. */
    private fun scheduleRetry() {
        if (initAttempts >= MAX_INIT_ATTEMPTS) {
            Log.e(TAG, "TextToSpeech no pudo inicializarse tras $MAX_INIT_ATTEMPTS intentos")
            return
        }
        handler.postDelayed(
            { appContext?.let { initialize(it) } },
            RETRY_DELAY_MS,
        )
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
            // Motor todavía arrancando (o caído): se encola y no se pierde.
            if (flush) pending.clear()
            pending.addLast(text)
            Log.d(TAG, "TTS aún no listo; encolado (${pending.size} pendientes): $text")
            // Si el motor nunca arrancó, intentamos levantarlo de nuevo.
            if (engine == null) appContext?.let { initialize(it) }
            return
        }

        Log.d(TAG, "TTS speak (flush=$flush): $text")
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
