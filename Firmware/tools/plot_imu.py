#!/usr/bin/env python3
"""Lee las líneas de printk del demo MPU6050 por puerto serie y las grafica en vivo.

Uso:
    pip install pyserial matplotlib
    python3 plot_imu.py [-p /dev/ttyACM0] [-b 115200] [-n 200]
"""

import argparse
import math
import re
import threading
from collections import deque

import serial
import matplotlib.pyplot as plt
import matplotlib.animation as animation

STANDARD_GRAVITY = 9.80665  # m/s^2 por g
DEG_TO_RAD = math.pi / 180

LINE_RE = re.compile(
    r"accel\[mg\]=\((-?\d+)\s+(-?\d+)\s+(-?\d+)\)\s+"
    r"gyro\[mdps\]=\((-?\d+)\s+(-?\d+)\s+(-?\d+)\)\s+"
    r"temp\[cC\]=(-?\d+)"
)


def reader_thread(port, baud, buffers, stop_event):
    with serial.Serial(port, baud, timeout=1) as ser:
        while not stop_event.is_set():
            line = ser.readline().decode(errors="ignore").strip()
            match = LINE_RE.search(line)
            if not match:
                continue

            ax, ay, az, gx, gy, gz, temp = (int(v) for v in match.groups())

            # mg -> g -> m/s^2
            buffers["ax"].append(ax / 1000 * STANDARD_GRAVITY)
            buffers["ay"].append(ay / 1000 * STANDARD_GRAVITY)
            buffers["az"].append(az / 1000 * STANDARD_GRAVITY)

            # mdps -> dps -> rad/s
            buffers["gx"].append(gx / 1000 * DEG_TO_RAD)
            buffers["gy"].append(gy / 1000 * DEG_TO_RAD)
            buffers["gz"].append(gz / 1000 * DEG_TO_RAD)

            buffers["temp"].append(temp / 100)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("-p", "--port", default="/dev/ttyACM0")
    parser.add_argument("-b", "--baud", type=int, default=115200)
    parser.add_argument("-n", "--samples", type=int, default=200, help="muestras visibles en pantalla")
    args = parser.parse_args()

    buffers = {k: deque(maxlen=args.samples) for k in ("ax", "ay", "az", "gx", "gy", "gz", "temp")}

    stop_event = threading.Event()
    thread = threading.Thread(target=reader_thread, args=(args.port, args.baud, buffers, stop_event), daemon=True)
    thread.start()

    plt.style.use("dark_background")
    COLOR_X, COLOR_Y, COLOR_Z, COLOR_T = "#ff5c5c", "#5cff8f", "#5c9dff", "#ffd15c"

    fig, (ax_accel, ax_gyro, ax_temp) = plt.subplots(3, 1, sharex=True, figsize=(9, 7))
    fig.canvas.manager.set_window_title("MPU6050 - live plot")
    fig.suptitle("MPU6050", fontsize=14, fontweight="bold")

    lines = {
        "ax": ax_accel.plot([], [], label="x", color=COLOR_X, linewidth=1.6)[0],
        "ay": ax_accel.plot([], [], label="y", color=COLOR_Y, linewidth=1.6)[0],
        "az": ax_accel.plot([], [], label="z", color=COLOR_Z, linewidth=1.6)[0],
        "gx": ax_gyro.plot([], [], label="x", color=COLOR_X, linewidth=1.6)[0],
        "gy": ax_gyro.plot([], [], label="y", color=COLOR_Y, linewidth=1.6)[0],
        "gz": ax_gyro.plot([], [], label="z", color=COLOR_Z, linewidth=1.6)[0],
        "temp": ax_temp.plot([], [], label="temp", color=COLOR_T, linewidth=1.6)[0],
    }

    ax_accel.set_title("Acelerómetro", fontsize=10, loc="left", color="gray")
    ax_gyro.set_title("Giroscopio", fontsize=10, loc="left", color="gray")
    ax_temp.set_title("Temperatura", fontsize=10, loc="left", color="gray")

    ax_accel.set_ylabel("m/s²")
    ax_gyro.set_ylabel("rad/s")
    ax_temp.set_ylabel("°C")
    ax_temp.set_xlabel("muestra")

    for ax in (ax_accel, ax_gyro, ax_temp):
        ax.legend(loc="upper left", framealpha=0.3, ncols=3)
        ax.grid(True, alpha=0.25, linestyle="--")
        for spine in ax.spines.values():
            spine.set_alpha(0.3)

    def update(_frame):
        for key, line in lines.items():
            data = buffers[key]
            line.set_data(range(len(data)), data)

        for ax in (ax_accel, ax_gyro, ax_temp):
            ax.relim()
            ax.autoscale_view()

        return list(lines.values())

    anim = animation.FuncAnimation(fig, update, interval=200, blit=False)
    plt.tight_layout(rect=(0, 0, 1, 0.96))
    plt.show()

    stop_event.set()


if __name__ == "__main__":
    main()
