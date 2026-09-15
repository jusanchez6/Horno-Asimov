/*
 * Horno de reflujo SMD - orquestador.
 *
 * La logica vive en lib/: lib/oven/ (maquina de estados + simulacion
 * termica, todavia sin sensor/SSR reales) y lib/ble/ (advertising,
 * servicio GATT, protocolo con la app - ver
 * docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md). Este archivo solo
 * inicializa cada capa y corre el loop principal.
 */

#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

#include "ble/oven_ble.h"
#include "oven/oven_control.h"

LOG_MODULE_REGISTER(horno, LOG_LEVEL_INF);

int main(void)
{
	int err;
	int tick = 0;

	if (!oven_control_init()) {
		LOG_ERR("oven_control_init fallo (revisa la termocupla)");
	}

	err = oven_ble_init();
	if (err) {
		LOG_ERR("oven_ble_init fallo (err %d)", err);
	}

	while (true) {
		k_sleep(K_SECONDS(1));
		tick++;

		oven_tick();
		oven_ble_notify_estado();

		/* Salud: cada 2s */
		if (tick % 2 == 0) {
			oven_ble_notify_salud();
		}
	}

	return 0;
}
