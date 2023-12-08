/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "common.h"

Siren client(true, true, "semaforky");

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }
