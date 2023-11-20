
build_webapp:
	cd prototype/webapp/semaforky && ../node_modules/.bin/ng build --optimization true --output-hashing none
	cp prototype/webapp/semaforky/dist/semaforky/browser/* prototype/data

install_esp:
	arduino-cli compile --fqbn esp8266:esp8266:d1 prototype
	arduino-cli upload -p /dev/ttyUSB0 --fqbn esp8266:esp8266:d1 prototype