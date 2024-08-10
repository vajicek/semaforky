/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_common.h"

SemaphoreProcess global;

void setup() {
  global.Init();
}

void loop() {
  global.Execute();
}

