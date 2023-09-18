/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_common.h"

RgbMatrixDisplayProcess64 global;

void setup() {
  global.Init();
}

void loop() {
  global.Execute();
}

