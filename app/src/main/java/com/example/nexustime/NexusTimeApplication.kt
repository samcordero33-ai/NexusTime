package com.example.nexustime

import android.app.Application
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.service.chime.ChimeScheduler
import com.example.nexustime.core.service.tts.TtsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto de arranque del proceso.
 *
 * Inicializa lo que debe existir ANTES de que cualquier componente (receptor,
 * actividad, futuro chime) pida leer una preferencia o hablar:
 *
 *  * [NexusSettings] — DataStore; los receivers lo leen dentro de `goAsync()`.
 *  * [TtsManager]    — para que el motor de voz tenga tiempo de estar listo; el texto
 *                      que llegue mientras arranca se encola y se reproduce al final.
 *  * [ChimeScheduler]— (re)programa el anuncio horario con AlarmManager.
 *
 * No se arranca ningún trabajo pesado aquí: solo crear las instancias singleton
 * y dejar agendada la próxima alarma, para no penalizar el arranque de la app.
 *
 * El alcance del proceso es propio y con `SupervisorJob`: un fallo al programar
 * la primera alarma no debe tumbar el arranque ni a los demás consumidores.
 */
class NexusTimeApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NexusSettings.initialize(this)
        TtsManager.initialize(this)
        // scheduleNext lee DataStore (suspend), así que se lanza en corrutina.
        applicationScope.launch { ChimeScheduler.scheduleNext(this@NexusTimeApplication) }
    }
}
