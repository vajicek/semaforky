/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include <semaforky_common.h>

SAA1064ClockProcess global;

void setup() {
  global.Init();
}

void loop() {
  global.Execute();
}

