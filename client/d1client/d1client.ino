/// Copyright (C) 2016, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include <ESP8266WiFi.h>
#include <SPI.h>

// connection properties
const char* ssid = "vajnet";
const char* password = "pocernicka57";
const char servername[] = "192.168.1.11";
//const char servername[] = "10.0.2.15";
const int port = 8888;

/// data chunk
struct ControlChunk {
  int light;
  int status;
};

/// Process object, state machine behavior
struct Process {
	WiFiClient client;
	String result;
  bool is_connected;
  bool light_enabled;
  ControlChunk last_chunk;

  /// connect to server specified by global variables
  void Connect();

  /// disconnect
  void Disconnect();

  /// non blocking processing of states
  void Execute();

  /// read data chunk
  void ReadControlChunk();

  void SetLights();

  Process();
};

Process::Process() : 
  is_connected(false), light_enabled(false) {
}

void Process::ReadControlChunk() {
  Serial.println("ReadControlChunk");
  client.read((uint8_t*)(&last_chunk), sizeof(last_chunk));
  //Serial.println(client.read());
  Serial.print("last_chunk.light=");
  Serial.println(last_chunk.light);
  Serial.print("last_chunk.status=");
  Serial.println(last_chunk.status);
}

void Process::Connect() {
  if (client.connect(servername, port)) {  //starts client connection, checks for connection
    Serial.println("connected");
    is_connected = true;
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
  if (light_enabled) {
    digitalWrite(2, HIGH);
  } else {
    digitalWrite(2, LOW);
  }
}
  
void Process::Execute() {
  if (!is_connected) {
    Connect();
  } else  {
    if (client.connected()) {
      if (client.available()) {
        ReadControlChunk();
        light_enabled = !light_enabled;
      }
    } else {
      Serial.println("disconnected");
      is_connected = false;
    }
  }
  SetLights();
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
    delay(500);
    Serial.print(".");
  }
  Serial.println("");
}

void setup() {
  Serial.begin(115200);

  pinMode(2, OUTPUT);
  
  ConnectWiFiAP();
  PrintWiFiInfo();
}

/// global instance
Process global;

void loop() {
  delay(1000); // for debug
  global.Execute();
}



