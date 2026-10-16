package com.example.nexustime

import android.app.Application
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.service.tts.TtsManager

/**
 * Punto de arranque del proceso.
 *
 * Inicializa lo que debe existir ANTES de que cualquier componente (receptor,
 * actividad, futuro chime) pida leer una preferencia o hablar:
 *
 *  * [NexusSettings] — para que el receiver pueda leer el interruptor de forma síncrona.
 *  * [TtsManager]    — para que el motor de voz tenga tiempo de estar listo; el texto
 *                      que llegue mientras arranca se encola y se reproduce al final.
 *
 * No se arranca ningún trabajo pesado aquí: solo crear las instancias singleton,
 * para no penalizar el arranque de la app.
 */
class NexusTimeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NexusSettings.initialize(this)
        TtsManager.initialize(this)
    }
}
