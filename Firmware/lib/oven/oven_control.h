/*
 * Capa de control del horno: maquina de estados + simulacion termica.
 * No sabe nada de BLE ni de ningun protocolo de comunicacion - solo expone
 * comandos (start/cancel) y una foto de solo lectura del estado actual
 * (oven_get_snapshot). Hoy la temperatura es simulada; cuando se integre
 * el MAX6675 (SPI) y el control de los SSR, esta es la capa que cambia -
 * el resto de la aplicacion (BLE, main) no deberia necesitar tocarse.
 */
#ifndef OVEN_CONTROL_H_
#define OVEN_CONTROL_H_

#include <stdint.h>

/* Valores de OvenState. Coinciden por diseno con el byte "state" del
 * paquete de Estado del protocolo
 * (docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md), pero este enum es el
 * modelo de dominio del horno, no un detalle de BLE.
 */
enum oven_state_code {
	OVEN_STATE_IDLE = 0,
	OVEN_STATE_PREHEAT = 1,
	OVEN_STATE_SOAK = 2,
	OVEN_STATE_REFLOW = 3,
	OVEN_STATE_COOLING = 4,
	OVEN_STATE_DONE = 5,
	OVEN_STATE_FAULT = 6,
};

/* Foto de solo lectura del estado del horno en un instante dado. */
struct oven_snapshot {
	uint8_t state;
	int16_t current_temp_x10; /* grados C x10 */
	int16_t target_temp_x10;  /* grados C x10 */
	uint16_t elapsed_s;
	uint16_t remaining_s;
	uint8_t profile_id;
	uint8_t fault_code;
};

/** Inicia un trabajo con el perfil indicado (comando START_JOB). */
void oven_start_job(uint8_t profile_id);

/** Cancela el trabajo en curso y vuelve a IDLE (comando CANCEL_JOB). */
void oven_cancel_job(void);

/** Avanza la simulacion un paso. Se espera 1 llamada por segundo. */
void oven_tick(void);

/** Copia el estado actual en *out (thread-safe). */
void oven_get_snapshot(struct oven_snapshot *out);

#endif /* OVEN_CONTROL_H_ */
