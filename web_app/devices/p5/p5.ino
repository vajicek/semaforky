/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_webui_common.h"

BaseSettings baseSettings {
	true, // serve spa
	false, // force_hotspot
	"p5-01", // dns
	"semaforky", // ssid
	"semaforky", // password
	"clock,semaphore,lines" }; // capabilities
P5 client(&baseSettings, LINE);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }

