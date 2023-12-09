BOARDCONFIG := esp8266:esp8266:d1:eesz=4M1M
ARDUINO_CLI ?= arduino-cli
MKSPIFFS    ?= mkspiffs
BC          ?= bc
DEVICE      := /dev/ttyUSB0
ESPTOOL_PY  := ${HOME}/.arduino15/packages/esp8266/hardware/esp8266/3.0.2/tools/esptool/esptool.py

# DEPENDENCISE AND PREREQUISITES
install_common:
	ln -s ${PWD}/common ~/Arduino/libraries

install_deps:
	$(ARDUINO_CLI) config init
	$(ARDUINO_CLI) config set library.enable_unsafe_install true
	$(ARDUINO_CLI) lib install --git-url https://github.com/bblanchon/ArduinoJson.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/me-no-dev/ESPAsyncTCP.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/me-no-dev/ESPAsyncWebServer.git

# BUILD WEBAPP
setup_ng:
	cd prototype/webapp && npm install @angular/cli

build_webapp:
	cd prototype/webapp/semaforky && ../node_modules/.bin/ng build --optimization true --output-hashing none
	mkdir -p prototype/data && cp prototype/webapp/semaforky/dist/semaforky/browser/* prototype/data

# CREATE WEBAPP FILESYSTEM PACKAGE
filesystem.bin:
	PROPS=$$($(ARDUINO_CLI) compile --fqbn $(BOARDCONFIG) --show-properties prototype)
	BUILD_SPIFFS_BLOCKSIZE=$$(echo "$$PROPS"|grep "^build.spiffs_blocksize"|cut -d= -f2)
	BUILD_SPIFFS_END=$$(echo "$$PROPS"|grep "^build.spiffs_end"|cut -d= -f2| sed 's/x//')
	BUILD_SPIFFS_PAGESIZE=$$(echo "$$PROPS"|grep "^build.spiffs_pagesize"|cut -d= -f2)
	BUILD_SPIFFS_START=$$(echo "$$PROPS"|grep "^build.spiffs_start"|cut -d= -f2| sed 's/x//')
	BUILD_SPIFFS_SIZE=$$(echo "ibase=16;$${BUILD_SPIFFS_END}-$${BUILD_SPIFFS_START}"|bc -q)
	$(MKSPIFFS) -c prototype/data -p $$BUILD_SPIFFS_PAGESIZE -b $$BUILD_SPIFFS_BLOCKSIZE -s $$BUILD_SPIFFS_SIZE $@

#TODO: FINISH THIS
flash-fs: filesystem.bin
	python3 ${ESPTOOL_PY} \
	  --chip esp8266 \
	  --port ${DEVICE} \
	  --baud 460800 \
	  --before default_reset \
	  --after hard_reset \
	  write_flash 0x300000 filesystem.bin

# BUILD AND INSTALL APPLICATIONS
install_prototype: DIR=prototype
install_p5: DIR=p5
install_p10x3: DIR=p10x3
install_p10: DIR=p10
install_siren: DIR=siren

install_prototype install_p5 install_p10x3 install_siren:
	$(ARDUINO_CLI) compile --fqbn $(BOARDCONFIG) ${DIR}
	$(ARDUINO_CLI) upload -p ${DEVICE} --fqbn $(BOARDCONFIG) ${DIR}
