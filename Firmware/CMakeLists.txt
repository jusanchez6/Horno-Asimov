cmake_minimum_required(VERSION 3.20.0)

find_package(Zephyr REQUIRED HINTS $ENV{ZEPHYR_BASE})
project(horno_reflujo)

target_include_directories(app PRIVATE lib)

target_sources(app PRIVATE
	src/main.c
	lib/oven/oven_control.c
	lib/ble/oven_ble.c
	lib/drivers/thermocouple.c
)
