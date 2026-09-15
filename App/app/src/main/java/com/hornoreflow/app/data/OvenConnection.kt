package com.hornoreflow.app.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Contrato entre la UI de la app y el horno. Hoy la unica implementacion es
 * [SimulatedOvenConnection]; cuando exista el firmware, una implementacion
 * BLE real (GATT client) reemplazara esta clase sin tocar la UI, siguiendo
 * el mismo contrato documentado en docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md.
 */
interface OvenConnection {
    val status: StateFlow<OvenStatus>
    val health: StateFlow<EspHealth>
    val history: StateFlow<List<CompletedJob>>
    val availableProfiles: List<SolderProfile>

    suspend fun connect(): Boolean
    fun disconnect()
    fun startJob(profileId: Int)
    fun cancelJob()
}
