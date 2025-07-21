/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_webui_common.h"

BaseSettings baseSettings {
	true, // serve spa
	false, // force_hotspot
	"p10-02", // dns
	"semaforky", // ssid
	"semaforky", // password
	"semaphore" }; // capabilities
P10Semaphore client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
