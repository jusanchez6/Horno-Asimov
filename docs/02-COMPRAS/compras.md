# Lista de Compras — Horno de Reflow

> Nota: los precios en Colombia cambian con frecuencia (promociones, IVA,
> disponibilidad). Los valores aquí son una referencia tomada en
> septiembre de 2026 — **verificar el precio vigente antes de comprar**.

## 1. Sensado de temperatura

### Kit Termocupla tipo K + MAX6675

- **Producto**: Kit Termo K — termocupla tipo K + módulo amplificador MAX6675
- **Proveedor**: Didácticas Electrónicas
- **Link**: https://www.didacticaselectronicas.com/shop/kit-termo-k-kit-termocupla-tipo-k-3479
- **Precio de referencia**: ~$22.253 COP
- **Contenido del kit**:
  - Módulo MAX6675 (conversión ADC de 12 bits, salida SPI)
  - Sonda termocupla tipo K, punta ~13.5 cm, cable ~34 cm
- **Especificaciones relevantes**:
  - Alimentación: 3.3–5 V
  - Rango de medición: 0–800 °C
  - Resolución: 0.25 °C
  - Comunicación: SPI (solo lectura)
- **Por qué esta elección**: es el sensor especificado en los requerimientos
  (`docs/01-REQUERIMIENTOS/requerimientos.md`, RF-01/RF-02) y ya viene
  integrado con el MAX6675, evitando circuitos de amplificación discretos.

## 2. Horno base a adecuar

Criterios de selección para que un horno tostador comercial sirva de base
para un horno de reflow:

- Resistencias **superior e inferior independientes** (idealmente
  seleccionables por separado), para poder favorecer una u otra fase de la
  curva (RF-04).
- Potencia entre 1200 W y 1600 W aproximadamente: suficiente para alcanzar
  ~250 °C con rampas razonables, sin disparar el breaker de un circuito
  doméstico típico.
- Cámara metálica (no plástico expuesto cerca de las resistencias) y puerta
  con vidrio, para poder observar el proceso.
- Tamaño interno suficiente para placas electrónicas típicas (al menos
  20 x 20 cm de bandeja).
- Preferible control mecánico simple (perilla/temporizador análogo) en vez de
  panel táctil/electrónico complejo: es más fácil de intervenir para retirar
  el control original y conectar el SSR controlado por el microcontrolador.

### ✅ Opción elegida: Home Elements HEHT09N (9 L, 800 W)

- **Link**: https://www.homecenter.com.co/homecenter-co/product/3044138/horno-tostador-9l-negro-800w/3044138/
- **Precio de referencia**: ~$183.600–$251.900 COP según tienda (Homecenter,
  Falabella, otras) — **dentro del techo de $300.000 COP**.
- **Especificaciones**:
  - Capacidad: 9 L
  - Potencia: 800 W, 110 V
  - Termostato mecánico regulable: 100–250 °C
  - Temporizador: hasta 30 min
  - Doble resistencia tubular (superior e inferior)
  - Puerta de vidrio templado, parrilla cromada
  - Dimensiones externas: 34 × 22 × 20 cm — peso 2.9 kg
- **Por qué se eligió**:
  - Es la opción que mejor cumple el presupuesto de $300.000 COP.
  - Trae **doble resistencia tubular** (superior e inferior); hay que
    confirmar al desarmarlo que estén cableadas por separado para poder
    conectarlas a dos salidas SSR independientes (RF-04). Si internamente
    están unidas, se puede seguir controlando como una sola salida.
  - Termostato mecánico simple, fácil de retirar para instalar el control
    por SSR + MCU.
- **Desventajas conocidas frente a un horno más grande tipo Kalley K-HA20
  (20 L, 1500 W, con convección)**:
  - **Cámara pequeña**: la bandeja interna queda por debajo del criterio de
    ~20×20 cm planteado arriba, así que limita el tamaño máximo de PCB a
    procesar (adecuado para placas y prototipos pequeños/medianos, no para
    placas grandes).
  - **Sin convección/ventilador**: mayor probabilidad de gradiente térmico
    dentro de la cámara (puntos calientes cerca de las resistencias); puede
    requerir calibración/offset según la posición de la termocupla para
    cumplir RNF-09 (±3–5 °C respecto a la curva objetivo).
  - **Menor potencia (800 W)**: en compensación, al ser una cámara más
    pequeña necesita calentar menos volumen de aire, pero da menos margen
    térmico si hay fugas de calor por la puerta/sellos.
- **Acción antes de modificar**: abrir el horno y verificar el cableado
  interno de las dos resistencias (separadas vs. unidas) antes de decidir si
  se usan uno o dos SSR.

## 3. Pendiente de definir en una siguiente iteración de compras

- Relé de estado sólido (SSR) para control de las resistencias AC (uno o dos,
  según si se controla superior/inferior por separado).
- Fusible térmico / corte de seguridad por hardware (RNF-03).
- Placa de desarrollo compatible con Zephyr para el firmware (definir según
  el board de referencia elegido para el primer prototipo).
- Módulo de conectividad BLE/Wi-Fi (si no viene integrado en la placa
  elegida).
