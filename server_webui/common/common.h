/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#define double_buffer

#include <Arduino.h>
#include <AsyncJson.h>
#include <DMD2.h>
#include <ESP8266WiFi.h>
#include <ESP8266mDNS.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <FS.h>
#include <PxMatrix.h>
#include <SPI.h>
#include <Ticker.h>

struct StackedDMD : public SPIDMD {
	StackedDMD() : SPIDMD(3, 1) {}

	void setPixel(unsigned int x1, unsigned int y1, DMDGraphicsMode mode) {
		unsigned int x, y;
		mapPanelCoords(x1, y1, &x, &y);
		SPIDMD::setPixel(x, y, mode);
	}

	void mapPanelCoords(
		unsigned int x, unsigned int y, unsigned int *tx, unsigned int *ty) {
		const int PANEL_H = 16;
		const int PANEL_W = 32;
		const int PANELS = 3;
		const int MAX_X = PANEL_H * PANELS - 1;

		int panel = (MAX_X - x) / PANEL_H;
		*tx = (PANELS - 1) * PANEL_W - (PANELS - 1 - panel) * PANEL_W + y;
		*ty = PANEL_H - 1 - x % PANEL_H;
	}
};

void computeDigits(int *digits, int digit_count, int value) {
	int divider = 1;
	for (int i = 0; i < digit_count; i++) {
		if (value / divider == 0 && divider > 1) {
			// value is lower than order represented by i-th digit
			// does not apply for 1st order, i.e. if value=0
			digits[digit_count - i - 1] = -1;
		} else {
			digits[digit_count - i - 1] = (value / divider) % 10;
		}
		divider *= 10;
	}
}

int decodeTimeValue(int value) { return value & 0x0000ffff; }

int decodeColorValue(int value) { return (value & 0x00ff0000) >> 16; }

int decodeBrightnessValue(int value) { return (value & 0xff000000) >> 24; }

///////////////////////////////////////////////////////////////////

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

struct BaseSettings {
	bool hostSpa;
	bool hotspot;
	const char *dns;
	const char *ssid;
	const char *password;
};

class Base {
 protected:
	int value;
	bool hostSpa;
	bool hotspot;
	const char *dns;
	const char *ssid;
	const char *password;

	void setCrossOrigin(AsyncWebServerResponse *response);
	void sendCrossOriginHeader(AsyncWebServerRequest *request);
	void control(AsyncWebServerRequest *request, JsonVariant &json);
	void restServerRouting();

	AsyncWebServer server;

 public:
	Base(BaseSettings *baseSettings);

	void Init();
	void Execute();
};

Base::Base(BaseSettings *baseSettings)
	: ssid{baseSettings->ssid}, password{baseSettings->password}, server{80},
	  hostSpa{baseSettings->hostSpa}, hotspot{baseSettings->hotspot},
	  dns{baseSettings->dns} {}

void Base::setCrossOrigin(AsyncWebServerResponse *response) {
	response->addHeader("Access-Control-Allow-Origin", "*");
	response->addHeader("Access-Control-Max-Age", "600");
	response->addHeader("Access-Control-Allow-Methods", "PUT,POST,GET,OPTIONS");
	response->addHeader("Access-Control-Allow-Headers", "*");
}

void Base::sendCrossOriginHeader(AsyncWebServerRequest *request) {
	AsyncWebServerResponse *response = request->beginResponse(204);
	response->addHeader("access-control-allow-credentials", "false");
	setCrossOrigin(response);
	request->send(response);
}

void Base::control(AsyncWebServerRequest *request, JsonVariant &json) {
	const JsonObject &postObj = json.as<JsonObject>();
	if (!postObj.containsKey("value")) {
		request->send(200);
	} else {
		AsyncWebServerResponse *response = request->beginResponse(200);
		setCrossOrigin(response);
		request->send(response);
		value = postObj[F("value")];
	}
}

void Base::restServerRouting() {
	if (hostSpa) {
		server.serveStatic("/", SPIFFS, "/").setDefaultFile("index.html");
	}

	server.addHandler(new AsyncCallbackJsonWebHandler(
		"/control", [this](AsyncWebServerRequest *request, JsonVariant &json) {
			this->control(request, json);
		}));
	server.on(
		"/control", HTTP_OPTIONS, [this](AsyncWebServerRequest *request) {
			this->sendCrossOriginHeader(request);
		});
}

void Base::Init() {
	Serial.begin(115200);

	if (hostSpa) {
		// Initialize SPIFFS
		if (!SPIFFS.begin()) {
			Serial.println("An Error has occurred while mounting SPIFFS");
			return;
		}
	}

	// Initialize Wifi
	if (hotspot) {
		setupHotspot(ssid, password);
	} else {
		connectToHotspot(ssid, password);
	}

	// Initialize mDNS
	if (MDNS.begin(dns, WiFi.localIP())) {
		Serial.println("MDNS responder started");
	}
	MDNS.addService("http", "tcp", 80);

	// Initialize HTTP Server
	restServerRouting();
	server.begin();
	Serial.println("HTTP server started");
}

void Base::Execute() { MDNS.update(); }

///////////////////////////////////////

#include "digits32.h"

class P10x3 : public Base {
 private:
	Ticker displayTicker;
	StackedDMD dmd;
	StackedDMD buffer;

	int oldValue = -1;
	bool bufferSwapped = false;

	void updateDisplay();
	void drawDigits();
	void redrawDisplay();

 public:
	void Init();
	void Execute();

	P10x3(BaseSettings *baseSettings);
};

