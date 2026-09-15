#include "ble/oven_ble.h"

#include "oven/oven_control.h"

#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

#include <zephyr/bluetooth/att.h>
#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/gatt.h>
#include <zephyr/bluetooth/hci.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/sys/byteorder.h>

LOG_MODULE_REGISTER(oven_ble, LOG_LEVEL_INF);

#define DEVICE_NAME CONFIG_BT_DEVICE_NAME
#define DEVICE_NAME_LEN (sizeof(DEVICE_NAME) - 1)

/* Servicio y caracteristicas del protocolo (docs/03-INTERFAZ-APP-FIRMWARE/protocolo.md) */
#define BT_UUID_OVEN_SERVICE_VAL \
	BT_UUID_128_ENCODE(0x12345678, 0x0001, 0x1000, 0x8000, 0x00805f9b34fb)
#define BT_UUID_OVEN_SERVICE BT_UUID_DECLARE_128(BT_UUID_OVEN_SERVICE_VAL)

#define BT_UUID_OVEN_ESTADO_VAL \
	BT_UUID_128_ENCODE(0x12345678, 0x0002, 0x1000, 0x8000, 0x00805f9b34fb)
#define BT_UUID_OVEN_ESTADO BT_UUID_DECLARE_128(BT_UUID_OVEN_ESTADO_VAL)

#define BT_UUID_OVEN_COMANDO_VAL \
	BT_UUID_128_ENCODE(0x12345678, 0x0003, 0x1000, 0x8000, 0x00805f9b34fb)
#define BT_UUID_OVEN_COMANDO BT_UUID_DECLARE_128(BT_UUID_OVEN_COMANDO_VAL)

#define BT_UUID_OVEN_SALUD_VAL \
	BT_UUID_128_ENCODE(0x12345678, 0x0004, 0x1000, 0x8000, 0x00805f9b34fb)
#define BT_UUID_OVEN_SALUD BT_UUID_DECLARE_128(BT_UUID_OVEN_SALUD_VAL)

/* Opcodes del paquete de Comando (primer byte escrito por la app) */
#define CMD_START_JOB  1
#define CMD_CANCEL_JOB 2
#define CMD_PING       3

/* Tamanos de paquete, little-endian (ver protocolo.md) */
#define ESTADO_PKT_LEN 12
#define SALUD_PKT_LEN  14

/* Advertising data: flags + nombre. Sin scan response por ahora. */
static const struct bt_data ad[] = {
	BT_DATA_BYTES(BT_DATA_FLAGS, BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR),
	BT_DATA(BT_DATA_NAME_COMPLETE, DEVICE_NAME, DEVICE_NAME_LEN),
};

static void estado_ccc_changed(const struct bt_gatt_attr *attr, uint16_t value)
{
	ARG_UNUSED(attr);

	LOG_INF("Notificaciones de Estado %s", value == BT_GATT_CCC_NOTIFY ? "activadas" : "desactivadas");
}

static ssize_t comando_write(struct bt_conn *conn, const struct bt_gatt_attr *attr,
			      const void *buf, uint16_t len, uint16_t offset, uint8_t flags)
{
	const uint8_t *data = buf;

	ARG_UNUSED(conn);
	ARG_UNUSED(attr);
	ARG_UNUSED(flags);

	if (offset != 0) {
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_OFFSET);
	}
	if (len < 1) {
		return BT_GATT_ERR(BT_ATT_ERR_INVALID_ATTRIBUTE_LEN);
	}

	switch (data[0]) {
	case CMD_START_JOB: {
		uint8_t profile_id = len >= 2 ? data[1] : 0;

		LOG_INF("Comando: START_JOB, profile_id=%u", profile_id);
		oven_start_job(profile_id);
		break;
	}
	case CMD_CANCEL_JOB:
		LOG_INF("Comando: CANCEL_JOB");
		oven_cancel_job();
		break;
	case CMD_PING:
		LOG_INF("Comando: PING");
		break;
	default:
		LOG_WRN("Comando: opcode desconocido %u", data[0]);
		break;
	}

	return len;
}

static void salud_ccc_changed(const struct bt_gatt_attr *attr, uint16_t value)
{
	ARG_UNUSED(attr);

	LOG_INF("Notificaciones de Salud %s", value == BT_GATT_CCC_NOTIFY ? "activadas" : "desactivadas");
}

/* Indices dentro de oven_svc.attrs[] (BT_GATT_CHARACTERISTIC agrega 2
 * entradas cada uno: declaracion + valor; BT_GATT_CCC agrega 1):
 *   0 primary service
 *   1 Estado: declaracion   2 Estado: valor   3 Estado: CCC
 *   4 Comando: declaracion  5 Comando: valor
 *   6 Salud: declaracion    7 Salud: valor    8 Salud: CCC
 * bt_gatt_notify() acepta el atributo de declaracion y resuelve solo
 * el de valor (attr + 1), por eso notificamos con los indices 1 y 6.
 */
