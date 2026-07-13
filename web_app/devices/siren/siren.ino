/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_webui_common_esp8266.h"

BaseSettings baseSettings {
	true, // serve spa
	false, // force_hotspot
	"siren-01", // dns
	"semaforky", // ssid
	"semaforky", // password
	"siren" }; // capabilities
Siren client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
