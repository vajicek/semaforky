#include "semaforky_webui_common_esp32.h"

BaseSettings baseSettings{true,               // serve spa
                          false,              // force_hotspot
                          "p10x3-01",         // dns
                          "semaforky",        // ssid
                          "semaforky",        // password
                          "clock,lines"};     // capabilities
P10x3 client(&baseSettings);

void setup(void) { client.Init(); }

void loop(void) { client.Execute(); }

