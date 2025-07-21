/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#define double_buffer

#include <Arduino.h>
#include <DMD2.h>

#include <WiFiClient.h>
#include <ESP8266WiFi.h>
#include <ESP8266mDNS.h>

#include <ArduinoJson.h>
#include <AsyncJson.h>
#include <ESPAsyncTCP.h>
#include <ESPAsyncWebServer.h>

#include <FS.h>
#include <PxMatrix.h>
#include <SPI.h>
#include <Ticker.h>

#define CONTROL_VALUE_CLOCK 1
#define CONTROL_VALUE_SEMAPHORE 2
#define CONTROL_VALUE_LINES 3
#define CONTROL_VALUE_SIRENE 4
#define CONTROL_VALUE_COUNTDOWN 5

struct StackedDMD : public SPIDMD {
	unsigned int mapping[3 * 16 * 32];

	StackedDMD() : SPIDMD(3, 1) {
		computeMapping();
	}

	void computeMapping() {
		for (int y = 0; y < 32; y++) {
			for (int x = 0; x < 3 * 16; x++) {
				unsigned int tx, ty;
				mapPanelCoords(x, y, &tx, &ty);
				mapping[y * (3 * 16) + x] = (tx | ty << 16);
			}
		}
	}

	void setPixel(unsigned int x1, unsigned int y1, DMDGraphicsMode mode) {
		unsigned int x, y;
		unsigned xy = mapping[y1 * (3 * 16) + x1];
		x = xy & 0xFFFF;
		y = xy >> 16;
		// mapPanelCoords(x1, y1, &x, &y);
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

bool networkExist(String target_ssid) {
	int n = WiFi.scanNetworks();
	for (int i = 0; i < n; ++i) {
		String ssid = WiFi.SSID(i);
		if (ssid == target_ssid) {
			return true;
		}
	}
	return false;
}

void connectToHotspot(const char *ssid, const char *password) {
	int connection_indicating_led = D4;
	int connection_waiting_counter = 0;

	// setup wifi module to station mode
	Serial.print("Connecting to ");
	Serial.println(ssid);
	WiFi.mode(WIFI_STA);
	WiFi.begin(ssid, password);
	WiFi.setAutoReconnect(true);

	// led indication
	pinMode(connection_indicating_led, OUTPUT);

	int status = WL_IDLE_STATUS;
	for (int i = 0; i < 300; i++) {
	// serial report
	Serial.print(status);

	// led indication
	digitalWrite(connection_indicating_led,
		((connection_waiting_counter++ / 10) % 2) ? LOW  : HIGH);

	status = WiFi.status();
	if (status == WL_CONNECTED) {
		break;
	}

	delay(100);
	}

	// turn off led
	digitalWrite(connection_indicating_led, HIGH);

	if (status == WL_CONNECTED) {
		Serial.println("Connected to AP...");
		Serial.println(ssid);
		Serial.print("IP address: ");
		Serial.println(WiFi.localIP());
	} else {
		Serial.println("");
		switch (status) {
			case WL_IDLE_STATUS:
			Serial.println("status = WL_IDLE_STATUS");
			break;
			case WL_NO_SSID_AVAIL:
			Serial.println("status = WL_NO_SSID_AVAIL");
			break;
			case WL_CONNECTED:
			Serial.println("status = WL_CONNECTED");
			break;
			case WL_CONNECT_FAILED:
			Serial.println("status = WL_CONNECT_FAILED");
			break;
			case WL_WRONG_PASSWORD:
			Serial.println("status = WL_WRONG_PASSWORD");
			break;
			case WL_DISCONNECTED:
			Serial.println("status = WL_DISCONNECTED");
			break;
		}
		Serial.println("Restarting...");
		delay(3000);
		ESP.reset();
	}
}

int dBmToPercentage(int dBm) {
	const int RSSI_MAX =-50;
	const int RSSI_MIN =-100;
	int quality;
	if (dBm <= RSSI_MIN) {
		quality = 0;
	} else if (dBm >= RSSI_MAX) {
		quality = 100;
	} else {
		quality = 2 * (dBm + 100);
	}
	return quality;
}

int selectChannel() {
	int sum_of_signals[12];
	std::fill(std::begin(sum_of_signals), std::end(sum_of_signals), 0);

	int n = WiFi.scanNetworks();
	for (int i = 0; i < n; ++i) {
		sum_of_signals[WiFi.channel(i) - 1] += dBmToPercentage(WiFi.RSSI(i));

		Serial.print(i + 1);
		Serial.print(") ");
		Serial.print(WiFi.SSID(i));
		Serial.print(" ch:");
		Serial.print(WiFi.channel(i));
		Serial.print(" ");
		Serial.print(dBmToPercentage(WiFi.RSSI(i)));
		Serial.print("% )");
		Serial.print(" MAC:");
		Serial.println(WiFi.BSSIDstr(i));
		delay(10);
	}

	int channel_index = std::distance(
		std::begin(sum_of_signals),
		std::min_element(std::begin(sum_of_signals), std::end(sum_of_signals)));
	return channel_index + 1;
}

void setupHotspot(const char *ssid, const char *password) {
	Serial.println();

	WiFi.disconnect();

	Serial.println("Search for free channel...");
	const int free_channel = selectChannel();
	Serial.print("Selected channel: ");
	Serial.println(free_channel);

	WiFi.mode(WIFI_AP);

	Serial.print("Setting soft-AP ... ");
	Serial.println(WiFi.softAP(ssid, password, free_channel) ? "Ready" : "Failed!");

	Serial.print("Soft-AP IP address = ");
	Serial.println(WiFi.softAPIP());
}

struct BaseSettings {
	bool hostSpa;
	bool force_hotspot;
	const char *dns;
	const char *ssid;
	const char *password;
	const char *capabilities;
};

class Base {
 protected:
	int stations;
	int controlValue = CONTROL_VALUE_CLOCK;
	int value = 0;

	const BaseSettings *settings;

	void setCrossOrigin(AsyncWebServerResponse *response);
	void sendCrossOriginHeader(AsyncWebServerRequest *request);
	void control(AsyncWebServerRequest *request, JsonVariant &json);
	void capabilities(AsyncWebServerRequest *request, JsonVariant &json);
	void restServerRouting();

	AsyncWebServer server;

 public:
	Base(const BaseSettings *baseSettings);

	void Init();
	void Execute();
};

Base::Base(const BaseSettings *baseSettings)
	: stations{0}, server{80}, settings{baseSettings} {}

void Base::setCrossOrigin(AsyncWebServerResponse *response) {
	response->addHeader("Access-Control-Allow-Origin", "*");
	response->addHeader("Access-Control-Max-Age", "600");
	response->addHeader("Access-Control-Allow-Methods", "PUT,POST,GET,OPTIONS");
	response->addHeader("Access-Control-Allow-Headers", "*");
}

void Base::sendCrossOriginHeader(AsyncWebServerRequest * request) {
	AsyncWebServerResponse *response = request->beginResponse(204);
	response->addHeader("access-control-allow-credentials", "false");
	setCrossOrigin(response);
	request->send(response);
}

void Base::control(AsyncWebServerRequest * request, JsonVariant & json) {
	const JsonObject &postObj = json.as<JsonObject>();
	if (!postObj.containsKey("value")) {
		request->send(200);
	} else {
		AsyncWebServerResponse *response = request->beginResponse(200);
		setCrossOrigin(response);
		request->send(response);
		controlValue = postObj[F("control")];
		value = postObj[F("value")];

		Serial.print("controlValue=");
		Serial.print(controlValue);
		Serial.print(", value=");
		Serial.println(value);
	}
}

void Base::capabilities(AsyncWebServerRequest * request, JsonVariant & json) {
	DynamicJsonDocument doc(512);
	doc["capabilities"] = settings->capabilities;

	String buf;
	serializeJson(doc, buf);

	AsyncWebServerResponse *response = request->beginResponse(200, F("application/json"), buf);
	setCrossOrigin(response);
	request->send(response);
}

void Base::restServerRouting() {
	if (settings->hostSpa) {
		Serial.println("Hosting SPA");
		server.serveStatic("/", SPIFFS, "/").setDefaultFile("index.html");
	}

	auto optionsCallback = [this](AsyncWebServerRequest *request) {
		this->sendCrossOriginHeader(request);
	};

	server.addHandler(new AsyncCallbackJsonWebHandler("/control",
		[this](AsyncWebServerRequest *request, JsonVariant &json) {
			this->control(request, json);
		}));
	server.on("/control", HTTP_OPTIONS, optionsCallback);

	server.addHandler(new AsyncCallbackJsonWebHandler("/capabilities",
		[this](AsyncWebServerRequest *request, JsonVariant &json) {
			this->capabilities(request, json);
		}));
	server.on("/capabilities", HTTP_OPTIONS, optionsCallback);
}

void Base::Init() {
	Serial.begin(115200);

	if (settings->hostSpa) {
		// Initialize SPIFFS
		Serial.println("Initializing SPIFFS");
		if (!SPIFFS.begin()) {
			Serial.println("An Error has occurred while mounting SPIFFS");
			return;
		}
	}

	// Initialize Wifi
	if (settings->force_hotspot || !networkExist(settings->ssid)) {
		setupHotspot(settings->ssid, settings->password);
	} else {
		connectToHotspot(settings->ssid, settings->password);
	}

	// Initialize mDNS
	if (MDNS.begin(settings->dns, WiFi.localIP())) {
		Serial.println("MDNS responder started");
	}
	MDNS.addService("http", "tcp", 80);

	// Initialize HTTP Server
	restServerRouting();
	server.begin();
	Serial.println("HTTP server started");
}

void Base::Execute() {
	if (settings->force_hotspot){
		if (WiFi.softAPgetStationNum() != stations) {
			auto info_stations = WiFi.softAPgetStationNum();
			Serial.print("Total Connections: ");
			Serial.println(stations);
			Serial.println(info_stations);
			stations = info_stations;
		}
	}

	MDNS.update();
}

///////////////////////////////////////

#include "digits32.h"
#include "abcd32.h"

struct Countdown {
	int start_ms;
	int start_value;
	int brightness;
	bool running;
};

class P10x3 : public Base {
 private:
	StackedDMD dmd;
	StackedDMD buffer;

	int oldValue = -1;
	bool bufferSwapped = false;
	Countdown countdown;

	void updateDisplay();
	void drawDigits();
	void drawLetters();

 public:
	void redrawDisplay();

	void Init();
	void Execute();

	P10x3(BaseSettings *baseSettings);
};

void P10x3::drawDigits() {
	auto time = decodeTimeValue(value);
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

void P10x3::drawLetters() {
	buffer.fillScreen(false);
	if (value == 1) {
		buffer.drawChar(0, 0, 'A', GRAPHICS_ON, Abcd32);
		buffer.drawChar(24, 0, 'B', GRAPHICS_ON, Abcd32);
	} else if(value == 2) {
		buffer.drawChar(0, 0, 'C', GRAPHICS_ON, Abcd32);
		buffer.drawChar(24, 0, 'D', GRAPHICS_ON, Abcd32);
	}
}

void P10x3::updateDisplay() {
	if (controlValue == CONTROL_VALUE_LINES) {
		if (oldValue != value) {
			oldValue = value;
			drawLetters();
			bufferSwapped = false;
		}
	} else if (controlValue == CONTROL_VALUE_CLOCK) {
		if (oldValue != value) {
			oldValue = value;
			drawDigits();
			bufferSwapped = false;
		}
	} else if (controlValue == CONTROL_VALUE_COUNTDOWN) {
		if (oldValue != value) { // start countdown
			countdown = Countdown{millis(),
				decodeTimeValue(value),
				decodeBrightnessValue(value),
				decodeColorValue(value) > 0};
		}
		value = countdown.start_value;
		if (countdown.running) {
			value -= (millis() - countdown.start_ms) / 1000;
		}
		value = std::max(value, 0) | (countdown.brightness << 24);
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
}

void P10x3::Execute() {
	Base::Execute();
	updateDisplay();
	redrawDisplay();
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
 protected:
	PxMATRIX *display;
	Ticker displayTicker;
	int digitWidth;
	int digitHeight;
	int digitSize;
	int oldValue;
	const uint16_t *colors;
	int panelWidth;

protected:
	virtual void updateDisplay();
	virtual void drawDigits();
	virtual void drawLetters();

 public:
	void Init();
	void Execute();

	Px(BaseSettings *baseSettings, PxMATRIX *_display, int digitWidth,
		int _digitHeight, int _digitSize, const uint16_t *_colors,
		int _panelWidth);
};

Px::Px(BaseSettings *baseSettings, PxMATRIX *_display, int _digitWidth,
	int _digitHeight, int _digitSize, const uint16_t *_colors, int _panelWidth)
	: Base(baseSettings), display(_display), digitWidth(_digitWidth),
	  digitHeight(_digitHeight), digitSize(_digitSize), oldValue(-1),
	  colors(_colors), panelWidth(_panelWidth) {}

void Px::updateDisplay() {
	if (controlValue == CONTROL_VALUE_LINES) {
		if (oldValue != value) {
			oldValue = value;
			display->fillScreen(color565(0, 0, 0));
			drawLetters();
			display->showBuffer();
		}
	} else if (controlValue == CONTROL_VALUE_CLOCK || controlValue == CONTROL_VALUE_SEMAPHORE) {
		if (oldValue != value) {
			oldValue = value;
			display->fillScreen(color565(0, 0, 0));
			drawDigits();
			display->showBuffer();
		}
	}
}

void Px::drawDigits() {
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
}

void Px::drawLetters() {
	display->setBrightness(255); // TODO: use brightness encoded in value

	auto x_offset = (panelWidth - (2 * digitWidth)) / 2;

	if (value == 1) {
		display->drawChar(x_offset, digitHeight, 'A', colors[0], colors[0], digitSize);
		display->drawChar(x_offset + digitWidth, digitHeight, 'B', colors[0], colors[0], digitSize);
	} else if(value == 2) {
		display->drawChar(x_offset, digitHeight, 'C', colors[0], colors[0], digitSize);
		display->drawChar(x_offset + digitWidth, digitHeight, 'D', colors[0], colors[0], digitSize);
	}
}

void Px::Init() {
	Base::Init();

	// Initialize Display
	display->begin(8);

	display->clearDisplay();
	displayTicker.attach_ms(4, [this]() { this->display->display(); });
}

void Px::Execute() {
	Base::Execute();
	updateDisplay();
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
	  Px(baseSettings, &display, 11, 1, 2, P10_COLORS, 32) {}

//////////////////////////

#include "digits_font.h"
#include "abcd_font.h"

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
	scan_patterns scanPatterns;

	void drawDigits();
	void drawLetters();

 public:
	P5(BaseSettings *baseSettings, scan_patterns scanPatterns);
	virtual void Init();
};

void P5::drawDigits() {
	display.setFont(&FixedWidthDigit);
	Px::drawDigits();
}

void P5::drawLetters() {
	display.setFont(&FixedWidthAbcd);
	Px::drawLetters();
}

P5::P5(BaseSettings *baseSettings, scan_patterns scanPatterns=LINE)
	: display(64, 32, P5_LAT, P5_OE, P5_A, P5_B, P5_C, P5_D),
	  scanPatterns(scanPatterns),
	  Px(baseSettings, &display, 21, 32, 1, P5_COLORS, 64) {}

void P5::Init() {
	Px::Init();
	display.setScanPattern(scanPatterns);
	display.setFont(&FixedWidthDigit);
	display.setTextWrap(false);
}

//////////////////////////

class P10Semaphore : public P10 {
 public:
	P10Semaphore(BaseSettings *baseSettings);
 protected:
	void updateDisplay() override;
};

P10Semaphore::P10Semaphore(BaseSettings *baseSettings)
	: P10(baseSettings) {}

void P10Semaphore::updateDisplay() {
	if (controlValue == CONTROL_VALUE_SEMAPHORE) {
		if (oldValue != value) {
			oldValue = value;
			auto color = decodeColorValue(value);
			display.fillScreen(colors[color]);
			display.showBuffer();
		}
	}
}
