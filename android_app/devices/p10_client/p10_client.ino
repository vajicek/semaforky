/// Copyright (C) 2022, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_common.h"

RgbMatrixDisplayProcess32 global;

void setup() {
  global.Init();
}

void loop() {
  global.Execute();
}

