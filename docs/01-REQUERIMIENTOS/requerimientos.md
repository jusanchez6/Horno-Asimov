# Requerimientos del Horno de Reflow

## 1. Contexto

El proyecto consiste en transformar un horno tostador comercial en un horno de
reflow para soldadura de placas electrónicas (SMD/THT). El horno mide la
temperatura interna con una termocupla tipo K amplificada por un módulo
MAX6675 (SPI), y controla las resistencias del horno para seguir una curva de
temperatura (perfil de reflow) de forma automática.

El firmware se desarrolla sobre **Zephyr RTOS** para que la lógica de control
sea portable entre distintas placas/MCUs, y el sistema expone conectividad
para ser operado y monitoreado desde una **app móvil**.

## 2. Alcance

Incluye: control térmico en lazo cerrado, ejecución de perfiles de reflow,
seguridad térmica, conectividad inalámbrica, y la app móvil de control y
monitoreo.

No incluye (fuera de alcance de esta primera versión): control de atmósfera
(nitrógeno), visión artificial para inspección de soldadura, ni integración
con líneas de producción (SMEMA).

## 3. Requerimientos Funcionales (RF)

### 3.1 Sensado y control térmico

- **RF-01**: El sistema debe leer la temperatura de la cámara mediante el
  módulo MAX6675 sobre bus SPI, con un período de muestreo consistente con el
  tiempo de conversión del MAX6675 (~220 ms), es decir, al menos 1 lectura
  válida por segundo.
- **RF-02**: El sistema debe detectar y reportar fallas de la termocupla
  (circuito abierto / termocupla desconectada), condición que el MAX6675
  expone en el bit D2 de su salida.
- **RF-03**: El sistema debe controlar la potencia entregada a las
  resistencias (superior e inferior, si el horno lo permite) mediante
  actuación on/off o PWM de baja frecuencia sobre un relé de estado sólido
  (SSR), implementando un lazo de control (p. ej. PID o bang-bang con
  histéresis) que siga el setpoint de la curva activa.
- **RF-04**: El sistema debe soportar el control independiente de la
  resistencia superior e inferior cuando el hardware del horno lo permita, de
  modo que se pueda favorecer una u otra en distintas fases del perfil.

### 3.2 Perfiles de curva de soldadura

- **RF-05**: El sistema debe ejecutar perfiles de reflow compuestos como
  mínimo por las fases: precalentamiento (preheat), remojo/activación de
  flux (soak), reflow (por encima de la temperatura de liquidus) y
  enfriamiento (cooling), cada una con temperatura objetivo, rampa
  (°C/s) y duración.
- **RF-06**: El sistema debe incluir perfiles predefinidos de fábrica para al
  menos pasta con plomo (Sn63/Pb37) y sin plomo (SAC305).
- **RF-07**: El sistema debe permitir crear, editar, guardar y eliminar
  perfiles personalizados (puntos de la curva tiempo/temperatura) desde la
  app móvil.
- **RF-08**: El sistema debe registrar (log) la curva real de temperatura vs.
  tiempo durante cada trabajo, para poder compararla contra la curva objetivo
  después del proceso.

### 3.3 Máquina de estados / operación

- **RF-09**: El sistema debe implementar una máquina de estados explícita
  con, como mínimo: Inactivo (Idle), Precalentamiento, Soak, Reflow,
  Enfriamiento, Trabajo completado, y Falla/Error.
- **RF-10**: El sistema debe permitir iniciar, pausar/cancelar un trabajo de
  soldadura desde la app móvil o desde un control local (si existe) en
  cualquier momento del proceso.
- **RF-11**: El sistema debe notificar el fin de un trabajo y permitir un
  aviso sonoro/visual local además de la notificación remota vía app.

### 3.4 Firmware y portabilidad

- **RF-12**: El firmware debe estar escrito sobre Zephyr RTOS, separando la
  lógica de control térmico/perfil (independiente de placa) de los drivers y
  el devicetree específicos de cada board, de modo que portar el sistema a
  otra placa soportada por Zephyr no requiera reescribir la lógica de
  aplicación.
- **RF-13**: La configuración de pines, bus SPI del MAX6675 y salidas de
  control de los SSR debe definirse vía devicetree/overlays, no hardcodeada
  en el código de aplicación.
- **RF-14**: El sistema debe soportar actualización de firmware (OTA o vía
  cable/USB/DFU) sin requerir herramientas de programación especializadas
  por parte del usuario final.

### 3.5 Conectividad

- **RF-15**: El horno debe exponer conectividad inalámbrica (BLE y/o Wi-Fi)
  para ser descubierto y emparejado por la app móvil.
- **RF-16**: El sistema debe transmitir en tiempo (casi) real a la app: estado
  actual, temperatura medida, temperatura objetivo, fase del perfil activo, y
  tiempo transcurrido/restante del trabajo.

### 3.6 App móvil

