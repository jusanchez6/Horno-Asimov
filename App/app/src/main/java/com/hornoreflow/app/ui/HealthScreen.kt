package com.hornoreflow.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hornoreflow.app.data.EspHealth
import com.hornoreflow.app.data.ResetReason

private fun ResetReason.label(): String = when (this) {
    ResetReason.POWER_ON -> "Encendido normal"
    ResetReason.WATCHDOG -> "Reinicio por watchdog"
    ResetReason.PANIC -> "Panic / excepcion"
    ResetReason.SOFTWARE -> "Reinicio por software"
    ResetReason.BROWNOUT -> "Caida de voltaje (brownout)"
    ResetReason.UNKNOWN -> "Desconocido"
}

private fun formatUptime(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
private fun HealthRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun HealthScreen(health: EspHealth) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        AsimovTopBar(title = "Salud ESP32")
        Spacer(Modifier.height(24.dp))

        val statusColor = if (health.connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        HealthRow("Conexion", if (health.connected) "EN LINEA" else "SIN CONEXION", statusColor)
        HealthRow("Uptime", formatUptime(health.uptimeSec))
        HealthRow("Heap libre", "${health.freeHeapKb} KB")
        HealthRow("Temp. CPU", "%.1f°C".format(health.cpuTempC))
        HealthRow("Ultimo reinicio", health.resetReason.label())
        HealthRow(
            "Sensor (MAX6675)",
            if (health.sensorOk) "OK" else "FALLA",
            if (health.sensorOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        HealthRow("Firmware", health.fwVersion.ifBlank { "-" })
    }
}
