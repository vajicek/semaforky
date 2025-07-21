/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_webui_common.h"

BaseSettings baseSettings {
	true, // serve spa
	false, // force_hotspot
	"p10x3-01", // dns
	"semaforky", // ssid
	"semaforky", // password
	"countdown,lines" }; // capabilities
P10x3 client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
