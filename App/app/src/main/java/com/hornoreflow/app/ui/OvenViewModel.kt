package com.hornoreflow.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hornoreflow.app.data.OvenConnection
import com.hornoreflow.app.data.OvenState
import com.hornoreflow.app.data.SimulatedOvenConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OvenViewModel @JvmOverloads constructor(
    private val connection: OvenConnection = SimulatedOvenConnection()
) : ViewModel() {

    val status = connection.status
    val health = connection.health
    val history = connection.history
    val availableProfiles = connection.availableProfiles

    private val _connecting = MutableStateFlow(false)
    val connecting: StateFlow<Boolean> = _connecting.asStateFlow()

    private val _tempHistory = MutableStateFlow<List<Pair<Int, Float>>>(emptyList())
    val tempHistory: StateFlow<List<Pair<Int, Float>>> = _tempHistory.asStateFlow()

    init {
        viewModelScope.launch {
            connection.status.collect { s ->
                _tempHistory.update { prev ->
                    if (s.elapsedSec == 0 && s.state == OvenState.PREHEAT) {
                        listOf(s.elapsedSec to s.currentTempC)
                    } else {
                        (prev + (s.elapsedSec to s.currentTempC)).takeLast(400)
                    }
                }
            }
        }
    }

    fun connect(onConnected: () -> Unit) {
        // Conectar (scan + GATT + discovery) puede tardar menos de 1s, asi
        // que un doble-tap del boton alcanza a disparar un segundo connect()
        // en paralelo antes de que la UI reaccione. Ese segundo intento
        // nunca encuentra el horno (ya esta conectado a la primera sesion) y
        // se queda escaneando 10s para nada. Se ignora si ya hay uno en curso.
        if (_connecting.value) return

        viewModelScope.launch {
            _connecting.value = true
            val ok = connection.connect()
            _connecting.value = false
            if (ok) onConnected()
        }
    }

    fun startJob(profileId: Int) = connection.startJob(profileId)

    fun cancelJob() = connection.cancelJob()
}
