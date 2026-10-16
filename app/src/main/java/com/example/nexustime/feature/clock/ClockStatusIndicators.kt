package com.example.nexustime.feature.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nexustime.core.data.local.NexusSettings
import com.example.nexustime.core.ui.theme.NexusSkyBlue
import com.example.nexustime.core.ui.theme.NexusSilver

/** Lectura instantánea de la batería para los indicadores en miniatura. */
private data class BatteryReading(
    val percent: Int,
    val isCharging: Boolean,
    val isFull: Boolean,
)

/**
 * Lee el estado de la batería en tiempo real.
 *
 * Se registra como receiver local de ACTION_BATTERY_CHANGED, que el sistema
 * emite de forma sticky (también devuelve el último valor al registrar). No
 * requiere permiso porque es un broadcast del propio sistema hacia la app.
 */
@Composable
fun rememberBatteryReading(): BatteryReading {
    val context = LocalContext.current
    var reading by remember { mutableStateOf(BatteryReading(percent = 0, isCharging = false, isFull = false)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                reading = intent.toBatteryReading()
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { reading = it.toBatteryReading() }

        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    return reading
}

private fun Intent.toBatteryReading(): BatteryReading {
    val level = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    val status = getIntExtra(BatteryManager.EXTRA_STATUS, -1)

    val percent = if (level >= 0 && scale > 0) (level * 100f / scale).toInt() else 0
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val isFull = status == BatteryManager.BATTERY_STATUS_FULL

    return BatteryReading(percent = percent, isCharging = isCharging, isFull = isFull)
}

/**
 * Indicadores de estado en miniatura (zona central, bajo el reloj).
 *
 * Muestran el estado de carga/batería y si el anuncio horario está activo,
 * ambos en celeste (#80D8FF), el color oficial de "widgets y estado".
 */
@Composable
fun ClockStatusIndicators(modifier: Modifier = Modifier) {
    val battery = rememberBatteryReading()
    val chimeEnabled by NexusSettings.chimeEnabled.collectAsStateWithLifecycle()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusIndicator(
            icon = battery.icon(),
            label = "${battery.percent}%",
        )
        Spacer(modifier = Modifier.width(24.dp))
        StatusIndicator(
            icon = if (chimeEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
            label = if (chimeEnabled) "Chime activo" else "Chime apagado",
        )
    }
}

private fun BatteryReading.icon(): ImageVector = when {
    isCharging -> Icons.Outlined.BatteryChargingFull
    percent <= 10 -> Icons.Outlined.BatteryAlert
    isFull -> Icons.Outlined.BatteryFull
    else -> Icons.Outlined.BatteryStd
}

@Composable
private fun StatusIndicator(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NexusSkyBlue,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = NexusSilver,
        )
    }
}
