package com.example.nexustime

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.nexustime.core.ui.theme.NexusBlack
import com.example.nexustime.core.ui.theme.NexusTimeTheme

/**
 * Actividad única de NexusTime.
 *
 * La orientación horizontal se fija en el manifiesto
 * (`android:screenOrientation="sensorLandscape"`), no aquí: así el sistema ya
 * infla la ventana en landscape y se evita el parpadeo de rotación al arrancar.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Un reloj de mesa no debe apagar la pantalla mientras se mira.
        // FLAG_KEEP_SCREEN_ON no requiere el permiso WAKE_LOCK.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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
}
