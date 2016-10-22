/// Copyright (C) 2016, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include <ESP8266WiFi.h>
#include <SPI.h>

// connection properties
const char* ssid = "semaforky";
const char* password = "semaforky";
const char servername[] = "192.168.43.1";
const int port = 8888;

const int SEMAPHORE_CLIENT = 1;
const int CLOCK_CLIENT = 2;

bool wifi_disabled = false;

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
  void Disconnect();

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
  if (client.connect(servername, port)) {  //starts client connection, checks for connection
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
  if (!is_connected && !wifi_disabled) {
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
}

void Process::Init() {
}

/// Semaphore client specialization.
struct SemaphoreProcess : public Process {
  virtual void Init();
  virtual void SetLights();
  virtual void OnConnect();
};

void SemaphoreProcess::OnConnect() {
  RegisterChunk chunk{1};
  client.write((uint8_t*)(&chunk), sizeof(RegisterChunk));
}

void SemaphoreProcess::SetLights() {
  digitalWrite(14, last_chunk.status == 1 ? HIGH : LOW);
  digitalWrite(12, last_chunk.status == 2 ? HIGH : LOW);
  digitalWrite(13, last_chunk.status == 3 ? HIGH : LOW);
}

void SemaphoreProcess::Init() {
  pinMode(14, OUTPUT);
  pinMode(12, OUTPUT);
  pinMode(13, OUTPUT);
}

/// Clock client specialization.
struct ClockProcess : public Process {
  const static int digit_count = 4;
  const static int segment_count = 7;
  int segment_pin[7] = {2,4,5,12,13,14,15};
  int digit_pin[digit_count] = {3, 1, 16, 0};
  bool digit_configuration[11][7] = {
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
    {false, false, false, false, false, false, false} //off
  };
  virtual void Init();
  virtual void SetLights();
  virtual void OnConnect();
};

void ClockProcess::OnConnect() {
  RegisterChunk chunk{2};
  client.write((uint8_t*)(&chunk), sizeof(RegisterChunk));
}

void ComputeDigits(int* digits, int digit_count, int value) {
  int divider = 1;
  for (int i = 0; i < digit_count; i++) {
    digits[digit_count - i - 1] = (value / divider) % 10;
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
  for (int i = 0; i < segment_count; i++) {
    pinMode(segment_pin[i], OUTPUT);
  }
  for (int i = 0; i < digit_count; i++) {
    pinMode(digit_pin[i], OUTPUT);
  }
}

void PrintWiFiInfo() {
  Serial.println("Connected!");
  Serial.print("localIP: ");
  Serial.println(WiFi.localIP().toString());
  Serial.print("gatewayIP: ");
  Serial.println(WiFi.gatewayIP().toString());
  Serial.print("macAddress: ");
  Serial.println(WiFi.macAddress());
}

void ConnectWiFiAP() {
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(50);
    Serial.print(".");
  }
  Serial.println("");
}

/// global instance
//Process global;
//SemaphoreProcess global;
ClockProcess global;

void setup() {
  Serial.begin(115200);

  global.Init();

  if (!wifi_disabled) {
    ConnectWiFiAP();
    PrintWiFiInfo();
  }
}

void loop() {
  //delay(500);
  global.Execute();
}



