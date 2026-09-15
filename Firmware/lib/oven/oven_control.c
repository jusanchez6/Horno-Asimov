#include "oven/oven_control.h"

#include "drivers/thermocouple.h"

#include <zephyr/kernel.h>

/* La lectura de temperatura ya es real (MAX6675 via SPI, lib/drivers/
 * thermocouple.c). Lo que todavia no existe es el control de las
 * resistencias (SSR): sin eso el horno no puede realmente calentar, asi
 * que la progresion de un trabajo (elapsed_s/remaining_s -> DONE) sigue
 * siendo una cuenta regresiva de tiempo fijo, no una curva real siguiendo
 * al setpoint. Cuando se agregue el SSR esto se reemplaza por RF-03/RF-05.
 */
#define SIM_TARGET_TEMP_X10 1500 /* 150.0 C */
#define SIM_IDLE_TEMP_X10    250 /* 25.0 C, valor inicial antes de la primera lectura real */
#define SIM_JOB_DURATION_S    60

static struct oven_snapshot oven = {
	.state = OVEN_STATE_IDLE,
	.current_temp_x10 = SIM_IDLE_TEMP_X10,
	.target_temp_x10 = SIM_IDLE_TEMP_X10,
	.elapsed_s = 0,
	.remaining_s = 0,
	.profile_id = 0,
	.fault_code = OVEN_FAULT_NONE,
	.sensor_ok = false,
};

/* oven_start_job()/oven_cancel_job() corren en el thread de BT RX (los
 * dispara comando_write() en lib/ble/); oven_tick()/oven_get_snapshot()
 * corren en el thread de main. Ambos tocan `oven`, por eso el mutex.
 */
K_MUTEX_DEFINE(oven_mutex);

bool oven_control_init(void)
{
	return thermocouple_init();
}

void oven_start_job(uint8_t profile_id)
{
	k_mutex_lock(&oven_mutex, K_FOREVER);
	oven.state = OVEN_STATE_PREHEAT;
	oven.profile_id = profile_id;
	oven.target_temp_x10 = SIM_TARGET_TEMP_X10;
	oven.elapsed_s = 0;
	oven.remaining_s = SIM_JOB_DURATION_S;
	oven.fault_code = 0;
	k_mutex_unlock(&oven_mutex);
}

void oven_cancel_job(void)
{
	k_mutex_lock(&oven_mutex, K_FOREVER);
	oven.state = OVEN_STATE_IDLE;
	oven.target_temp_x10 = SIM_IDLE_TEMP_X10;
	oven.elapsed_s = 0;
	oven.remaining_s = 0;
	k_mutex_unlock(&oven_mutex);
}

void oven_tick(void)
{
	int16_t measured_temp_x10;
	bool sensor_ok;

	/* La transaccion SPI se hace fuera del mutex, para no bloquear a
	 * oven_start_job()/oven_cancel_job() (que corren en el thread de BT
	 * RX) mientras dura la lectura.
	 */
	sensor_ok = thermocouple_read(&measured_temp_x10);

	k_mutex_lock(&oven_mutex, K_FOREVER);

	oven.sensor_ok = sensor_ok;
	oven.fault_code = sensor_ok ? OVEN_FAULT_NONE : OVEN_FAULT_SENSOR;
	if (sensor_ok) {
		oven.current_temp_x10 = measured_temp_x10;
	}
	/* Si la lectura falla, se mantiene el ultimo valor valido conocido
	 * en vez de mostrar un dato inventado (fail-safe, RNF-02).
	 */

	if (oven.state == OVEN_STATE_PREHEAT) {
		oven.elapsed_s++;
		oven.remaining_s = oven.remaining_s > 0 ? oven.remaining_s - 1 : 0;

		if (oven.elapsed_s >= SIM_JOB_DURATION_S) {
			oven.state = OVEN_STATE_DONE;
			oven.remaining_s = 0;
		}
	}

	k_mutex_unlock(&oven_mutex);
}

void oven_get_snapshot(struct oven_snapshot *out)
{
	k_mutex_lock(&oven_mutex, K_FOREVER);
	*out = oven;
	k_mutex_unlock(&oven_mutex);
}
