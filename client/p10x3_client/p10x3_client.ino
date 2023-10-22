/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "semaforky_common.h"

MonoMatrixDisplayProcess global;

#define ESP8266_TIMER0_TICKS microsecondsToClockCycles(250)

static void inline ICACHE_RAM_ATTR esp8266_ISR_wrapper() {
  noInterrupts();
  global.dmd.scanDisplay();
  timer0_write(ESP.getCycleCount() + ESP8266_TIMER0_TICKS);
  interrupts();
}

void setup() {
  global.Init();

  noInterrupts();
  timer0_isr_init();
  timer0_attachInterrupt(esp8266_ISR_wrapper);
  timer0_write(ESP.getCycleCount() + ESP8266_TIMER0_TICKS);
  interrupts();
}

void loop() {
  global.Execute();
}

