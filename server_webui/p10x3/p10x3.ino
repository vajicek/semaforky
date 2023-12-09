/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "common.h"

BaseSettings baseSettings{
	true, // serve spa
	true, // hotspot
	"semaforky", // dns
	"semaforky", // ssid
	"semaforky"}; // password
P10x3 client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
