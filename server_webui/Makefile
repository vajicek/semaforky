
setup_ng:
	cd prototype/webapp && npm install @angular/cli

build_webapp:
	cd prototype/webapp/semaforky && ../node_modules/.bin/ng build --optimization true --output-hashing none
	mkdir -p prototype/data && cp prototype/webapp/semaforky/dist/semaforky/browser/* prototype/data

upload_webapp:
	echo 123

install_esp:
	arduino-cli compile --fqbn esp8266:esp8266:d1 prototype
	arduino-cli upload -p /dev/ttyUSB0 --fqbn esp8266:esp8266:d1 prototype

install_p10x3:
	arduino-cli compile --fqbn esp8266:esp8266:d1 p10x3
	arduino-cli upload -p /dev/ttyUSB0 --fqbn esp8266:esp8266:d1 p10x3

install_common:
	ln -s ${PWD}/common ~/Arduino/libraries

sketch          := foo.ino
boardconfig     := esp8266:esp8266:generic:eesz=4M1M

ARDUINO_CLI ?= arduino-cli
MKSPIFFS    ?= mkspiffs
BC          ?= bc


all: build

.PHONY: build
build:
	$(ARDUINO_CLI) compile --fqbn $(boardconfig) $(sketch)

.ONESHELL:
filesystem.bin:
	PROPS=$$($(ARDUINO_CLI) compile --fqbn $(boardconfig) --show-properties prototype)
	BUILD_SPIFFS_BLOCKSIZE=$$(echo "$$PROPS"|grep "^build.spiffs_blocksize"|cut -d= -f2)
	BUILD_SPIFFS_END=$$(echo "$$PROPS"|grep "^build.spiffs_end"|cut -d= -f2| sed 's/x//')
	BUILD_SPIFFS_PAGESIZE=$$(echo "$$PROPS"|grep "^build.spiffs_pagesize"|cut -d= -f2)
	BUILD_SPIFFS_START=$$(echo "$$PROPS"|grep "^build.spiffs_start"|cut -d= -f2| sed 's/x//')
	BUILD_SPIFFS_SIZE=$$(echo "ibase=16;$${BUILD_SPIFFS_END}-$${BUILD_SPIFFS_START}"|bc -q)
	$(MKSPIFFS) -c prototype/data -p $$BUILD_SPIFFS_PAGESIZE -b $$BUILD_SPIFFS_BLOCKSIZE -s $$BUILD_SPIFFS_SIZE $@

.PHONY: flash
flash: filesystem.bin

.PHONY: flash-fs
flash-fs: