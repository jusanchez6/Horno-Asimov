# Interfaz App ↔ Firmware (contrato BLE)

> Documento rápido de referencia. Hoy la app (`App/`) corre en modo
> simulado (`SimulatedOvenConnection`) porque el firmware BLE aún no existe.
> Este documento es el contrato que el firmware debe implementar cuando se
> aborde la conectividad, para que la app pueda cambiar de
> `SimulatedOvenConnection` a una implementación BLE real sin tocar la UI.

## 1. Transporte

- **BLE GATT**. El horno actúa como **periférico / GATT server**
  (advertising, un solo servicio custom). La app actúa como
  **central / GATT client**.
- Board de referencia actual del firmware: XIAO ESP32S3 (BLE nativo vía
  Zephyr Bluetooth stack).
- UUIDs (placeholder, definir/generar definitivos al implementar el
  servidor GATT en firmware):
  - Servicio: `12345678-0001-1000-8000-00805f9b34fb`
  - Característica **Estado** (notify): `12345678-0002-1000-8000-00805f9b34fb`
  - Característica **Comando** (write): `12345678-0003-1000-8000-00805f9b34fb`
  - Característica **Salud** (notify): `12345678-0004-1000-8000-00805f9b34fb`

## 2. Qué recibe la app (uplink): paquete de Estado

El firmware publica este paquete por notify en la característica **Estado**,
aproximadamente 1 vez por segundo mientras hay un trabajo activo (o al
conectar, en reposo). 12 bytes, little-endian.

| Offset | Bytes | Campo            | Tipo   | Notas |
|--------|-------|-------------------|--------|-------|
| 0      | 1     | `protocol_version`| uint8  | `1` |
| 1      | 1     | `state`           | uint8  | enum, ver tabla abajo |
| 2-3    | 2     | `current_temp_c_x10` | int16 | temperatura real × 10 |
| 4-5    | 2     | `target_temp_c_x10`  | int16 | setpoint actual × 10 |
| 6-7    | 2     | `elapsed_s`       | uint16 | segundos desde inicio del trabajo |
| 8-9    | 2     | `remaining_s`     | uint16 | segundos estimados restantes |
| 10     | 1     | `profile_id`      | uint8  | perfil activo (ver tabla de perfiles) |
| 11     | 1     | `fault_code`      | uint8  | `0` = sin falla |

### Valores de `state`

| Código | Estado    | Corresponde a `OvenState` en la app |
|--------|-----------|--------------------------------------|
| 0      | IDLE      | `OvenState.IDLE` |
| 1      | PREHEAT   | `OvenState.PREHEAT` |
| 2      | SOAK      | `OvenState.SOAK` |
| 3      | REFLOW    | `OvenState.REFLOW` |
| 4      | COOLING   | `OvenState.COOLING` |
| 5      | DONE      | `OvenState.DONE` |
| 6      | FAULT     | `OvenState.FAULT` |

Estos códigos ya están implementados en
`App/app/src/main/java/com/hornoreflow/app/data/OvenModels.kt`
(`enum class OvenState`).

## 3. Qué recibe la app (uplink): paquete de Salud (Health)

Además del paquete de Estado (que solo importa durante un trabajo), el
firmware debe publicar este paquete de **salud del sistema** por notify en
la característica **Salud**, todo el tiempo mientras esté conectado
(aprox. cada 2 segundos, independiente de si hay un trabajo corriendo).
Sirve para que la app pueda mostrar si el ESP32 está en buen estado antes
de lanzar un trabajo (memoria, temperatura del chip, sensor OK, etc.), no
solo si el horno está soldando. 14 bytes, little-endian.

| Offset | Bytes | Campo              | Tipo   | Notas |
|--------|-------|---------------------|--------|-------|
| 0      | 1     | `protocol_version`  | uint8  | `1` |
| 1-4    | 4     | `uptime_s`          | uint32 | segundos desde el último arranque |
| 5-6    | 2     | `free_heap_kb`      | uint16 | memoria RAM libre |
| 7-8    | 2     | `cpu_temp_c_x10`    | int16  | temperatura interna del chip ESP32-S3 × 10 |
| 9      | 1     | `reset_reason`      | uint8  | enum, ver tabla abajo |
| 10     | 1     | `sensor_ok`         | uint8  | `1` = termocupla/MAX6675 OK, `0` = falla o desconectada |
| 11-13  | 3     | `fw_version`        | uint8×3 | major, minor, patch |

### Valores de `reset_reason`

