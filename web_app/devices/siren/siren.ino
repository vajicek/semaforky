/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_webui_common.h"

BaseSettings baseSettings{
	false, // serve spa
	false, // hotspot
	"siren-01", // dns
	"semaforky", // ssid
	"semaforky", // password
	"siren"}; // capabilities
Siren client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