void P10x3::drawDigits() {
	auto time = decodeTimeValue(value);
	auto color = decodeColorValue(value);
	auto brightness = decodeBrightnessValue(value);

	buffer.fillScreen(false);

	int digits[4];
	computeDigits(digits, 4, time);
	for (int i = 3; i > 0; i--) {
		if (digits[i] >= 0) {
			buffer.drawChar(
				(i - 1) * 16, 0, '0' + digits[i], GRAPHICS_ON, Digits32);
		}
	}

	dmd.setBrightness(brightness);
}

void P10x3::updateDisplay() {
	if (oldValue != value) {
		oldValue = value;
		drawDigits();
		bufferSwapped = false;
	}
}

void P10x3::redrawDisplay() {
	if (!bufferSwapped) {
		dmd.swapBuffers(buffer);
		bufferSwapped = true;
	}
	dmd.scanDisplay();
}

void P10x3::Init() {
	Base::Init();

	// Initialize Display
	dmd.beginNoTimer();
	displayTicker.attach(0.003, [this]() { this->redrawDisplay(); });
}

void P10x3::Execute() {
	Base::Execute();
	updateDisplay();
}

P10x3::P10x3(BaseSettings *baseSettings) : Base(baseSettings) {}

///////////////////////////////////////

#define SIREN_TONE_LENGTH 500
#define SIREN_PAUSE_LENGTH 500

// Wemos D1 mini
#define SIREN_AUDIO_PIN 5

class Siren : public Base {
 public:
	void Init();
	void Execute();

	Siren(BaseSettings *baseSettings);
};

Siren::Siren(BaseSettings *baseSettings) : Base(baseSettings) {}

void Siren::Init() {
	Base::Init();

	// Initialize siren control pin
	pinMode(SIREN_AUDIO_PIN, OUTPUT);
}

void Siren::Execute() {
	Base::Execute();
	for (int i = 0; i < value; i++) {
		digitalWrite(SIREN_AUDIO_PIN, HIGH);
		delay(SIREN_TONE_LENGTH);
		digitalWrite(SIREN_AUDIO_PIN, LOW);
		delay(SIREN_PAUSE_LENGTH);
	}
	value = 0;
}

///////////////////////////////////////

static uint16_t color565(uint8_t r, uint8_t g, uint8_t b) {
	return ((b & 0xF8) << 8) | ((g & 0xFC) << 3) | (r >> 3);
}

class Px : public Base {
 private:
	PxMATRIX *display;
	Ticker displayTicker;
	int digitWidth;
	int digitHeight;
	int digitSize;
	int oldValue;
	const uint16_t *colors;

 public:
	void Init();
	void Execute();

	Px(BaseSettings *baseSettings, PxMATRIX *_display, int digitWidth,
		int _digitHeight, int _digitSize, const uint16_t *_colors);
};

Px::Px(BaseSettings *baseSettings, PxMATRIX *_display, int _digitWidth,
	int _digitHeight, int _digitSize, const uint16_t *_colors)
	: Base(baseSettings), display(_display), digitWidth(_digitWidth),
	  digitHeight(_digitHeight), digitSize(_digitSize), oldValue(-1),
	  colors(_colors) {}

void Px::Init() {
	Base::Init();

	// Initialize Display
	display->begin(8);
	display->clearDisplay();
	displayTicker.attach_ms(4, [this]() { this->display->display(); });
}

void Px::Execute() {
	Base::Execute();

	if (oldValue == value) {
		return;
	}
	oldValue = value;

	display->fillScreen(color565(0, 0, 0));

	auto time = decodeTimeValue(value);
	auto color = decodeColorValue(value);
	auto brightness = decodeBrightnessValue(value);
	display->setBrightness(brightness);

	int digits[4];
	computeDigits(digits, 4, time);
	for (int i = 3; i > 0; i--) {
		if (digits[i] >= 0) {
			display->drawChar((i - 1) * digitWidth, digitHeight, // coords
				'0' + digits[i], // character
				colors[color], colors[color], // colors
				digitSize); // size
		}
	}
	display->showBuffer();
}

//////////////////////////

#define P10_LAT 16
#define P10_A 5
#define P10_B 4
#define P10_C 15
#define P10_OE 2

const uint16_t P10_COLORS[] = {
	color565(255, 255, 255), // white
	color565(255, 0, 0), // red
	color565(0, 255, 0), // green
	color565(255, 165, 0) // orange
};

class P10 : public Px {
 protected:
	PxMATRIX display;

 public:
	P10(BaseSettings *baseSettings);
};

P10::P10(BaseSettings *baseSettings)
	: display(32, 16, P10_LAT, P10_OE, P10_A, P10_B, P10_C),
	  Px(baseSettings, &display, 11, 1, 2, P10_COLORS) {}

//////////////////////////

#include "digits_font.h"

#define P5_LAT 16
#define P5_A 5
#define P5_B 4
#define P5_C 15
#define P5_D 12
#define P5_OE 2

const uint16_t P5_COLORS[] = {
	color565(255, 255, 255), // white
	color565(0, 0, 255), // red
	color565(0, 255, 0), // green
	color565(0, 165, 255) // orange
};

class P5 : public Px {
 protected:
	PxMATRIX display;

 public:
	P5(BaseSettings *baseSettings);
	virtual void Init();
};

P5::P5(BaseSettings *baseSettings)
	: display(64, 32, P5_LAT, P5_OE, P5_A, P5_B, P5_C, P5_D),
	  Px(baseSettings, &display, 21, 32, 1, P5_COLORS) {}

void P5::Init() {
	Px::Init();
	display.setFont(&FixedWidthDigit);
	display.setTextWrap(false);
}
