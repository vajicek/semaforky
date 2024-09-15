BOARDCONFIG := esp8266:esp8266:d1:eesz=4M1M
ARDUINO_CLI ?= arduino-cli
MKSPIFFS    ?= mkspiffs
BC          ?= bc
DEVICE      := /dev/ttyUSB0
ESPTOOL_PY  := ${HOME}/.arduino15/packages/esp8266/hardware/esp8266/3.1.2/tools/esptool/esptool.py
PATH        := ${PATH}:/root/bin

# DEPENDENCISE AND PREREQUISITES
install_common:
	mkdir -p ~/Arduino/libraries/
	ln -sf ${PWD}/devices/semaforky_webui_common ~/Arduino/libraries/

install_deps: install_common
	$(ARDUINO_CLI) config init --overwrite
	$(ARDUINO_CLI) config add board_manager.additional_urls http://arduino.esp8266.com/stable/package_esp8266com_index.json
	$(ARDUINO_CLI) core install esp8266:esp8266
	$(ARDUINO_CLI) config set library.enable_unsafe_install true
	$(ARDUINO_CLI) lib install --git-url https://github.com/bblanchon/ArduinoJson.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/me-no-dev/ESPAsyncTCP.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/me-no-dev/ESPAsyncWebServer.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/vajicek/PxMatrix.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/freetronics/DMD2.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/adafruit/Adafruit-GFX-Library.git
	$(ARDUINO_CLI) lib install --git-url https://github.com/adafruit/Adafruit_BusIO.git

# SERVER WEBAPP
serve_ng:
	cd app/webapp/semaforky && ../node_modules/.bin/ng serve --host 0.0.0.0

# BUILD WEBAPP
setup_ng:
	cd app/webapp && npm install @angular/cli

build_webapp:
	sed -e "s/GIT_HASH_PLACEHOLDER/$(git log -1 --format='%H'|cut -c1-6)/" app/webapp/semaforky/src/app/version.ts
	sed -e "s/GIT_DATE_PLACEHOLDER/$(git log -1 --format='%cd' --date=iso)/" app/webapp/semaforky/src/app/version.ts
	cd app/webapp/semaforky && ../node_modules/.bin/ng build --optimization true --output-hashing none
	mkdir -p app/data && cp -r app/webapp/semaforky/dist/semaforky/browser/* app/data

# CREATE WEBAPP FILESYSTEM PACKAGE
filesystem.bin: app/data/main.js
	PROPS=$$($(ARDUINO_CLI) compile --fqbn $(BOARDCONFIG) --show-properties app); \
	BUILD_SPIFFS_BLOCKSIZE=$$(echo "$$PROPS"|grep "^build.spiffs_blocksize"|cut -d= -f2); \
	BUILD_SPIFFS_END=$$(echo "$$PROPS"|grep "^build.spiffs_end"|cut -d= -f2| sed 's/x//'); \
	BUILD_SPIFFS_PAGESIZE=$$(echo "$$PROPS"|grep "^build.spiffs_pagesize"|cut -d= -f2); \
	BUILD_SPIFFS_START=$$(echo "$$PROPS"|grep "^build.spiffs_start"|cut -d= -f2| sed 's/x//'); \
	BUILD_SPIFFS_SIZE=$$(echo "ibase=16;$${BUILD_SPIFFS_END}-$${BUILD_SPIFFS_START}"|bc -q); \
	$(MKSPIFFS) -c app/data -p $$BUILD_SPIFFS_PAGESIZE -b $$BUILD_SPIFFS_BLOCKSIZE -s $$BUILD_SPIFFS_SIZE $@

# FLASH WEBAPP FILESYSTEM
flash-fs: filesystem.bin
	python3 ${ESPTOOL_PY} \
	  --chip auto \
	  --port ${DEVICE} \
	  --baud 921600 \
	  --before default_reset \
	  --after hard_reset \
	  write_flash 0x200000 filesystem.bin

# BUILD AND INSTALL APPLICATIONS
install_app: DIR=app
install_p5: DIR=p5
install_p5_new: DIR=p5_new
install_p10x3: DIR=p10x3
install_p10: DIR=p10
install_siren: DIR=siren
install_p10_semaphore: DIR=p10_semaphore

install_app install_p5 install_p5_new install_p10x3 install_siren install_p10_semaphore:
	rm -f $(PWD)/devices/${DIR}/data
	ln -s $(PWD)/app/data $(PWD)/devices/${DIR}/data
	$(ARDUINO_CLI) compile --fqbn $(BOARDCONFIG) devices/${DIR}
	$(ARDUINO_CLI) upload -p ${DEVICE} --fqbn $(BOARDCONFIG) devices/${DIR}

build_devcontainer:
	docker build -f ./Dockerfile . -t semaforky

devcontainer:
	docker run \
	--privileged -v /dev/bus/usb:/dev/bus/usb \
	-it \
	--name devcontainer \
	-v $(PWD):/tmp/home \
	-w /tmp/home \
	-p 4200:4200 \
	semaforky /bin/bash

deploy_testing:
	cp -r app/webapp/semaforky/dist/semaforky/browser/* ../docs