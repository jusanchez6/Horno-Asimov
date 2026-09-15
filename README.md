# Horno Asimov

Un horno tostador comercial convertido en horno de reflow para soldar placas
SMD/THT — control térmico en un ESP32-S3 corriendo Zephyr RTOS, controlado
y monitoreado en tiempo real desde una app Android por Bluetooth Low
Energy.

<p align="left">
  <img alt="Zephyr RTOS" src="https://img.shields.io/badge/Zephyr_RTOS-firmware-7B3FF2?style=for-the-badge&logo=zephyrproject&logoColor=white">
  <img alt="ESP32-S3" src="https://img.shields.io/badge/ESP32--S3-Espressif-E7352C?style=for-the-badge&logo=espressif&logoColor=white">
  <img alt="Bluetooth LE" src="https://img.shields.io/badge/Bluetooth_LE-GATT-0082FC?style=for-the-badge&logo=bluetooth&logoColor=white">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-Android-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/license-MIT-3DA639?style=for-the-badge">
</p>

---

## Supported targets

| Board | Estado |
|---|---|
| **Seeed XIAO ESP32s3 - ESP32s3** | Soportado — target de referencia, probado en hardware real |
| Cualquier board con BLE soportada por Zephyr | Debería andar con solo agregar `boards/<board>.overlay` — la lógica de aplicación (`Firmware/lib/`) no usa APIs específicas del fabricante, solo APIs estándar de Zephyr (ver [RNF-07](docs/01-REQUERIMIENTOS/requerimientos.md)) |

## Estructura del repo

```
.
├── Firmware/           Firmware Zephyr RTOS (C)
│   ├── src/main.c        orquestador: solo init + loop principal
│   ├── lib/oven/          maquina de estados + control termico (hoy simulado)
│   ├── lib/ble/           advertising, servicio GATT, protocolo
│   └── boards/            overlays de devicetree por placa
├── App/                 App Android (Kotlin + Jetpack Compose)
│   └── app/src/main/java/com/hornoreflow/app/
│       ├── data/          modelos, OvenConnection (BLE real + simulada)
│       └── ui/            pantallas Compose
└── docs/
    ├── 01-REQUERIMIENTOS/         requerimientos funcionales/no funcionales
    ├── 02-COMPRAS/                lista de materiales
    └── 03-INTERFAZ-APP-FIRMWARE/  contrato BLE app ↔ firmware
```

## Requisitos

- **Zephyr workspace** (`west`) instalado siguiendo la [guía oficial de
  Zephyr](https://docs.zephyrproject.org/latest/develop/getting_started/index.html)
  — este repo no trae su propio `west.yml`; `Firmware/` es una app
  freestanding que se compila contra un workspace Zephyr ya existente
  (`$ZEPHYR_BASE`).
- **Zephyr SDK** (toolchain Xtensa para ESP32), instalado vía `west sdk
  install`.
- **Android Studio** (Koala o más nuevo) o el SDK de Android por línea de
  comandos (`compileSdk 34`, `minSdk 26`, `targetSdk 34`, Build Tools 35).
- **JDK 17+**.
- **`adb`** (Android Platform Tools) para instalar/debuggear en el
  dispositivo.
- Un ESP32-S3 (probado en Seeed XIAO ESP32S3) por USB.
- Opcional pero recomendado para debug: nRF Connect for Mobile (Nordic
  Semiconductor) — deja inspeccionar el servicio GATT sin depender de la app.

## Firmware (Zephyr)

```bash
cd Firmware
source <tu-zephyrproject>/.venv/bin/activate
export ZEPHYR_BASE=<tu-zephyrproject>/zephyr

# build
west build -p always -b xiao_esp32s3/esp32s3/procpu -- -DDTC_OVERLAY_FILE=boards/xiao_esp32s3.overlay

# flash (agregá --esp-device /dev/ttyACM0 si detecta el puerto mal)
west flash -d build

# logs en vivo por USB
west espressif monitor -p /dev/ttyACM0
```

## App Android

```bash
cd App
export ANDROID_HOME=<tu-android-sdk>

# build del APK debug
./gradlew :app:assembleDebug

# instalar en un dispositivo conectado por USB (con depuracion USB habilitada)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# logs en vivo de la conexion BLE
adb logcat -s BleOvenConnection:D
```

También se puede abrir `App/` directo en Android Studio y correr desde ahí.

## Protocolo BLE

El contrato completo entre la app y el firmware (UUIDs, formato de los
paquetes Estado/Comando/Salud) está documentado en
[`docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md`](docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md).

## Estado del proyecto

- Conectividad BLE de punta a punta: advertising, reconexión automática,
  servicio GATT (Estado/Comando/Salud), app real conectando, parseando e
  iniciando/cancelando trabajos.
- Control térmico real: hoy el firmware **simula** la curva de
  temperatura. Falta integrar la termocupla tipo K + MAX6675 (SPI) y el
  control de las resistencias vía SSR — ver
  [`docs/01-REQUERIMIENTOS/requerimientos.md`](docs/01-REQUERIMIENTOS/requerimientos.md)
  (RF-01 a RF-06).

## Licencia

[MIT](LICENSE)
