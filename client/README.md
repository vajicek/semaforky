# Clients based on Wemos D1 mini (ESP8266)

## Prerequisites
* Install [Arduino IDE](https://www.arduino.cc/en/software)

* Install support for ESP8266 [http://arduino.esp8266.com/stable/package_esp8266com_index.json](http://arduino.esp8266.com/stable/package_esp8266com_index.json)

* Install semaforky_common as local library

	* On Windows
	```cmd
	cd Arduino\libraries
	mklink /D semaforky_common semaforky_github\client\semaforky_common
	```
	* On Linux
	```bash
	cd Arduino\libraries
	ln -s semaforky_github/client/semaforky_common
	```
	* Restart Arduino Studio


## Upload sketch in ArduinoIDE
* Open one of sketches, i.e. **\*_client/\*.ino**
* Connect ESP8266 with USB
* Upload program

## Upload sketch from command-line
* Install [Arduino-CLI](https://arduino.github.io/arduino-cli/)
* Install **\*.ino** from command-line
```bash
arduino-cli compile --fqbn esp8266:esp8266:d1 <*_client>
arduino-cli upload -p /dev/ttyUSB0 --fqbn esp8266:esp8266:d1 <*_client>
```
