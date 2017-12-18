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
};

void ESPWifiUtils::PrintWiFiInfo() {
  Serial.println("Connected!");
  Serial.print("localIP: ");
  Serial.println(WiFi.localIP().toString());
  Serial.print("gatewayIP: ");
  Serial.println(WiFi.gatewayIP().toString());
  Serial.print("macAddress: ");
  Serial.println(WiFi.macAddress());
}

void ESPWifiUtils::ConnectWiFiAP() {
  Serial.print("Connecting to ");
  Serial.println(ESPWifiUtils::ssid);
  WiFi.begin(ESPWifiUtils::ssid, ESPWifiUtils::password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(50);
    Serial.print(".");
  }
  Serial.println("");
}
