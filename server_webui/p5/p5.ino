/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "common.h"

P5 client(false, false, "p5_01");

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
