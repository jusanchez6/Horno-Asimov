package com.hornoreflow.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hornoreflow.app.data.OvenState
import com.hornoreflow.app.data.OvenStatus

private fun OvenState.label(): String = when (this) {
    OvenState.IDLE -> "Inactivo"
    OvenState.PREHEAT -> "Precalentamiento"
    OvenState.SOAK -> "Soak"
    OvenState.REFLOW -> "Reflow"
    OvenState.COOLING -> "Enfriamiento"
    OvenState.DONE -> "Completado"
    OvenState.FAULT -> "Falla"
}

private fun formatTime(totalSec: Int): String {
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}

@Composable
fun DashboardScreen(
    status: OvenStatus,
    tempHistory: List<Pair<Int, Float>>,
    onStartJob: () -> Unit,
    onCancelJob: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenHealth: () -> Unit
) {
    val running = status.state !in listOf(OvenState.IDLE, OvenState.DONE, OvenState.FAULT)

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        AsimovTopBar(
            title = "Panel de control",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = onOpenHealth, label = { Text("SALUD") })
                    AssistChip(onClick = onOpenHistory, label = { Text("HISTORIAL") })
                }
            }
        )

        Spacer(Modifier.height(24.dp))

        Column {
            Text("Estado", style = MaterialTheme.typography.labelLarge)
            Text(status.state.label(), style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Actual", style = MaterialTheme.typography.labelMedium)
                Text("%.1f°C".format(status.currentTempC), style = MaterialTheme.typography.displaySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Objetivo", style = MaterialTheme.typography.labelMedium)
                Text("%.1f°C".format(status.targetTempC), style = MaterialTheme.typography.displaySmall)
            }
        }

        Spacer(Modifier.height(20.dp))

        TempCurveChart(
            points = tempHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Transcurrido: ${formatTime(status.elapsedSec)}")
            Text("Restante: ${formatTime(status.remainingSec)}")
        }

        Spacer(Modifier.height(28.dp))

        if (running) {
            Button(onClick = onCancelJob, modifier = Modifier.fillMaxWidth()) {
                Text("CANCELAR TRABAJO")
            }
        } else {
            Button(onClick = onStartJob, modifier = Modifier.fillMaxWidth()) {
                Text("INICIAR NUEVO TRABAJO")
            }
        }
    }
}