BT_GATT_SERVICE_DEFINE(oven_svc,
	BT_GATT_PRIMARY_SERVICE(BT_UUID_OVEN_SERVICE),
	BT_GATT_CHARACTERISTIC(BT_UUID_OVEN_ESTADO, BT_GATT_CHRC_NOTIFY,
				BT_GATT_PERM_NONE, NULL, NULL, NULL),
	BT_GATT_CCC(estado_ccc_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),
	BT_GATT_CHARACTERISTIC(BT_UUID_OVEN_COMANDO, BT_GATT_CHRC_WRITE,
				BT_GATT_PERM_WRITE, NULL, comando_write, NULL),
	BT_GATT_CHARACTERISTIC(BT_UUID_OVEN_SALUD, BT_GATT_CHRC_NOTIFY,
				BT_GATT_PERM_NONE, NULL, NULL, NULL),
	BT_GATT_CCC(salud_ccc_changed, BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),
);

static void start_advertising(void)
{
	int err = bt_le_adv_start(BT_LE_ADV_CONN_FAST_1, ad, ARRAY_SIZE(ad), NULL, 0);

	if (err) {
		LOG_ERR("No se pudo arrancar advertising (err %d)", err);
		return;
	}

	LOG_INF("Advertising arrancado como \"%s\"", DEVICE_NAME);
}

static void connected(struct bt_conn *conn, uint8_t err)
{
	ARG_UNUSED(conn);

	if (err) {
		LOG_ERR("Conexion fallida (err 0x%02x)", err);
		return;
	}

	LOG_INF("Central conectado");
}

static void disconnected(struct bt_conn *conn, uint8_t reason)
{
	ARG_UNUSED(conn);

	LOG_INF("Central desconectado (razon 0x%02x), reanudando advertising", reason);

	/* Zephyr para el advertising automaticamente al conectar; sin esto el
	 * horno quedaria "sordo" para siempre despues de la primera conexion.
	 */
	start_advertising();
}

BT_CONN_CB_DEFINE(conn_callbacks) = {
	.connected = connected,
	.disconnected = disconnected,
};

static void bt_ready(int err)
{
	if (err) {
		LOG_ERR("Bluetooth init fallo (err %d)", err);
		return;
	}

	LOG_INF("Bluetooth inicializado");

	start_advertising();
}

int oven_ble_init(void)
{
	return bt_enable(bt_ready);
}

void oven_ble_notify_estado(void)
{
	struct oven_snapshot snap;
	uint8_t buf[ESTADO_PKT_LEN];
	int err;

	oven_get_snapshot(&snap);

	buf[0] = 1; /* protocol_version */
	buf[1] = snap.state;
	sys_put_le16((uint16_t)snap.current_temp_x10, &buf[2]);
	sys_put_le16((uint16_t)snap.target_temp_x10, &buf[4]);
	sys_put_le16(snap.elapsed_s, &buf[6]);
	sys_put_le16(snap.remaining_s, &buf[8]);
	buf[10] = snap.profile_id;
	buf[11] = snap.fault_code;

	err = bt_gatt_notify(NULL, &oven_svc.attrs[1], buf, sizeof(buf));
	if (err && err != -ENOTCONN) {
		LOG_ERR("bt_gatt_notify (Estado) fallo (err %d)", err);
	}
}

void oven_ble_notify_salud(void)
{
	struct oven_snapshot snap;
	uint8_t buf[SALUD_PKT_LEN];
	uint32_t uptime_s = (uint32_t)(k_uptime_get() / 1000);
	int err;

	oven_get_snapshot(&snap);

	/* Paquete de Salud (14 bytes, little-endian, ver protocolo.md seccion
	 * 3). sensor_ok ya es real (viene de la termocupla MAX6675); heap
	 * libre y temp. del chip siguen siendo placeholders (todavia no
	 * leemos esos datos del sistema).
	 */
	buf[0] = 1; /* protocol_version */
	sys_put_le32(uptime_s, &buf[1]);
	sys_put_le16(200, &buf[5]);           /* free_heap_kb: placeholder */
	sys_put_le16((uint16_t)390, &buf[7]); /* cpu_temp_c_x10: 39.0 C placeholder */
	buf[9] = 0;  /* reset_reason: POWER_ON */
	buf[10] = snap.sensor_ok ? 1 : 0;
	buf[11] = 0; /* fw_version major */
	buf[12] = 1; /* fw_version minor */
	buf[13] = 0; /* fw_version patch */

	err = bt_gatt_notify(NULL, &oven_svc.attrs[6], buf, sizeof(buf));
	if (err && err != -ENOTCONN) {
		LOG_ERR("bt_gatt_notify (Salud) fallo (err %d)", err);
	}
}
