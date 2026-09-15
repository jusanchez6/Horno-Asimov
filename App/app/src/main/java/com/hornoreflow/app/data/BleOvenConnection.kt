package com.hornoreflow.app.data

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Implementacion BLE real de OvenConnection, siguiendo el contrato de
 * docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md. El llamador debe haber pedido
 * (y obtenido) los permisos BLUETOOTH_SCAN / BLUETOOTH_CONNECT en runtime
 * ANTES de invocar connect() - esta clase no los pide (ver ConnectScreen).
 */
class BleOvenConnection(private val context: Context) : OvenConnection {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private var gatt: BluetoothGatt? = null
    private var comandoCharacteristic: BluetoothGattCharacteristic? = null

    override val availableProfiles: List<SolderProfile> = Profiles.ALL

    private val _status = MutableStateFlow(
        OvenStatus(
            connected = false,
            state = OvenState.IDLE,
            currentTempC = 0f,
            targetTempC = 0f,
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
        val connected = withTimeoutOrNull(SCAN_TIMEOUT_MS) { scanAndConnect() }
        return connected ?: false
    }

    @Suppress("DEPRECATION")
    private suspend fun scanAndConnect(): Boolean = suspendCancellableCoroutine { cont ->
        val adapter = bluetoothManager.adapter
        val scanner = adapter?.takeIf { it.isEnabled }?.bluetoothLeScanner

        if (scanner == null) {
            Log.e(TAG, "Bluetooth apagado o adaptador BLE no disponible")
            cont.resume(false)
            return@suspendCancellableCoroutine
        }

        var finished = false
        fun finish(result: Boolean) {
            if (!finished) {
                finished = true
                cont.resume(result)
            }
        }

        lateinit var scanCallback: ScanCallback

        // Cuando termina de escribirse el descriptor CCC de una caracteristica
        // (activar notify), seguimos con la siguiente suscripcion pendiente.
        // Android solo permite UNA operacion GATT en vuelo por vez.
        var pendingAfterDescriptorWrite: (() -> Unit)? = null

        fun subscribeTo(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, then: (() -> Unit)?) {
            g.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CCC_DESCRIPTOR_UUID)
            if (descriptor == null) {
                Log.e(TAG, "Caracteristica ${characteristic.uuid} sin descriptor CCC")
                then?.invoke()
                return
            }
            pendingAfterDescriptorWrite = then
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            g.writeDescriptor(descriptor)
        }

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "GATT conectado a ${g.device.address}, descubriendo servicios...")
                        gatt = g
                        g.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d(TAG, "GATT desconectado (status=$status)")
                        g.close()
                        if (gatt === g) gatt = null
                        comandoCharacteristic = null
                        _status.update { it.copy(connected = false) }
                        _health.update { it.copy(connected = false) }
                        finish(false)
                    }
                }
            }

            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "discoverServices fallo, status=$status")
                    finish(false)
                    return
                }

                val service = g.getService(SERVICE_UUID)
                val estadoChar = service?.getCharacteristic(ESTADO_UUID)
                val comandoChar = service?.getCharacteristic(COMANDO_UUID)
                val saludChar = service?.getCharacteristic(SALUD_UUID)

                if (service == null || estadoChar == null || comandoChar == null || saludChar == null) {
                    Log.e(TAG, "No se encontro el servicio/caracteristicas del horno")
                    finish(false)
                    return
                }

                comandoCharacteristic = comandoChar

                Log.d(TAG, "Servicio encontrado, suscribiendo a Estado y Salud")
                subscribeTo(g, estadoChar) {
                    subscribeTo(g, saludChar, null)
                }

                _status.update { it.copy(connected = true) }
                finish(true)
            }

            override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                Log.d(TAG, "CCC escrito para ${descriptor.characteristic.uuid}, status=$status")
                val next = pendingAfterDescriptorWrite
                pendingAfterDescriptorWrite = null
                next?.invoke()
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val bytes = characteristic.value ?: return
                when (characteristic.uuid) {
                    ESTADO_UUID -> parseEstado(bytes)?.let { _status.value = it }
                    SALUD_UUID -> parseSalud(bytes)?.let { _health.value = it }
                }
            }
        }

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // El nombre anunciado por BLE viene en el scan record, no en
                // result.device.name (eso es el nombre cacheado por Android
                // de un pairing/conexion previa, que para un dispositivo
                // nuevo como el horno va a ser null).
                val name = result.scanRecord?.deviceName ?: result.device.name
                Log.d(TAG, "Dispositivo BLE encontrado: $name (${result.device.address})")
                if (name == TARGET_DEVICE_NAME) {
                    scanner.stopScan(this)
                    result.device.connectGatt(context, false, gattCallback)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "Scan BLE fallo, codigo $errorCode")
                finish(false)
            }
        }

        try {
            Log.d(TAG, "Empezando scan BLE, buscando \"$TARGET_DEVICE_NAME\"...")
            scanner.startScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permisos BLUETOOTH_SCAN/CONNECT en runtime", e)
            finish(false)
        }

        cont.invokeOnCancellation {
            try {
                scanner.stopScan(scanCallback)
            } catch (_: SecurityException) {
                // permisos revocados mientras cancelabamos; nada mas para hacer
            }
        }
    }

    /** Estado: 12 bytes little-endian, ver protocolo.md seccion 2. */
    private fun parseEstado(bytes: ByteArray): OvenStatus? {
        if (bytes.size < 12) {
            Log.w(TAG, "Paquete de Estado con tamano inesperado: ${bytes.size}")
            return null
        }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.get() // protocol_version, sin usar por ahora
        val stateCode = buf.get().toInt() and 0xFF
        val currentTempX10 = buf.short.toInt()
        val targetTempX10 = buf.short.toInt()
        val elapsedS = buf.short.toInt() and 0xFFFF
        val remainingS = buf.short.toInt() and 0xFFFF
        val profileId = buf.get().toInt() and 0xFF
        val faultCode = buf.get().toInt() and 0xFF

        return OvenStatus(
            connected = true,
            state = OvenState.fromCode(stateCode),
            currentTempC = currentTempX10 / 10f,
            targetTempC = targetTempX10 / 10f,
            elapsedSec = elapsedS,
            remainingSec = remainingS,
            profileId = profileId,
            faultCode = faultCode
        )
    }

    /** Salud: 14 bytes little-endian, ver protocolo.md seccion 3. */
    private fun parseSalud(bytes: ByteArray): EspHealth? {
        if (bytes.size < 14) {
            Log.w(TAG, "Paquete de Salud con tamano inesperado: ${bytes.size}")
            return null
        }
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buf.get() // protocol_version, sin usar por ahora
        val uptimeS = buf.int.toLong() and 0xFFFFFFFFL
        val freeHeapKb = buf.short.toInt() and 0xFFFF
        val cpuTempX10 = buf.short.toInt()
        val resetReasonCode = buf.get().toInt() and 0xFF
        val sensorOk = buf.get().toInt() != 0
        val fwMajor = buf.get().toInt() and 0xFF
        val fwMinor = buf.get().toInt() and 0xFF
        val fwPatch = buf.get().toInt() and 0xFF

        return EspHealth(
            connected = true,
            uptimeSec = uptimeS,
            freeHeapKb = freeHeapKb,
            cpuTempC = cpuTempX10 / 10f,
            resetReason = ResetReason.fromCode(resetReasonCode),
            sensorOk = sensorOk,
            fwVersion = "$fwMajor.$fwMinor.$fwPatch"
        )
    }

    override fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        comandoCharacteristic = null
        _status.update { it.copy(connected = false) }
        _health.update { it.copy(connected = false) }
    }

    @Suppress("DEPRECATION")
    private fun writeComando(bytes: ByteArray) {
        val g = gatt
        val characteristic = comandoCharacteristic
        if (g == null || characteristic == null) {
            Log.w(TAG, "writeComando: sin conexion/caracteristica todavia")
            return
        }
        characteristic.value = bytes
        try {
            g.writeCharacteristic(characteristic)
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso BLUETOOTH_CONNECT para escribir", e)
        }
    }

    override fun startJob(profileId: Int) {
        writeComando(byteArrayOf(CMD_START_JOB, profileId.toByte()))
    }

    override fun cancelJob() {
        writeComando(byteArrayOf(CMD_CANCEL_JOB))
    }

    companion object {
        private const val TAG = "BleOvenConnection"

        /** Debe coincidir con CONFIG_BT_DEVICE_NAME en Firmware/prj.conf */
        private const val TARGET_DEVICE_NAME = "Test horno"

        private const val SCAN_TIMEOUT_MS = 10_000L

        private val SERVICE_UUID: UUID = UUID.fromString("12345678-0001-1000-8000-00805f9b34fb")
        private val ESTADO_UUID: UUID = UUID.fromString("12345678-0002-1000-8000-00805f9b34fb")
        private val COMANDO_UUID: UUID = UUID.fromString("12345678-0003-1000-8000-00805f9b34fb")
        private val SALUD_UUID: UUID = UUID.fromString("12345678-0004-1000-8000-00805f9b34fb")
        private val CCC_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        private const val CMD_START_JOB: Byte = 1
        private const val CMD_CANCEL_JOB: Byte = 2
    }
}
