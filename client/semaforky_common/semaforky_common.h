/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#define double_buffer
#include <PxMatrix.h>
#include <Ticker.h>

#include "wifi.h"

#define INVERTED_WIRE_LOGIC
#include "saa1064_i2c.h"

const int SEMAPHORE_CLIENT = 1;
const int CLOCK_CLIENT = 2;
const int SIREN_CLIENT = 3;
const int RGB_MATRIX_DISPLAY_CLIENT = 4;

// Wemos D1 mini - common pins
#define POWER_ON_PIN 0
#define CONNECTED_PIN 4

// Ping magic number
#define PING_MAGIC_NUMBER 69

/// data chunk
struct ControlChunk {
  int control;
  int value;
  ControlChunk() : control(0), value(0) {
  }
};

struct RegisterChunk {
  int type;
};

/// Process object, state machine behavior
struct Process {
  WiFiClient client;
  String result;
  bool is_connected;
  ControlChunk last_chunk;

  /// connect to server specified by global variables
  void Connect();

  /// OnConnect event
  virtual void OnConnect();

  /// disconnect
  virtual void Disconnect();

  /// non blocking processing of states
  void Execute();

  /// init output pins
  virtual void Init();

  /// read data chunk
  ControlChunk ReadControlChunk();

  /// set outputs
  virtual void Output() = 0;

  Process();
};

Process::Process() :
  is_connected(false) {
}

ControlChunk Process::ReadControlChunk() {
  Serial.println("ReadControlChunk");
  ControlChunk received_chunk;
  client.read(reinterpret_cast<uint8_t*>(&received_chunk),
    sizeof(received_chunk));
  Serial.print("received_chunk.control=");
  Serial.println(received_chunk.control);
  Serial.print("received_chunk.value=");
  Serial.println(received_chunk.value);
  return received_chunk;
}

void Process::OnConnect() {
}

void Process::Connect() {
  // starts client connection, checks for connection
  if (client.connect(ESPWifiUtils::servername, ESPWifiUtils::port)) {
    Serial.println("connected");
    is_connected = true;
    OnConnect();
  } else {
    Serial.println("connection failed");  // error message if no client connect
    Serial.println();
    is_connected = false;
  }
}

void Process::Disconnect() {
  client.stop();  // stop client
  is_connected = false;
}

void Process::Execute() {
  if (!is_connected && !ESPWifiUtils::wifi_disabled) {
    Connect();
  } else {
    if (client.connected()) {
      if (client.available()) {
        ControlChunk new_chunk = ReadControlChunk();
        if (new_chunk.control == PING_MAGIC_NUMBER) {
          Serial.println("ping received");
          // send pong
          RegisterChunk chunk{PING_MAGIC_NUMBER};
          client.write(reinterpret_cast<uint8_t*>(&chunk),
            sizeof(RegisterChunk));
        } else {
          last_chunk = new_chunk;
        }
      }
    } else {
      Serial.println("disconnected");
      is_connected = false;
    }
  }
  Output();
  delay(100);
}

void Process::Init() {
  Serial.begin(115200);
  if (!ESPWifiUtils::wifi_disabled) {
    ESPWifiUtils::ConnectWiFiAP();
    ESPWifiUtils::PrintWiFiInfo();
  }
}

//-------------------------------------------------------------------------------
// Wemos D1 mini
#define SEMAPHORE_RED_PIN 5
#define SEMAPHORE_YELLOW_PIN 4
#define SEMAPHORE_GREEN_PIN 0

/// Semaphore client specialization.
struct SemaphoreProcess : public Process {
  virtual void Init();
  virtual void Output();
  virtual void OnConnect();
};

void SemaphoreProcess::OnConnect() {
  RegisterChunk chunk{SEMAPHORE_CLIENT};
  client.write(reinterpret_cast<uint8_t*>(&chunk), sizeof(RegisterChunk));
}

void SemaphoreProcess::Output() {
  digitalWrite(SEMAPHORE_RED_PIN, last_chunk.value == 1 ? HIGH : LOW);
  digitalWrite(SEMAPHORE_YELLOW_PIN, last_chunk.value == 2 ? HIGH : LOW);
  digitalWrite(SEMAPHORE_GREEN_PIN, last_chunk.value == 3 ? HIGH : LOW);
}

void SemaphoreProcess::Init() {
  Process::Init();
  pinMode(SEMAPHORE_RED_PIN, OUTPUT);
  pinMode(SEMAPHORE_YELLOW_PIN, OUTPUT);
  pinMode(SEMAPHORE_GREEN_PIN, OUTPUT);
}

//-------------------------------------------------------------------------------

// Wemos D1 mini
#define SAA1064_CLOCK_CLOCK_PIN 1
#define SAA1064_CLOCK_DATA_PIN 2

