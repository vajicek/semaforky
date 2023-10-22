/// Copyright (C) 2017, Vajsoft
/// Author: Vaclav Krajicek <vajicek@volny.cz>

#include <ESP8266WiFi.h>

struct ESPWifiUtils {
  static void PrintWiFiInfo();
  static void ConnectWiFiAP();
  static constexpr char* ssid = "semaforky";
  static constexpr char* password = "semaforky";
  static constexpr char* servername = "192.168.43.1";

  static constexpr int port = 8888;
  static constexpr bool wifi_disabled = false;

  static constexpr int connection_indicating_led = D4;
  static int connection_waiting_counter;
};

int ESPWifiUtils::connection_waiting_counter = 0;

void ESPWifiUtils::PrintWiFiInfo() {
  Serial.println("Connected!");
  Serial.print("localIP: ");
  Serial.println(WiFi.localIP().toString());
  Serial.print("gatewayIP: ");
  Serial.println(WiFi.gatewayIP().toString());
  Serial.print("macAddress: ");
  Serial.println(WiFi.macAddress());
  Serial.print("connection_waiting_counter: ");
  Serial.println(connection_waiting_counter);
}

void ESPWifiUtils::ConnectWiFiAP() {
  // setup wifi module to station mode
  WiFi.mode(WIFI_STA);
  int status = WL_IDLE_STATUS;

  // led indication
  pinMode(connection_indicating_led, OUTPUT);

  while (status != WL_CONNECTED) {
    if (connection_waiting_counter % 400 == 0) {
      Serial.print("Connecting to ");
      Serial.println(ESPWifiUtils::ssid);
      WiFi.begin(ESPWifiUtils::ssid, ESPWifiUtils::password);
    }

    delay(50);

    // led indication
    digitalWrite(connection_indicating_led,
      ((connection_waiting_counter++ / 10) % 2) ? LOW  : HIGH);

    // serial report
    Serial.print(".");

    status = WiFi.status();
  }

  // turn off led
  digitalWrite(connection_indicating_led, HIGH);

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("Connected to AP...");
  } else {
    Serial.println("Restarting...");
    delay(3000);
    ESP.reset();
  }
}
