# semaforky
Android/Arduino-based DIY system for remote controlled signaling for sport events

**NEW!: Next-gen version is based on web app hosted by the device on its own wifi network. See [server_webui](server_webui).**

Semaforky is client-server software for Android (server) and several Arduino-based controllers (clients) of various signaling devices:
- Semaphore lights (red, yellow, green)
- Segment clock display
- Siren/acoustic device

This platform was originally designed for archery competitions according to World Archery rules (REFERENCE TO BE ADDED). The solution should replace outdated cabel-based predecessor.

## Motivation

Server app             |  Client hardware
:-:|:-:
![](doc/server_app.png)  |  ![](doc/client_hardware.jpg)

## Goals
- Most of existing devices should be preserved, only extended by remote transmitter and power source
- Simple construction blueprints of hardware should be also provided
- Simple application for android available for most existing devices should be created to replace Windows-based custom software

## Additional features
- Training mode: various countdown and signaling scenarios (e.g. mini games) for training coordination and timing a good form in sport activity.
