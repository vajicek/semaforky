/// Copyright (C) 2026, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#define CLOCK_EOPIN 22
#define CLOCK_CLKPIN 18
#define CLOCK_LATPIN 2
#define CLOCK_APIN 19
#define CLOCK_BPIN 21
#define CLOCK_RDATAPIN 23

#define CONNECTION_INDICATING_LED 17

#define SIREN_TONE_LENGTH 300
#define SIREN_PAUSE_LENGTH 300
#define SIREN_AUDIO_PIN 5

#define SEMAPHORE_RED_PIN 25
#define SEMAPHORE_GREEN_PIN 26
#define SEMAPHORE_ORANGE_PIN 27

#include "semaforky_webui_common.h"

#include "digits32_font.h"
#include "abcd32_font.h"

class Semaphore {
private:
	uint8_t pins[3];
	int state = 0;

public:
	Semaphore(uint8_t redPin, uint8_t greenPin, uint8_t orangePin)
		: pins{redPin, greenPin, orangePin} {}

	void init() {
		for (uint8_t pin : pins) {
			pinMode(pin, OUTPUT);
		}
		state = -1;
		execute();
	}

	void setColor(int nextState){
		if (nextState < 0 || nextState > 3) {
			nextState = 0;
		}
		if (state == nextState) {
			return;
		}

		state = nextState;
	}

	void execute() {
		for (int i = 0; i < 3; i++) {
			digitalWrite(pins[i], state == i + 1 ? HIGH : LOW);
		}
	}
};

class Siren {
private:
	enum class Phase {
		IDLE,
		TONE,
		PAUSE,
	};

	uint8_t audioPin;
	Phase phase = Phase::IDLE;
	unsigned long phaseStart = 0;
	int pulsesRemaining = 0;

public:
	Siren(uint8_t audioPin) : audioPin(audioPin) {}

	void init() {
		pinMode(audioPin, OUTPUT);
		digitalWrite(audioPin, LOW);
	}

	void start(int pulseCount) {
		if (phase != Phase::IDLE || pulseCount <= 0) {
			return;
		}

		pulsesRemaining = pulseCount;
		phase = Phase::TONE;
		phaseStart = millis();
		digitalWrite(audioPin, HIGH);
	}

	void execute() {
		if (phase == Phase::IDLE) {
			return;
		}

		const unsigned long now = millis();
		const unsigned long phaseLength = phase == Phase::TONE
			? SIREN_TONE_LENGTH
			: SIREN_PAUSE_LENGTH;
		if (now - phaseStart < phaseLength) {
			return;
		}

		phaseStart = now;

		if (phase == Phase::TONE) {
			// The tone finished; switch to the silent part of this pulse.
			phase = Phase::PAUSE;
			digitalWrite(audioPin, LOW);
			return;
		}

		// The pause finished, so the current pulse is complete.
		pulsesRemaining--;
		if (pulsesRemaining == 0) {
			// All requested pulses have completed.
			phase = Phase::IDLE;
			return;
		}

		// More pulses remain; start the next tone.
		phase = Phase::TONE;
		digitalWrite(audioPin, HIGH);
	}
};

class P10x3 : public Base {
private:
	FthnLabsDisplay display;
	GFXcanvas1 canvas;
	TaskHandle_t displayTaskHandle = NULL;
	Semaphore semaphore;
	Siren siren;

	int oldValue = -1;
	String oldDisplayValue;

	void updateDisplay();
	void drawDigits();
	void drawLetters();
	void drawRgbImage();
	void copyCanvas();
	void executeSiren();
	void executeSemaphore();

public:
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

void P10x3::drawRgbImage() {
	int colon = displayValue.indexOf(':');
	int xSep = displayValue.indexOf('x');
	if (colon < 0 || xSep < 0 || xSep >= colon) {
		return;
	}

	int width = displayValue.substring(0, xSep).toInt();
	int height = displayValue.substring(xSep + 1, colon).toInt();
	if (width <= 0 || height <= 0) {
		return;
	}

	size_t rgbLen = (size_t)width * (size_t)height * 3;
	uint8_t *rgb = (uint8_t *)malloc(rgbLen);
	if (!rgb) {
		return;
	}
	memset(rgb, 0, rgbLen);

	const char *b64 = displayValue.c_str() + colon + 1;
	size_t b64Len = displayValue.length() - (size_t)colon - 1;
	decodeBase64(b64, b64Len, rgb, rgbLen);

	for (int16_t y = 0; y < 32; y++) {
		for (int16_t x = 0; x < 48; x++) {
			uint16_t colorToDraw = 0;
			if (x < width && y < height) {
				size_t i = ((size_t)y * (size_t)width + (size_t)x) * 3;
				colorToDraw = color565(rgb[i], rgb[i + 1], rgb[i + 2]);
			}
			display.drawPixel(y, 48 - x, colorToDraw);
		}
	}
	free(rgb);
	display.display();
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
	} else if (controlValue == CONTROL_VALUE_DISPLAY) {
		if (oldDisplayValue != displayValue) {
			oldDisplayValue = displayValue;
			drawRgbImage();
		}
	}
}

void displayUpdateLoop(void *pvParameters) {
	for (;;) {
		((P10x3 *)pvParameters)->getDisplay()->loop();
		vTaskDelay(1 / portTICK_PERIOD_MS);
	}
}

void P10x3::Init() {
	Base::Init();
	semaphore.init();
	siren.init();
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

void P10x3::executeSemaphore() {
	if (controlValue == CONTROL_VALUE_SEMAPHORE) {
		semaphore.setColor(decodeColorValue(value));
	}
	semaphore.execute();
}

void P10x3::executeSiren() {
	if (controlValue == CONTROL_VALUE_SIRENE) {
		controlValue = 0;
		siren.start(value);
	}
	siren.execute();
}

void P10x3::Execute() {
	Base::Execute();
	executeSemaphore();
	executeSiren();
	updateDisplay();
}

DisplayConfig getP10x3DisplayConfig() {
	uint8_t oePin = CLOCK_EOPIN;
	uint8_t clkPin = CLOCK_CLKPIN;
	uint8_t latPin = CLOCK_LATPIN;
	uint8_t aPin = CLOCK_APIN;
	uint8_t bPin = CLOCK_BPIN;
	uint8_t rDataPin = CLOCK_RDATAPIN;

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
	: Base(baseSettings), display(getP10x3DisplayConfig()), canvas(48, 32),
	  semaphore(SEMAPHORE_RED_PIN, SEMAPHORE_GREEN_PIN, SEMAPHORE_ORANGE_PIN),
	  siren(SIREN_AUDIO_PIN) {}
