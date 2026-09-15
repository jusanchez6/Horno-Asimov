package com.hornoreflow.app.data

/**
 * Los valores de `code` son los mismos que usara el firmware en el byte
 * "state" del paquete de estado BLE. Ver docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md.
 */
enum class OvenState(val code: Int) {
    IDLE(0), PREHEAT(1), SOAK(2), REFLOW(3), COOLING(4), DONE(5), FAULT(6);

    companion object {
        fun fromCode(code: Int): OvenState = values().firstOrNull { it.code == code } ?: FAULT
    }
}

data class OvenStatus(
    val connected: Boolean,
    val state: OvenState,
    val currentTempC: Float,
    val targetTempC: Float,
    val elapsedSec: Int,
    val remainingSec: Int,
    val profileId: Int,
    val faultCode: Int
)

data class ProfileSegment(
    val phase: OvenState,
    val startSec: Int,
    val endSec: Int,
    val startTempC: Float,
    val endTempC: Float
)

data class SolderProfile(
    val id: Int,
    val name: String,
    val segments: List<ProfileSegment>
) {
    val totalDurationSec: Int get() = segments.last().endSec
    val peakTempC: Float get() = segments.maxOf { maxOf(it.startTempC, it.endTempC) }
}

enum class JobResult { COMPLETED, CANCELLED, FAULT }

data class CompletedJob(
    val profileName: String,
    val startedAtEpochMs: Long,
    val durationSec: Int,
    val result: JobResult
)

/**
 * Los valores de `code` son los mismos que usara el firmware en el byte
 * "reset_reason" del paquete de Salud (Health) BLE. Ver
 * docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md.
 */
enum class ResetReason(val code: Int) {
    POWER_ON(0), WATCHDOG(1), PANIC(2), SOFTWARE(3), BROWNOUT(4), UNKNOWN(5);

    companion object {
        fun fromCode(code: Int): ResetReason = values().firstOrNull { it.code == code } ?: UNKNOWN
    }
}

data class EspHealth(
    val connected: Boolean,
    val uptimeSec: Long,
    val freeHeapKb: Int,
    val cpuTempC: Float,
    val resetReason: ResetReason,
    val sensorOk: Boolean,
    val fwVersion: String
)
