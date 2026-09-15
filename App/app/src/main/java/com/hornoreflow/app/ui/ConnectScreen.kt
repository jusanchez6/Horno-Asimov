package com.hornoreflow.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.hornoreflow.app.R

/** Permisos BLE que hacen falta en runtime segun la version de Android. */
private fun requiredBlePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

@Composable
fun ConnectScreen(
    connecting: Boolean,
    onConnectClick: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            onConnectClick()
        }
    }

    fun connectWithPermissions() {
        val missing = requiredBlePermissions().filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            onConnectClick()
        } else {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_asimov_logo),
            contentDescription = null,
            modifier = Modifier.width(96.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            "ASIMOV",
            style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 8.sp),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            "REFLOW CONTROL SYSTEM",
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 4.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
        Text(
            "Busca el horno por Bluetooth (BLE). Solo la conexion es real por ahora, los datos todavia no.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        if (connecting) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("CONECTANDO...", style = MaterialTheme.typography.labelMedium)
        } else {
            Button(onClick = { connectWithPermissions() }) {
                Text("BUSCAR Y CONECTAR HORNO")
            }
        }
    }
}
