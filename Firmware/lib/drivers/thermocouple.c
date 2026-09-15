#include "drivers/thermocouple.h"

#include <zephyr/device.h>
#include <zephyr/devicetree.h>
#include <zephyr/drivers/sensor.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(thermocouple, LOG_LEVEL_INF);

/* El alias "oven-temp" lo define boards/xiao_esp32s3_procpu.overlay */
static const struct device *const dev = DEVICE_DT_GET(DT_ALIAS(oven_temp));

bool thermocouple_init(void)
{
	if (!device_is_ready(dev)) {
		LOG_ERR("MAX6675 no esta listo. Revisa cableado (VCC/GND/SCK/SO/CS) y overlay.");
		return false;
	}

	LOG_INF("MAX6675 listo (%s)", dev->name);
	return true;
}

bool thermocouple_read(int16_t *out_temp_x10)
{
	struct sensor_value val;
	int ret;

	ret = sensor_sample_fetch_chan(dev, SENSOR_CHAN_AMBIENT_TEMP);
	if (ret < 0) {
		/* -ENOENT = termocupla desconectada (bit D2 del MAX6675),
		 * cualquier otro codigo = fallo de la transaccion SPI.
		 */
		return false;
	}

	ret = sensor_channel_get(dev, SENSOR_CHAN_AMBIENT_TEMP, &val);
	if (ret < 0) {
		return false;
	}

	/* val1 = grados enteros, val2 = millonesimas de grado */
	*out_temp_x10 = (int16_t)(val.val1 * 10 + val.val2 / 100000);

	return true;
}
