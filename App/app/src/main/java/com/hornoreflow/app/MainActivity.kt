package com.hornoreflow.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hornoreflow.app.data.BleOvenConnection
import com.hornoreflow.app.ui.ConnectScreen
import com.hornoreflow.app.ui.DashboardScreen
import com.hornoreflow.app.ui.HealthScreen
import com.hornoreflow.app.ui.HistoryScreen
import com.hornoreflow.app.ui.OvenViewModel
import com.hornoreflow.app.ui.StartJobScreen
import com.hornoreflow.app.ui.theme.AsimovTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AsimovTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HornoApp()
                }
            }
        }
    }
}

@Composable
fun HornoApp() {
    val navController = rememberNavController()
    val appContext = LocalContext.current.applicationContext
    val viewModel: OvenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OvenViewModel(BleOvenConnection(appContext)) as T
            }
        }
    )

    val status by viewModel.status.collectAsStateWithLifecycle()
    val health by viewModel.health.collectAsStateWithLifecycle()
    val connecting by viewModel.connecting.collectAsStateWithLifecycle()
    val tempHistory by viewModel.tempHistory.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()

    // ConnectScreen se saca del back stack al conectar (ver popUpTo abajo),
    // asi que si el horno se desconecta (se apago, se fue de rango, etc.)
    // no hay forma de volver a esa pantalla via navegacion normal - hay que
    // forzarlo apenas status.connected pasa a false.
    LaunchedEffect(status.connected) {
        if (!status.connected && navController.currentDestination?.route != "connect") {
            navController.navigate("connect") {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = "connect") {
        composable("connect") {
            ConnectScreen(
                connecting = connecting,
                onConnectClick = {
                    viewModel.connect {
                        navController.navigate("dashboard") {
                            popUpTo("connect") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("dashboard") {
            DashboardScreen(
                status = status,
                tempHistory = tempHistory,
                onStartJob = { navController.navigate("start_job") },
                onCancelJob = { viewModel.cancelJob() },
                onOpenHistory = { navController.navigate("history") },
                onOpenHealth = { navController.navigate("health") }
            )
        }
        composable("start_job") {
            StartJobScreen(
                profiles = viewModel.availableProfiles,
                onProfileSelected = { profileId ->
                    viewModel.startJob(profileId)
                    navController.popBackStack("dashboard", inclusive = false)
                }
            )
        }
        composable("history") {
            HistoryScreen(jobs = history)
        }
        composable("health") {
            HealthScreen(health = health)
        }
    }
}
