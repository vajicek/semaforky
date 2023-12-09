/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "common.h"

BaseSettings baseSettings{
	false, // serve spa
	false, // hotspot
	"p5-01", // dns
	"semaforky", // ssid
	"semaforky"}; // password
P5 client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
