#include "oven/oven_control.h"

#include <zephyr/kernel.h>

/* Simulacion de trabajo: todavia no hay control termico real (MAX6675 +
 * SSR), asi que esto es solo para probar que el circuito completo
 * app -> Comando -> firmware -> Estado -> app funciona de punta a punta.
 * Cuando se implemente el control termico real esto se reemplaza por la
 * maquina de estados y las curvas de RF-05/RF-06.
 */
#define SIM_TARGET_TEMP_X10 1500 /* 150.0 C */
#define SIM_IDLE_TEMP_X10    250 /* 25.0 C */
#define SIM_JOB_DURATION_S    60
#define SIM_TEMP_STEP_X10     30 /* +3.0 C por tick (1s) */

static struct oven_snapshot oven = {
	.state = OVEN_STATE_IDLE,
	.current_temp_x10 = SIM_IDLE_TEMP_X10,
	.target_temp_x10 = SIM_IDLE_TEMP_X10,
	.elapsed_s = 0,
	.remaining_s = 0,
	.profile_id = 0,
	.fault_code = 0,
};

/* oven_start_job()/oven_cancel_job() corren en el thread de BT RX (los
 * dispara comando_write() en lib/ble/); oven_tick()/oven_get_snapshot()
 * corren en el thread de main. Ambos tocan `oven`, por eso el mutex.
 */
K_MUTEX_DEFINE(oven_mutex);

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
	k_mutex_lock(&oven_mutex, K_FOREVER);

	if (oven.state == OVEN_STATE_PREHEAT) {
		oven.elapsed_s++;
		oven.remaining_s = oven.remaining_s > 0 ? oven.remaining_s - 1 : 0;

		if (oven.current_temp_x10 < oven.target_temp_x10) {
			oven.current_temp_x10 += SIM_TEMP_STEP_X10;
			if (oven.current_temp_x10 > oven.target_temp_x10) {
				oven.current_temp_x10 = oven.target_temp_x10;
			}
		}

		if (oven.elapsed_s >= SIM_JOB_DURATION_S ||
		    oven.current_temp_x10 >= oven.target_temp_x10) {
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
