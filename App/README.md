# Horno Reflow — App Android

App Android (Kotlin + Jetpack Compose) para conectarse al horno de reflow y
monitorear/iniciar trabajos de soldadura. Se conecta por BLE real
(`app/src/main/java/com/hornoreflow/app/data/BleOvenConnection.kt`) al
firmware (`Firmware/`), siguiendo el contrato de
`docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md`. El firmware hoy simula el
comportamiento térmico (todavía no hay termocupla/SSR reales conectados),
pero la conexión, el descubrimiento del servicio GATT, y el intercambio de
datos son reales.

También existe `SimulatedOvenConnection.kt` (misma interfaz
`OvenConnection`) como alternativa para desarrollar/probar la UI sin
hardware a mano — no es la que usa `MainActivity` por defecto.

## Cómo abrir el proyecto

1. Abrir la carpeta `App/` con Android Studio (Koala o más nuevo).
2. Dejar que Android Studio sincronice Gradle — puede ofrecer actualizar
   AGP/Gradle a las últimas versiones estables, se puede aceptar sin
   problema porque no hay dependencias específicas de una versión exacta.
3. Ejecutar en un emulador o dispositivo (minSdk 26).

Probada compilando con `./gradlew :app:assembleDebug` e instalando con
`adb install` en una tablet Android real (Samsung Tab S7 FE), conectada de
verdad al ESP32 por BLE.

## Flujo actual

1. **Conectar** (`ConnectScreen`): pide permisos de Bluetooth en runtime si
   hace falta, botón "Buscar y conectar horno" — escanea BLE de verdad,
   conecta por GATT al horno, y pasa a Dashboard.
2. **Dashboard** (`DashboardScreen`): estado actual, temperatura
   actual/objetivo, curva en tiempo real (`TempCurveChart`, Canvas simple
   sin librerías externas), tiempo transcurrido/restante, botón para
   iniciar o cancelar un trabajo — todo alimentado por las notificaciones
   BLE reales de la característica Estado.
3. **Iniciar trabajo** (`StartJobScreen`): lista de curvas predefinidas
   (con plomo / sin plomo); al elegir una, escribe el comando START_JOB
   real por BLE.
4. **Historial** (`HistoryScreen`): todavía no se llena con trabajos reales
   (pendiente registrar el historial de trabajos vía BLE/almacenamiento
   local).

Si el horno se desconecta (se apaga, se va de rango), la app vuelve sola a
la pantalla de Connect.

## Qué falta para el control térmico real

La conexión BLE y el protocolo ya están completos de punta a punta. Lo que
falta es del lado del firmware: reemplazar la simulación de temperatura por
la lectura real del MAX6675 (SPI) y el control de los SSR, siguiendo
`docs/01-REQUERIMIENTOS/requerimientos.md` (RF-01 a RF-06). La app no
debería necesitar cambios para eso — ya consume el protocolo real.
