#include "Arduino.h"

#include "AsyncJson.h"
#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <ESP8266mDNS.h>

#include <FS.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>

#include <Ticker.h>
#include <SPI.h>
#include <DMD2.h>
#include "digits32.h"

struct MyDMD : public SPIDMD {
  MyDMD(byte panelsWide, byte panelsHigh) :
    SPIDMD(panelsWide, panelsHigh) {
  }

  void setPixel(unsigned int x1, unsigned int y1, DMDGraphicsMode mode) {
    unsigned int x, y;
    map_panel_coords(x1, y1, &x, &y);
    SPIDMD::setPixel(x, y, mode);
  }

  void map_panel_coords(unsigned int x, unsigned int y,
                        unsigned int* tx, unsigned int* ty) {
    const int PANEL_H = 16;
    const int PANEL_W = 32;
    const int PANELS = 3;
    const int MAX_X = PANEL_H * PANELS - 1;

    int panel = (MAX_X - x) / PANEL_H;
    *tx = (PANELS - 1) * PANEL_W - (PANELS - 1 - panel) * PANEL_W + y;
    *ty = PANEL_H - 1 - x % PANEL_H;
  }
};

MyDMD dmd(3, 1);
MyDMD buffer(3, 1);

void computeDigits(int* digits, int digit_count, int value) {
  int divider = 1;
  for (int i = 0; i < digit_count; i++) {
    if (value / divider == 0 && divider > 1) {
      // value is lower than order represented by i-th digit
      // does not apply for 1st order, i.e. if value=0
      digits[digit_count - i - 1] = - 1;
    } else {
      digits[digit_count - i - 1] = (value / divider) % 10;
    }
    divider *= 10;
  }
}

int decodeTimeValue(int value) {
  return value & 0x0000ffff;
}

int decodeColorValue(int value) {
  return (value & 0x00ff0000) >> 16;
}

int decodeBrightnessValue(int value) {
  return (value & 0xff000000) >> 24;
}

void drawDigits(int val) {
  auto time = decodeTimeValue(val);
  auto color = decodeColorValue(val);
  auto brightness = decodeBrightnessValue(val);

  buffer.fillScreen(false);

  int digits[4];
  computeDigits(digits, 4, time);
  for (int i = 3; i > 0; i--) {
    if (digits[i] >= 0) {
      buffer.drawChar((i - 1) * 16, 0, '0' + digits[i], GRAPHICS_ON, Digits32);
    }
  }

  dmd.setBrightness(brightness);
}

int old_value = -1;
int value = 0| (30 << 24);
bool buffer_swapped = false;

void updateDisplay() {
  if (old_value != value) {
    old_value = value;
    drawDigits(value);
    buffer_swapped = false;
  }
}

void redrawDisplay() {
  if (!buffer_swapped) {
    dmd.swapBuffers(buffer);
    buffer_swapped = true;
  }
  dmd.scanDisplay();
}

Ticker display_ticker;

void initDisplay() {
  dmd.beginNoTimer();
  display_ticker.attach(0.003, redrawDisplay);
}

AsyncWebServer server(80);

void setCrossOrigin(AsyncWebServerResponse* response) {
  response->addHeader("Access-Control-Allow-Origin", "*");
  response->addHeader("Access-Control-Max-Age", "600");
  response->addHeader("Access-Control-Allow-Methods", "PUT,POST,GET,OPTIONS");
  response->addHeader("Access-Control-Allow-Headers", "*");
}

void sendCrossOriginHeader(AsyncWebServerRequest *request) {
  Serial.println(F("sendCORSHeader"));
  AsyncWebServerResponse* response = request->beginResponse(204);
  response->addHeader("access-control-allow-credentials", "false");
  setCrossOrigin(response);
  request->send(response);
}
 
void control(AsyncWebServerRequest *request, JsonVariant &json) {
  const JsonObject& postObj = json.as<JsonObject>();
  if (!postObj.containsKey("value")) {
    request->send(200);
  } else {
    AsyncWebServerResponse* response = request->beginResponse(200);
    setCrossOrigin(response);
    request->send(response);
    value = postObj[F("value")];
  }
}

void restServerRouting() {
  server.serveStatic("/", SPIFFS, "/").setDefaultFile("index.html");
  server.addHandler(new AsyncCallbackJsonWebHandler("/control", control));
  server.on("/control", HTTP_OPTIONS, sendCrossOriginHeader);
}

void connectToHotspot(const char *ssid, const char *password) {
  // Init WIFI
  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);
  Serial.println("");

  // Wait for connection
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
  Serial.print("Connected to ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(WiFi.localIP());
}

void setupHotspot(const char *ssid, const char *password) {
  Serial.println();

  WiFi.mode(WIFI_AP);

  Serial.print("Setting soft-AP ... ");
  Serial.println(WiFi.softAP(ssid, password) ? "Ready" : "Failed!");
  
  Serial.print("Soft-AP IP address = ");
  Serial.println(WiFi.softAPIP());
}

void setup(void) {
  Serial.begin(115200);

  // Initialize SPIFFS
  if (!SPIFFS.begin()) {
    Serial.println("An Error has occurred while mounting SPIFFS");
    return;
  }

  // Initialize Wifi
  setupHotspot("semaforky", "semaforky");

	// Initialize mDNS
	if (MDNS.begin("esp8266", WiFi.localIP())) {
		Serial.println("MDNS responder started");
	}
  MDNS.addService("http", "tcp", 80);

	// Initialize HTTP Server
	restServerRouting();
	server.begin();
	Serial.println("HTTP server started");
  
  // Initialize Display
  initDisplay();
}

void loop(void) {
  MDNS.update();
  updateDisplay();
}
