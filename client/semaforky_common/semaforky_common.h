/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include "wifi.h"

#define INVERTED_WIRE_LOGIC
#include "saa1064_i2c.h"

const int SEMAPHORE_CLIENT = 1;
const int CLOCK_CLIENT = 2;
const int SIREN_CLIENT = 3;

// Wemos D1 mini - common pins
#define POWER_ON_PIN 0
#define CONNECTED_PIN 4

/// data chunk
struct ControlChunk {
  int light;
  int status;
  ControlChunk() : light(0), status(0) {
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
  void ReadControlChunk();

  /// set outputs
  virtual void SetLights();

  Process();
};

Process::Process() :
  is_connected(false) {
}

void Process::ReadControlChunk() {
  Serial.println("ReadControlChunk");
  client.read((uint8_t*)(&last_chunk), sizeof(last_chunk));
  Serial.print("last_chunk.light=");
  Serial.println(last_chunk.light);
  Serial.print("last_chunk.status=");
  Serial.println(last_chunk.status);
}

void Process::OnConnect() {
}

void Process::Connect() {
  if (client.connect(ESPWifiUtils::servername, ESPWifiUtils::port)) {  //starts client connection, checks for connection
    Serial.println("connected");
    is_connected = true;
    OnConnect();
  } else {
    Serial.println("connection failed"); //error message if no client connect
    Serial.println();
    is_connected = false;
  }
}

void Process::Disconnect() {
  client.stop(); //stop client
  is_connected = false;
}

void Process::SetLights() {
}

void Process::Execute() {
  if (!is_connected && !ESPWifiUtils::wifi_disabled) {
    Connect();
  } else  {
    if (client.connected()) {
      if (client.available()) {
        ReadControlChunk();
      }
    } else {
      Serial.println("disconnected");
      is_connected = false;
    }
  }
  SetLights();
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
  virtual void SetLights();
  virtual void OnConnect();
};

void SemaphoreProcess::OnConnect() {
  RegisterChunk chunk{SEMAPHORE_CLIENT};
  client.write((uint8_t*)(&chunk), sizeof(RegisterChunk));
}

void SemaphoreProcess::SetLights() {
  digitalWrite(SEMAPHORE_RED_PIN, last_chunk.status == 1 ? HIGH : LOW);
  digitalWrite(SEMAPHORE_YELLOW_PIN, last_chunk.status == 2 ? HIGH : LOW);
  digitalWrite(SEMAPHORE_GREEN_PIN, last_chunk.status == 3 ? HIGH : LOW);
}

void SemaphoreProcess::Init() {
  Process::Init();
  pinMode(SEMAPHORE_RED_PIN, OUTPUT);
  pinMode(SEMAPHORE_YELLOW_PIN, OUTPUT);
  pinMode(SEMAPHORE_GREEN_PIN, OUTPUT);
}

//-------------------------------------------------------------------------------

/// Clock client specialization.
struct ClockProcess : public Process {
  const static int digit_count = 4;
  const static int segment_count = 7;
  int segment_pin[7] = {2, 4, 5, 12, 13, 14, 15};
  int digit_pin[digit_count] = {3, 1, 16, 0};
  bool digit_configuration[12][7] = {
    {true, true, true, true, true, true, false},      //0
    {false, true, true, false, false, false, false},  //1
    {true, true, false, true, true, false, true},
    {true, true, true, true, false, false, true},     //3
    {false, true, true, false, false, true, true},    //4
    {true, false, true, true, false, true, true},
    {true, false, true, true, true, true, true},      //6
    {true, true, true, false, false, false, false},   //7
    {true, true, true, true, true, true, true},       //8
    {true, true, true, true, false, true, true},      //9
    {false, false, false, false, false, false, false},//off
    {false, false, false, false, false, false, true}  //-
  };
  virtual void Init();
  virtual void SetLights();
  virtual void OnConnect();
};

void ClockProcess::OnConnect() {
  RegisterChunk chunk{CLOCK_CLIENT};
  client.write((uint8_t*)(&chunk), sizeof(RegisterChunk));
}

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

void ClockProcess::SetLights() {
  int digits[4];
  ComputeDigits(digits, digit_count, last_chunk.status);
  for (int i = 0; i < digit_count; i++) {
    digitalWrite(digit_pin[i], LOW);
    for (int j = 0; j < segment_count; j++) {
       digitalWrite(segment_pin[j], digit_configuration[digits[i]][j] ? HIGH : LOW);
    }
    delay(4);
    digitalWrite(digit_pin[i], HIGH);
  }
}

void ClockProcess::Init() {
  Process::Init();
  for (int i = 0; i < segment_count; i++) {
    pinMode(segment_pin[i], OUTPUT);
  }
  for (int i = 0; i < digit_count; i++) {
    pinMode(digit_pin[i], OUTPUT);
  }
}

//-------------------------------------------------------------------------------

// Wemos D1 mini
#define SAA1064_CLOCK_CLOCK_PIN 1
#define SAA1064_CLOCK_DATA_PIN 2

/// Clock client specialization.
struct SAA1064ClockProcess : public Process {
  const static int digit_count = 4;
  SAA1064 saa1064;
  virtual void Init();
  virtual void SetLights();
  virtual void OnConnect();
  SAA1064ClockProcess();
};

SAA1064ClockProcess::SAA1064ClockProcess() : saa1064 (100, SAA1064_CLOCK_CLOCK_PIN, SAA1064_CLOCK_DATA_PIN) {
}

void SAA1064ClockProcess::OnConnect() {
  RegisterChunk chunk{CLOCK_CLIENT};
  client.write((uint8_t*)(&chunk), sizeof(RegisterChunk));
}

void SAA1064ClockProcess::SetLights() {
  digitalWrite(CONNECTED_PIN, is_connected ? HIGH : LOW);

  if (last_chunk.status == -1) {
    saa1064.digits(-2, -2, -2, -1);
  } else {
    int digits[4];
    ComputeDigits(digits, digit_count, last_chunk.status);
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
  virtual void SetLights();
  virtual void OnConnect();
  SirenProcess();
};

SirenProcess::SirenProcess() {
}

void SirenProcess::OnConnect() {
  RegisterChunk chunk{SIREN_CLIENT};
  client.write((uint8_t*)(&chunk), sizeof(RegisterChunk));
}

void SirenProcess::SetLights() {
  for (int i = 0; i < last_chunk.status; i++) {
    digitalWrite(SIREN_AUDIO_PIN, HIGH);
    delay(SIREN_TONE_LENGTH);
    digitalWrite(SIREN_AUDIO_PIN, LOW);
    delay(SIREN_PAUSE_LENGTH);
  }
  last_chunk.status = 0;
}

void SirenProcess::Init() {
  Process::Init();
  pinMode(SIREN_AUDIO_PIN, OUTPUT);
}
