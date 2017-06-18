/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_common.h"

ClockProcess global;

void setup() {
  global.Init();
  if (!wifi_disabled) {
    ConnectWiFiAP();
    PrintWiFiInfo();
  }
}

void loop() {
  global.Execute();
}



