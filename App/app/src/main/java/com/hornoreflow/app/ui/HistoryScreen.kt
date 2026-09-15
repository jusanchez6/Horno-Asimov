package com.hornoreflow.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hornoreflow.app.data.CompletedJob
import com.hornoreflow.app.data.JobResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun JobResult.label(): String = when (this) {
    JobResult.COMPLETED -> "Completado"
    JobResult.CANCELLED -> "Cancelado"
    JobResult.FAULT -> "Falla"
}

@Composable
fun HistoryScreen(jobs: List<CompletedJob>) {
    val formatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        AsimovTopBar(title = "Historial de trabajos")
        Spacer(Modifier.height(20.dp))
        if (jobs.isEmpty()) {
            Text("Todavia no hay trabajos registrados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(jobs.reversed()) { job ->
                    OutlinedCard(
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(job.profileName, style = MaterialTheme.typography.titleMedium)
                            Text(formatter.format(Date(job.startedAtEpochMs)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Duracion: ${job.durationSec}s · ${job.result.label()}")
                        }
                    }
                }
            }
        }
    }
}
