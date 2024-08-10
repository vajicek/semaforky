# semaforky
Android/Arduino-based DIY system for remote-controlled signaling for sport events

**NEW!: The next-gen version is based on a web app hosted by the device on its own Wi-Fi network. See [web_app](web_app).**

Semaforky is a client-server software for Android/web browsers and several Arduino-based controllers (clients) of various signaling devices:
- Semaphore lights (red, yellow, green)
- Segment clock display
- Siren/acoustic device

This platform was originally designed for archery competitions according to World Archery rules (see [rules](https://www.worldarchery.sport/rulebook/search/clock)). The solution is intended to replace an outdated cable-based predecessor.

## Original motivation

Server app             |  Client hardware
:-:|:-:
![](doc/server_app.png)  |  ![](doc/client_hardware.jpg)

## Original goals
- Most of the existing devices should be preserved, with the addition of a remote transmitter and power source.
- Simple construction blueprints for hardware should also be provided (TBD).
- A simple [application](android_app) for Android, compatible with most existing devices, should be created to replace the Windows-based custom software.

## Second generation goals
- Provide new, original hardware that is battery-powered (via off-the-shelf power banks), mobile, and simple.
- See [blueprints](hardware).

## Third generation goals
- Replace the Android app with a device-hosted [web app](web_app) that runs on anything with a web browser (laptop, Android device, iPhone).

## Additional features
- Training mode: Various countdown and signaling scenarios (e.g., mini-games) for training coordination and timing for improved form in sports activities.
