/// Copyright (C) 2023, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#define CONNECTION_INDICATING_LED D4

#include "semaforky_webui_common.h"

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

//////////////////////////

#include "abcd32.h"
#include "digits32.h"

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

#define SIREN_TONE_LENGTH 300
#define SIREN_PAUSE_LENGTH 300

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
	displayTicker.attach_ms(2, [this]() { this->display->display(); });
	display->setFastUpdate(true);
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
#include "time_font.h"

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

	int brightness = 255;

	JsonString displayValue;
	bool displayValueChanged;

	int hours = 0;
	int minutes = 0;
	bool timeChanged = false;

	void drawDisplay();
	void drawTime();

	void drawDigits();
	void drawLetters();
	void updateDisplay() override;
	void control(AsyncWebServerRequest * request, JsonVariant & json) override;

 public:
	P5(BaseSettings *baseSettings, scan_patterns scanPatterns);
	virtual void Init();
};

void P5::drawDisplay() {
	if (displayValue.isNull()) {
		return;
	}

	const char *rawDisplayValue = displayValue.c_str();
	size_t rawDisplayValueSize = displayValue.size();

	if (rawDisplayValue == nullptr) {
		return;
	}

	const char *separator = strchr(rawDisplayValue, 'x');
	const char *colon = strchr(rawDisplayValue, ':');
	if (separator == rawDisplayValue || separator == nullptr || colon == nullptr || colon <= separator) {
		return;
	}

	int width = atoi(rawDisplayValue);
	int height = atoi(separator + 1);
	if (width <= 0 || width > 64 || height <= 0 || height > 32) {
		return;
	}
	const char *encoded = colon + 1;

	display.setBrightness(10);

	Serial.println("Start decoding");

	size_t pixelIndex = 0;
	uint8_t pixel[3];
	size_t pixelBytes = 0;
	uint32_t acc = 0;
	int bits = 0;
	for (size_t i = 0; pixelIndex < width * height; i++) {

		if (i %100 == 0) {
			ESP.wdtFeed();
		}

		char c = encoded[i];
		if (c == '=' || c == '\n' || c == '\r') {
			continue;
		}
		int8_t v = decodeBase64Char(c);
		if (v < 0) {
			continue;
		}
		acc = (acc << 6) | (uint32_t)v;
		bits += 6;
		if (bits >= 8) {
			bits -= 8;
			pixel[pixelBytes++] = (acc >> bits) & 0xFF;
			if (pixelBytes == 3) {
				int x = pixelIndex % width;
				int y = pixelIndex / width;
				if (y < 32) {
					display.drawPixel(x, y, color565(pixel[0], pixel[1], pixel[2]));
				}

				pixelBytes = 0;
				pixelIndex++;
			}
		}
	}

	Serial.println("Decoding finished");
	display.showBuffer();
}

void P5::control(AsyncWebServerRequest * request, JsonVariant & json) {
	const JsonObject &postObj = json.as<JsonObject>();
	int requestedControl = postObj[F("control")].as<int>();
	if (requestedControl == CONTROL_VALUE_TIME || postObj.containsKey("value")) {
		controlValue = postObj[F("control")];

		if (controlValue == CONTROL_VALUE_DISPLAY) {
			displayValue = postObj[F("value")].as<JsonString>();
			displayValueChanged = true;
		} else if (controlValue == CONTROL_VALUE_TIME) {
			hours = postObj[F("hours")].as<int>();
			minutes = postObj[F("minutes")].as<int>();
			brightness = postObj[F("brightness")].as<int>();
			if (hours >= 0 && hours < 24 && minutes >= 0 && minutes < 60) {
				timeChanged = true;
			}
		}
	}
	Base::control(request, json);
}

void P5::drawDigits() {
	display.setFont(&FixedWidthDigit);
	Px::drawDigits();
}

void P5::drawLetters() {
	display.setFont(&FixedWidthAbcd);
	Px::drawLetters();
}

void P5::drawTime() {
	display.setFont(&TimeFont);
	display.fillScreen(color565(0, 0, 0));
	display.setBrightness(brightness);

	char timeText[] = {
		(char)('0' + hours / 10),
		(char)('0' + hours % 10),
		':',
		(char)('0' + minutes / 10),
		(char)('0' + minutes % 10),
		'\0'
	};
	int16_t x1;
	int16_t y1;
	uint16_t textWidth;
	uint16_t textHeight;
	display.setTextSize(1);
	display.getTextBounds(timeText, 0, 0, &x1, &y1, &textWidth, &textHeight);

	display.setCursor(0, // zero is better then (64 - textWidth) / 2 - x1,
		(32 - textHeight) / 2 - y1);
	display.setTextColor(P5_COLORS[0]);
	display.print(timeText);
	display.showBuffer();
}

void P5::updateDisplay() {
	if (controlValue == CONTROL_VALUE_TIME) {
		if (timeChanged) {
			drawTime();
			timeChanged = false;
		}
		return;
	} else if (controlValue == CONTROL_VALUE_DISPLAY) {
		if (displayValueChanged) {
			drawDisplay();
			displayValueChanged = false;
		}
		return;
	}
	Px::updateDisplay();
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