| Código | Razón     | Corresponde a `ResetReason` en la app |
|--------|-----------|-----------------------------------------|
| 0      | POWER_ON  | `ResetReason.POWER_ON` |
| 1      | WATCHDOG  | `ResetReason.WATCHDOG` |
| 2      | PANIC     | `ResetReason.PANIC` |
| 3      | SOFTWARE  | `ResetReason.SOFTWARE` |
| 4      | BROWNOUT  | `ResetReason.BROWNOUT` |
| 5      | UNKNOWN   | `ResetReason.UNKNOWN` |

Estos códigos ya están implementados en
`App/app/src/main/java/com/hornoreflow/app/data/OvenModels.kt`
(`enum class ResetReason`, `data class EspHealth`) y la app ya tiene una
pantalla ("Salud ESP32", accesible desde el Panel de control) que los
muestra, hoy alimentada por `SimulatedOvenConnection`.

El RSSI del enlace BLE no viaja en este paquete: la app lo puede leer
directamente del sistema operativo Android (`BluetoothGatt.readRemoteRssi`)
una vez conectada, sin que el firmware tenga que reportarlo.

## 4. Qué recibe el firmware (downlink): paquete de Comando

La app escribe en la característica **Comando**. Primer byte = opcode.

| Opcode | Nombre     | Payload adicional | Efecto esperado en firmware |
|--------|------------|--------------------|------------------------------|
| `1`    | START_JOB  | 1 byte: `profile_id` | inicia el perfil indicado desde IDLE |
| `2`    | CANCEL_JOB | ninguno | corta calentamiento, vuelve a IDLE |
| `3`    | PING       | ninguno | keepalive, firmware responde con notify de Estado inmediato |

## 5. Perfiles predefinidos (deben coincidir entre app y firmware)

| `profile_id` | Nombre                     | Definido en la app |
|---------------|-----------------------------|---------------------|
| `1`           | Con plomo (Sn63/Pb37)      | `Profiles.LEADED` |
| `2`           | Sin plomo (SAC305)         | `Profiles.LEAD_FREE` |

Los puntos de la curva de cada perfil (fase, tiempo, temperatura) están
hoy solo en la app (`Profiles.kt`) porque el firmware todavía no ejecuta
ningún control térmico real. Cuando se implemente el firmware, estas
curvas deben replicarse ahí como las curvas de fábrica (RF-06), usando los
mismos `profile_id`.

## 6. Pendiente (no cubierto todavía, futuras iteraciones)

- Característica para **subir perfiles personalizados** desde la app
  (RF-07): formato de lista de puntos (tiempo, temperatura) aún sin
  definir.
- Emparejamiento/seguridad BLE (autenticación del comando START_JOB para
  que no cualquier dispositivo cercano pueda operar el horno).
- Notificación de eventos discretos (ej. "puerta abierta", "fin de
  trabajo") además del polling periódico del paquete de Estado.
- OTA de firmware (RF-14).

## 7. Estado actual de la implementación

- **Firmware**: BLE real funcionando en hardware (XIAO ESP32S3). Advertising
  conectable con reconexión automática (reanuda advertising al
  desconectarse), servicio GATT con las 3 características del protocolo
  (Estado, Comando, Salud) implementado y probado. El paquete de Estado usa
  el formato real de 12 bytes; los valores de temperatura/tiempo hoy vienen
  de una **simulación simple** dentro del firmware (rampa fija hasta 150°C
  al recibir START_JOB), no de la termocupla real — todavía no está
  integrado el MAX6675 por SPI ni el control de las resistencias (SSR). El
  paquete de Salud también es real en formato pero con valores de ejemplo
  (`sensor_ok = 0` porque no hay sensor real conectado todavía). No hay
  1-Wire/DS18B20 en el firmware actual (se sacó al enfocar en BLE).
- **App**: se conecta de verdad por BLE (`BleOvenConnection`, ya no
  simulada por default), descubre el servicio, se suscribe a Estado/Salud,
  parsea los paquetes reales, y puede escribir Comando (iniciar/cancelar
  trabajo). `SimulatedOvenConnection` se mantiene en el repo como
  implementación alternativa de `OvenConnection` (ej. para desarrollar la
  UI sin hardware a mano), pero no es la que usa `MainActivity` hoy.
- **Circuito de punta a punta verificado en hardware real**: la app
  encuentra el horno, conecta, inicia un trabajo desde la UI, ve el estado
  y la temperatura simulada actualizarse en tiempo real, y vuelve sola a la
  pantalla de conexión si el horno se desconecta.
- **Pendiente** (más allá de lo listado en la sección 6): control térmico
  real (MAX6675 + SSR, máquina de estados completa con las fases
  preheat/soak/reflow/cooling y las curvas de `Profiles.kt` replicadas en
  firmware), que es la parte de RF-01 a RF-06 todavía sin tocar.