void ComputeDigits(int* digits, int digit_count, int value) {
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

/// Clock client specialization.
struct SAA1064ClockProcess : public Process {
  static const int digit_count = 4;
  SAA1064 saa1064;
  virtual void Init();
  virtual void Output();
  virtual void OnConnect();
  SAA1064ClockProcess();
};

SAA1064ClockProcess::SAA1064ClockProcess()
  : saa1064(100, SAA1064_CLOCK_CLOCK_PIN, SAA1064_CLOCK_DATA_PIN) {
}

void SAA1064ClockProcess::OnConnect() {
  RegisterChunk chunk{CLOCK_CLIENT};
  client.write(reinterpret_cast<uint8_t*>(&chunk), sizeof(RegisterChunk));
}

void SAA1064ClockProcess::Output() {
  digitalWrite(CONNECTED_PIN, is_connected ? HIGH : LOW);

  if (last_chunk.value == -1) {
    saa1064.digits(-2, -2, -2, -1);
  } else {
    int digits[4];
    ComputeDigits(digits, digit_count, last_chunk.value);
    saa1064.digits(digits[1], digits[2], digits[3], -1);
  }
}

void SAA1064ClockProcess::Init() {
  // setup common pins
  pinMode(CONNECTED_PIN, OUTPUT);
  pinMode(POWER_ON_PIN, OUTPUT);

  // power on
  digitalWrite(POWER_ON_PIN, HIGH);
  digitalWrite(CONNECTED_PIN, LOW);

  Process::Init();
}

//-------------------------------------------------------------------------------

#define SIREN_TONE_LENGTH 500
#define SIREN_PAUSE_LENGTH 500

// Wemos D1 mini
#define SIREN_AUDIO_PIN 5

/// Clock client specialization.
struct SirenProcess : public Process {
  virtual void Init();
  virtual void Output();
  virtual void OnConnect();
};

void SirenProcess::OnConnect() {
  RegisterChunk chunk{SIREN_CLIENT};
  client.write(reinterpret_cast<uint8_t*>(&chunk), sizeof(RegisterChunk));
}

void SirenProcess::Output() {
  for (int i = 0; i < last_chunk.value; i++) {
    digitalWrite(SIREN_AUDIO_PIN, HIGH);
    delay(SIREN_TONE_LENGTH);
    digitalWrite(SIREN_AUDIO_PIN, LOW);
    delay(SIREN_PAUSE_LENGTH);
  }
  last_chunk.value = 0;
}

void SirenProcess::Init() {
  Process::Init();
  pinMode(SIREN_AUDIO_PIN, OUTPUT);
}

//-------------------------------------------------------------------------------

#define P10_LAT 16
#define P10_A 5
#define P10_B 4
#define P10_C 15
#define P10_OE 2

static uint16_t color565(uint8_t r, uint8_t g, uint8_t b) {
  return ((b & 0xF8) << 8) | ((g & 0xFC) << 3) | (r >> 3);
}

const uint16_t P10_COLORS[] = {
  color565(255, 255, 255),  // white
  color565(255, 0, 0),      // red
  color565(0, 255, 0),      // green
  color565(255, 255, 0)     // yellow
};

struct RgbMatrixDisplayProcess : public Process {
  PxMATRIX display;
  Ticker display_ticker;
  Ticker redraw_ticker;
  int brightness;
  int color;
  int time;
  int start;
  RgbMatrixDisplayProcess();
  virtual void Init();
  virtual void Output();
  virtual void OnConnect();
  void DrawScreen();
};

int DecodeTimeValue(int value) {
  return value & 0x0000ffff;
}

int DecodeColorValue(int value) {
  return (value & 0x00ff0000) >> 16;
}

int DecodeBrightnessValue(int value) {
  return (value & 0xff000000) >> 24;
}

RgbMatrixDisplayProcess::RgbMatrixDisplayProcess()
  : display(32, 16, P10_LAT, P10_OE, P10_A, P10_B, P10_C) {
}

void RgbMatrixDisplayProcess::OnConnect() {
  RegisterChunk chunk{RGB_MATRIX_DISPLAY_CLIENT};
  client.write(reinterpret_cast<uint8_t*>(&chunk), sizeof(RegisterChunk));
}

void RgbMatrixDisplayProcess::Output() {
  time = DecodeTimeValue(last_chunk.value);
  color = DecodeColorValue(last_chunk.value);
  brightness = DecodeBrightnessValue(last_chunk.value);
  start = micros();
}

void RgbMatrixDisplayProcess::DrawScreen(int time, int brightness, int color) {
  display.fillScreen(color565(0, 0, 0));
  display.setBrightness(brightness);
  int digits[4];
  ComputeDigits(digits, 4, time);
  for (int i = 3; i > 0; i--) {
    if (digits[i] >= 0) {
      display.drawChar((i - 1) * 11, 1,  // coords
        '0' + digits[i],  // character
        P10_COLORS[color], P10_COLORS[color],  // colors
        2);  // size
    }
  }
  display.showBuffer();
}

void RgbMatrixDisplayProcess::Init() {
  Process::Init();
  Serial.begin(9600);
  display.begin(8);
  display.clearDisplay();
  display_ticker.attach_ms(4, [this]() {
    this->display.display();
  });
  redraw_ticker.attach_ms(100, [this]() {
    DrawScreen(time - (micros() - start) / 1e6 , brightness, color)
  });
}
