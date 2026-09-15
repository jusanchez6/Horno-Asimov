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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hornoreflow.app.data.SolderProfile

@Composable
fun StartJobScreen(
    profiles: List<SolderProfile>,
    onProfileSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        AsimovTopBar(title = "Nuevo trabajo")
        Spacer(Modifier.height(20.dp))
        Text("Selecciona una curva de soldadura", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(profiles) { profile ->
                OutlinedCard(
                    onClick = { onProfileSelected(profile.id) },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(profile.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Duracion: ${profile.totalDurationSec / 60} min " +
                                "${profile.totalDurationSec % 60} s · Pico: ${profile.peakTempC.toInt()}°C",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
