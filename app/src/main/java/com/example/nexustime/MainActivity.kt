package com.example.nexustime

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.data.local.ScreenOnMode
import com.example.nexustime.core.service.chime.ChimeScheduler
import com.example.nexustime.core.service.receivers.PowerConnectionReceiver
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusTimeTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Actividad única de NexusTime.
 *
 * La orientación horizontal se fija en el manifiesto
 * (`android:screenOrientation="sensorLandscape"`), no aquí: así el sistema ya
 * infla la ventana en landscape y se evita el parpadeo de rotación al arrancar.
 *
 * Aquí también se decide si mantener la pantalla encendida
 * (`FLAG_KEEP_SCREEN_ON`), según la preferencia [ScreenOnMode]: siempre, o solo
 * mientras el dispositivo está enchufado.
 */
class MainActivity : ComponentActivity() {

    /*
     * El receiver es un campo para que exista UNA sola instancia mientras viva
     * la actividad: registrarlo como local de onCreate/desregistrarlo con otra
     * referencia es una de las formas clásicas de acabar con
     * "Receiver not registered".
     */
    private val powerAlertsReceiver = PowerConnectionReceiver()

    /** Guarda contra doble registro / desregistro no emparejado. */
    private var receiverRegistered = false

    /** `true` solo entre onStart y onStop: es lo que decide si registramos. */
    private var isStarted = false

    /** Copia en memoria de la preferencia, mientras la actividad esté viva. */
    private var powerAlertsActive = false

    /** Modo de pantalla actual (copia en memoria de la preferencia). */
    private var screenOnMode = NexusSettings.DEFAULT_SCREEN_ON_MODE

    /** Estado de carga actual, que decide el flag en [ScreenOnMode.CHARGING_ONLY]. */
    private var isCharging = false

    /*
     * Receptor propio para seguir la corriente cuando el modo de pantalla la
     * necesita. Es INDEPENDIENTE del de alertas de voz: aquí solo queremos saber
     * si hay cargador, aunque los avisos de voz estén apagados.
     */
    private val chargingStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            isCharging = intent.isCharging()
            applyKeepScreenOn()
        }
    }

    /** Guarda contra doble registro del receiver de carga. */
    private var chargingReceiverRegistered = false

    /*
     * Launcher del permiso de calendario. Debe crearse como propiedad (y no
     * dentro de onCreate) para que el ActivityResultRegistry pueda restaurarlo
     * si el sistema mata la actividad mientras el diálogo está abierto.
     */
    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* La agenda relee sola en ON_RESUME; aquí no hay nada que hacer. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * La tarjeta "TU DÍA" lee el calendario del dispositivo. El permiso es en
         * tiempo de ejecución: se pide una vez al arrancar y la agenda degrada
         * con dignidad (mensaje propio) si el usuario lo deniega.
         */
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALENDAR,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }

        /*
         * El interruptor de Ajustes enciende/apaga el receptor. Se suscribe SOLO
         * en estado STARTED para no mantener una corrutina corriendo con la app
         * en segundo plano; cancela sola al irse a onStop.
         */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NexusSettings.powerAlertsEnabled.collect { enabled ->
                    powerAlertsActive = enabled
                    updatePowerAlertRegistration()
                }
            }
        }

        /*
         * El chime se (re)programa con AlarmManager. Se reacciona a los cambios de
         * preferencia mientras la app está visible (que es cuando se pueden tocar
         * los Ajustes); el Application ya lo programa al arrancar y el propio
         * ChimeReceiver se reprograma tras cada disparo.
         */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    NexusSettings.chimeEnabled,
                    NexusSettings.chimeInterval,
                ) { enabled, interval -> enabled to interval }
                    .collect { ChimeScheduler.scheduleNext(this@MainActivity) }
            }
        }

        /*
         * Modo de pantalla: al cambiar la preferencia se recalcula el flag. Se
         * aplica ya con el valor actual; el estado de carga llega al registrar el
         * receiver de batería (broadcast sticky), en onStart.
         */
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                NexusSettings.screenOnMode.collect { mode ->
                    screenOnMode = mode
                    applyKeepScreenOn()
                }
            }
        }

        setContent {
            NexusTimeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = NexusBlack,
                ) {
                    NexusTimeApp()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isStarted = true
        updatePowerAlertRegistration()
        registerChargingStateReceiver()
    }

    override fun onStop() {
        isStarted = false
        updatePowerAlertRegistration()
        unregisterChargingStateReceiver()
        super.onStop()
    }

    /**
     * Empareja registro/desregistro con el ciclo de vida.
     *
     * Registra solo si la actividad está visible Y el interruptor está activo.
     * Garantiza que `registerReceiver` y `unregisterReceiver` se compensan, que es
     * lo que evita la fuga de memoria (y el crash por desregistrar dos veces).
     */
    private fun updatePowerAlertRegistration() {
        val shouldRegister = isStarted && powerAlertsActive

        if (shouldRegister && !receiverRegistered) {
            /*
             * RECEIVER_NOT_EXPORTED porque estas cuatro acciones las envía el
             * system UID (Android exime al sistema de la marca de exportado),
             * de modo que ninguna otra app puede suplantar una alerta de carga.
             */
            ContextCompat.registerReceiver(
                this,
                powerAlertsReceiver,
                PowerConnectionReceiver.intentFilter(),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            receiverRegistered = true
        } else if (!shouldRegister && receiverRegistered) {
            unregisterReceiver(powerAlertsReceiver)
            receiverRegistered = false
        }
    }

    /**
     * Sigue el estado de carga mientras la actividad está visible.
     *
     * `ACTION_BATTERY_CHANGED` es *sticky*: al registrarse llega el valor actual,
     * así que no hace falta esperar a que cambie para saber si está enchufado.
     * Se registra siempre en onStart, con independencia de las alertas de voz.
     */
    private fun registerChargingStateReceiver() {
        if (chargingReceiverRegistered) return

        val sticky = ContextCompat.registerReceiver(
            this,
            chargingStateReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // El sticky trae el estado de carga actual; si es null, se asume sin cable.
        isCharging = sticky?.isCharging() ?: false
        chargingReceiverRegistered = true
        applyKeepScreenOn()
    }

    private fun unregisterChargingStateReceiver() {
        if (!chargingReceiverRegistered) return
        unregisterReceiver(chargingStateReceiver)
        chargingReceiverRegistered = false
    }

    /**
     * Aplica [WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON] según el modo.
     *
     * En [ScreenOnMode.CHARGING_ONLY] la pantalla se mantiene encendida solo con
     * el cargador puesto; de lo contrario se limpia el flag y el sistema apaga
     * la pantalla con normalidad.
     */
    private fun applyKeepScreenOn() {
        val keepOn = when (screenOnMode) {
            ScreenOnMode.ALWAYS -> true
            ScreenOnMode.CHARGING_ONLY -> isCharging
        }

        if (keepOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    /** `true` si el dispositivo se está cargando o ya está lleno. */
    private fun Intent.isCharging(): Boolean {
        val plugged = getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
        if (plugged) return true
        val status = getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }
}
