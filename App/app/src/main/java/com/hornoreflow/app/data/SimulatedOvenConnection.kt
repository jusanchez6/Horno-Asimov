package com.hornoreflow.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

class SimulatedOvenConnection : OvenConnection {

    private val scope = CoroutineScope(SupervisorJob())
    private var jobRunner: Job? = null
    private var healthRunner: Job? = null

    override val availableProfiles: List<SolderProfile> = Profiles.ALL

    private val _status = MutableStateFlow(
        OvenStatus(
            connected = false,
            state = OvenState.IDLE,
            currentTempC = 25f,
            targetTempC = 25f,
            elapsedSec = 0,
            remainingSec = 0,
            profileId = 0,
            faultCode = 0
        )
    )
    override val status: StateFlow<OvenStatus> = _status.asStateFlow()

    private val _health = MutableStateFlow(
        EspHealth(
            connected = false,
            uptimeSec = 0,
            freeHeapKb = 0,
            cpuTempC = 0f,
            resetReason = ResetReason.UNKNOWN,
            sensorOk = false,
            fwVersion = ""
        )
    )
    override val health: StateFlow<EspHealth> = _health.asStateFlow()

    private val _history = MutableStateFlow<List<CompletedJob>>(emptyList())
    override val history: StateFlow<List<CompletedJob>> = _history.asStateFlow()

    override suspend fun connect(): Boolean {
        delay(800)
        _status.update { it.copy(connected = true) }
        _health.update {
            it.copy(
                connected = true,
                uptimeSec = Random.nextInt(30, 600).toLong(),
                freeHeapKb = 195,
                cpuTempC = 39f,
                resetReason = ResetReason.POWER_ON,
                sensorOk = true,
                fwVersion = "0.0.0-sim"
            )
        }
        healthRunner?.cancel()
        healthRunner = scope.launch {
            while (true) {
                delay(2000)
                _health.update {
                    it.copy(
                        uptimeSec = it.uptimeSec + 2,
                        freeHeapKb = (it.freeHeapKb + Random.nextInt(-2, 3)).coerceIn(150, 220),
                        cpuTempC = (it.cpuTempC + (Random.nextFloat() * 1.0f - 0.5f)).coerceIn(34f, 46f)
                    )
                }
            }
        }
        return true
    }

    override fun disconnect() {
        jobRunner?.cancel()
        healthRunner?.cancel()
        _status.update { it.copy(connected = false) }
        _health.update { it.copy(connected = false) }
    }

    override fun startJob(profileId: Int) {
        val profile = availableProfiles.firstOrNull { it.id == profileId } ?: return
        jobRunner?.cancel()
        jobRunner = scope.launch {
            val startedAt = System.currentTimeMillis()
            var t = 0
            var result = JobResult.CANCELLED
            try {
                while (true) {
                    val segment = profile.segments.firstOrNull { t >= it.startSec && t < it.endSec }
                    if (segment == null) {
                        result = JobResult.COMPLETED
                        break
                    }
                    val span = (segment.endSec - segment.startSec).coerceAtLeast(1)
                    val frac = (t - segment.startSec).toFloat() / span
                    val target = segment.startTempC + (segment.endTempC - segment.startTempC) * frac
                    val noise = Random.nextFloat() * 1.6f - 0.8f
                    _status.update {
                        it.copy(
                            state = segment.phase,
                            currentTempC = target + noise,
                            targetTempC = target,
                            elapsedSec = t,
                            remainingSec = (profile.totalDurationSec - t).coerceAtLeast(0),
                            profileId = profile.id,
                            faultCode = 0
                        )
                    }
                    delay(1000)
                    t++
                }
                _status.update {
                    it.copy(
                        state = OvenState.DONE,
                        currentTempC = 50f,
                        targetTempC = 50f,
                        elapsedSec = profile.totalDurationSec,
                        remainingSec = 0
                    )
                }
            } finally {
                _history.update { it + CompletedJob(profile.name, startedAt, t, result) }
            }
        }
    }

    override fun cancelJob() {
        jobRunner?.cancel()
        jobRunner = null
        _status.update { it.copy(state = OvenState.IDLE, targetTempC = 25f) }
    }
}
