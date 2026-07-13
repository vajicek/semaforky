/// Copyright (C) 2026, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include <Adafruit_GFX.h>
#include <ArduinoJson.h>
#include <AsyncJson.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>
#include <ESPmDNS.h>
#include <FS.h>
#include <FthnLabsDisplay.h>
#include <SPIFFS.h>
#include <WiFi.h>

#define CONTROL_VALUE_CLOCK 1
#define CONTROL_VALUE_SEMAPHORE 2
#define CONTROL_VALUE_LINES 3
#define CONTROL_VALUE_SIRENE 4
#define CONTROL_VALUE_COUNTDOWN 5

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
	WiFi.persistent(true);

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
			case WL_DISCONNECTED:
				Serial.println("status = WL_DISCONNECTED");
			break;
		}
		Serial.println("Restarting...");
		delay(3000);
		//ESP.reset();
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
	bool hotspot = false;

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
		hotspot = true;
	} else {
		connectToHotspot(settings->ssid, settings->password);
	}

	// Initialize mDNS
	if (MDNS.begin(settings->dns/*, WiFi.localIP()*/)) {
		Serial.println("MDNS responder started");
	}
	MDNS.addService("http", "tcp", 80);

	// Initialize HTTP Server
	restServerRouting();
	server.begin();
	Serial.println("HTTP server started");
}

void Base::Execute() {
	if (this->hotspot) {
		if (WiFi.softAPgetStationNum() != stations) {
			auto info_stations = WiFi.softAPgetStationNum();
			Serial.print("Total Connections: ");
			Serial.println(stations);
			Serial.println(info_stations);
			stations = info_stations;
		}
	}

	//MDNS.update();
}

#include "digits32_font.h"
#include "abcd32_font.h"

class P10x3 : public Base {
private:
	FthnLabsDisplay display;
	GFXcanvas1 canvas;
	TaskHandle_t displayTaskHandle = NULL;

	int oldValue = -1;

	void updateDisplay();
	void drawDigits();
	void drawLetters();
	void copyCanvas();

public:
	void redrawDisplay();

	void Init();
	void Execute();

	FthnLabsDisplay* getDisplay();

	P10x3(BaseSettings *baseSettings);
};

void P10x3::drawDigits() {
	auto time = decodeTimeValue(value);
	auto brightness = decodeBrightnessValue(value);

	canvas.fillScreen(0);
	canvas.setFont(&FixedWidthDigit);

	int digits[4];
	computeDigits(digits, 4, time);
	for (int i = 3; i > 0; i--) {
		if (digits[i] >= 0) {
			canvas.drawChar(2 + (i - 1) * 16, 32, '0' + digits[i], 0xFFFF, 0, 1);
		}
	}

	display.setBrightness(brightness);
	copyCanvas();
	display.display();
}

void P10x3::drawLetters() {
	canvas.fillScreen(0);
	canvas.setFont(&FixedWidthAbcd);

	if (value == 1) {
		canvas.drawChar(0, 32, 'A', 0xFFFF, 0, 1);
		canvas.drawChar(24, 32, 'B', 0xFFFF, 0, 1);
	} else if (value == 2) {
		canvas.drawChar(0, 32, 'C', 0xFFFF, 0, 1);
		canvas.drawChar(24, 32, 'D', 0xFFFF, 0, 1);
	}

	copyCanvas();
	display.display();
}

void P10x3::copyCanvas() {
	for (int16_t y = 0; y < 32; y++) {
		for (int16_t x = 0; x < 48; x++) {
			bool pixelOn = canvas.getPixel(x, y);
			uint16_t colorToDraw = pixelOn ? 0xFFFF : 0x0;
			display.drawPixel(y, 48 - x, colorToDraw);
		}
	}
}

void P10x3::updateDisplay() {
	if (controlValue == CONTROL_VALUE_LINES) {
		if (oldValue != value) {
			oldValue = value;
			drawLetters();
		}
	} else if (controlValue == CONTROL_VALUE_CLOCK) {
		if (oldValue != value) {
			oldValue = value;
			drawDigits();
		}
	}
}

void P10x3::redrawDisplay() {
}

void displayUpdateLoop(void *pvParameters) {
	for (;;) {
		((P10x3 *)pvParameters)->getDisplay()->loop();
		vTaskDelay(1 / portTICK_PERIOD_MS);
	}
}

void P10x3::Init() {
	Base::Init();
	display.begin();

	// Create the display task on Core 1
	xTaskCreatePinnedToCore(
		displayUpdateLoop,  // Function to implement the task
		"DisplayTask",      // Name of the task
		4096,               // Stack size in words
		this,               // Task input parameter
		1,                  // Priority of the task
		&displayTaskHandle, // Task handle
		1                   // Core where the task should run (Core 1)
	);
}

void P10x3::Execute() {
	Base::Execute();
	updateDisplay();
	redrawDisplay();
}

DisplayConfig getP10x3DisplayConfig() {
	uint8_t oePin = 22;
	uint8_t clkPin = 18;
	uint8_t latPin = 2;
	uint8_t aPin = 19;
	uint8_t bPin = 21;
	uint8_t rDataPin = 23;

	uint8_t panelWidth = 32;
	uint8_t panelHeight = 48;

	// address pins
	static uint8_t addressPins[] = {aPin, bPin};

	// data pins
	static uint8_t dataPins[] = {rDataPin};

	DisplayConfig cfg = {
		panelWidth, panelHeight, // panel
		oePin, clkPin, latPin,   // control
		addressPins, 2,          // address
		dataPins, 1              // data
	};
	return cfg;
}

FthnLabsDisplay* P10x3::getDisplay() {
	return &display;
}

P10x3::P10x3(BaseSettings *baseSettings)
	: Base(baseSettings), display(getP10x3DisplayConfig()), canvas(48, 32) {}