- **RF-17**: La app debe permitir descubrir y conectarse a un horno
  disponible en la red/alcance BLE.
- **RF-18**: La app debe mostrar el estado actual del horno y, si hay un
  trabajo en curso, la curva de temperatura en tiempo real superpuesta a la
  curva objetivo.
- **RF-19**: La app debe permitir iniciar un nuevo trabajo, seleccionando un
  perfil de soldadura existente o personalizado.
- **RF-20**: La app debe mostrar el tiempo transcurrido y el tiempo estimado
  restante de cada fase y del trabajo completo.
- **RF-21**: La app debe permitir consultar el historial de trabajos
  anteriores (curva real registrada, perfil usado, fecha, resultado
  éxito/falla).
- **RF-22**: La app debe notificar al usuario (push/local) cuando el trabajo
  finaliza, cuando ocurre una falla, o cuando se requiere intervención
  (p. ej. abrir la puerta para enfriar).

## 4. Requerimientos No Funcionales (RNF)

### 4.1 Seguridad

- **RNF-01**: El sistema debe implementar un límite máximo de temperatura de
  software (corte de emergencia de las resistencias) independiente del lazo
  de control normal.
- **RNF-02**: El sistema debe protegerse ante pérdida de señal o lectura
  inválida de la termocupla, cortando la potencia a las resistencias como
  acción segura por defecto (fail-safe).
- **RNF-03**: Se recomienda un corte térmico/fusible térmico de respaldo por
  hardware, independiente del microcontrolador, como última línea de
  protección ante fallas de software.
- **RNF-04**: El firmware debe usar un watchdog de Zephyr que, ante un
  cuelgue del sistema, fuerce el apagado seguro de las resistencias.
- **RNF-05**: El control de potencia AC (SSR) debe mantener aislamiento
  eléctrico adecuado entre la lógica de bajo voltaje y la red eléctrica.

### 4.2 Portabilidad y mantenibilidad

- **RNF-06**: El código de aplicación (máquina de estados, control PID,
  gestión de perfiles) no debe depender de APIs específicas de un
  fabricante de MCU; debe usar exclusivamente las APIs de Zephyr (drivers de
  SPI, GPIO, PWM, sensor).
- **RNF-07**: Portar el firmware a una nueva placa soportada por Zephyr debe
  requerir únicamente un nuevo archivo de devicetree/overlay y, como máximo,
  ajustes de configuración (`prj.conf`), sin tocar la lógica de negocio.
- **RNF-08**: El código debe seguir una arquitectura modular (capas: drivers,
  control térmico, gestión de perfiles, comunicación) para facilitar pruebas
  unitarias y mantenimiento.

### 4.3 Desempeño y precisión

- **RNF-09**: El lazo de control debe mantener la temperatura real dentro de
  una tolerancia de ±5 °C respecto a la curva objetivo en las fases de
  rampa, y ±3 °C en las fases de meseta (soak/reflow), condiciones normales
  de carga.
- **RNF-10**: La latencia entre la medición de temperatura y la
  actualización del estado mostrado en la app no debe superar 1-2 segundos
  en condiciones normales de conexión.

### 4.4 Fiabilidad

- **RNF-11**: Ante una desconexión de la app durante un trabajo en curso, el
  horno debe continuar ejecutando el perfil de forma autónoma (la
  supervisión remota es opcional, no crítica para completar el proceso).
- **RNF-12**: Ante un corte de energía durante un trabajo, el sistema debe
  reiniciar en estado seguro (Idle) y no reanudar automáticamente el
  calentamiento sin confirmación del usuario.

### 4.5 Usabilidad

- **RNF-13**: La app móvil debe permitir a un usuario sin conocimientos
  técnicos de electrónica iniciar un trabajo de soldadura con un perfil
  predefinido en 3 pasos o menos.

### 4.6 Compatibilidad

- **RNF-14**: El sistema debe operar con la tensión de red eléctrica
  colombiana (110-120 VAC, 60 Hz).
- **RNF-15**: El diseño debe ser compatible con el rango de medición del
  MAX6675 (0-800 °C, resolución 0.25 °C) y con el rango típico de un proceso
  de reflow (hasta ~250 °C).

## 5. Restricciones

- El horno base es un horno tostador comercial adaptado, no un horno de
  reflow industrial: la potencia disponible y la uniformidad térmica están
  limitadas por el equipo elegido (ver `docs/02-COMPRAS`).
- El MAX6675 solo soporta lectura de temperatura positiva y no permite
  compensación de junta fría configurable ni lectura de termocupla tipo
  distinto a K.

## 6. Supuestos

- El usuario final asume la responsabilidad de las modificaciones físicas al
  horno (instalación de SSR, termocupla, aislamiento) siguiendo buenas
  prácticas eléctricas.
- La app móvil se desarrollará para al menos una plataforma (Android y/o
  iOS); la plataforma específica se define en una etapa posterior de diseño.
