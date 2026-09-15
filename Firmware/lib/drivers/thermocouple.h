/*
 * Capa de driver: envuelve el sensor MAX6675 (termocupla tipo K, SPI) detras
 * de dos funciones simples. El resto de la aplicacion (lib/oven/) no sabe
 * nada de la API de sensores de Zephyr ni de que el sensor es un MAX6675 -
 * si el dia de manana se cambia de sensor, esta es la unica capa que cambia.
 */
#ifndef THERMOCOUPLE_H_
#define THERMOCOUPLE_H_

#include <stdbool.h>
#include <stdint.h>

/** Verifica que el dispositivo este listo. Llamar una vez al arrancar. */
bool thermocouple_init(void);

/**
 * Lee la temperatura actual en grados C x10 (ej. 253 = 25.3 C).
 *
 * Devuelve true si la lectura es valida. Devuelve false si la termocupla
 * esta desconectada/en falla (bit D2 del MAX6675, ver RF-02) o si fallo la
 * transaccion SPI - en ese caso *out_temp_x10 no se toca.
 */
bool thermocouple_read(int16_t *out_temp_x10);

#endif /* THERMOCOUPLE_H_ */
