/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_webui_common.h"

BaseSettings baseSettings{
	true, // serve spa
	true, // hotspot
	"semaforky", // dns
	"semaforky", // ssid
	"semaforky", // password
	"clock"}; // capabilities
P10x3 client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
