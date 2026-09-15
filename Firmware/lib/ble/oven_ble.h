/*
 * Capa de comunicacion BLE: advertising, servicio GATT (Estado/Comando/Salud)
 * y la serializacion de paquetes segun
 * docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md. No conoce el control termico
 * real - solo llama a oven_start_job()/oven_cancel_job() (lib/oven/) y lee
 * snapshots via oven_get_snapshot().
 */
#ifndef OVEN_BLE_H_
#define OVEN_BLE_H_

/** Habilita el stack BT y arranca el advertising. */
int oven_ble_init(void);

/** Arma y notifica el paquete de Estado actual (llamar ~1 vez por segundo). */
void oven_ble_notify_estado(void);

/** Arma y notifica el paquete de Salud actual (llamar ~cada 2 segundos). */
void oven_ble_notify_salud(void);

#endif /* OVEN_BLE_H_ */
