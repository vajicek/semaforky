BOARDCONFIG := esp8266:esp8266:d1:eesz=4M1M
ARDUINO_CLI ?= arduino-cli
MKSPIFFS    ?= mkspiffs
BC          ?= bc
DEVICE      := /dev/ttyUSB0
ESPTOOL_PY  := ${HOME}/.arduino15/packages/esp8266/hardware/esp8266/3.1.2/tools/esptool/esptool.py
PATH        := ${PATH}:/root/bin
UID := $(shell id -u)
GID := $(shell id -g)
VERSION_FILE := app/webapp/semaforky/src/app/version.ts
NG           := node_modules/.bin/ng

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
	cd app/webapp/semaforky && $(NG) serve --host 0.0.0.0

# BUILD WEBAPP
setup_ng:
	cd app/webapp/semaforky && npm install @angular/cli @ngx-translate/core @ngx-translate/http-loader

build_webapp:
	git config --global --add safe.directory /tmp/home || true
	sed -e "s/gitHash = \"\(.*\)\"/gitHash = \"$(shell git log -1 --format='%H'|cut -c1-6)\"/" $(VERSION_FILE).temp > $(VERSION_FILE)
	sed -i -e "s/gitDate = \"\(.*\)\"/gitDate = \"$(shell git log -1 --format='%cd' --date=iso)\"/" $(VERSION_FILE)
	cd app/webapp/semaforky && $(NG) build --optimization true --output-hashing none
	mkdir -p app/data && cp -r app/webapp/semaforky/dist/semaforky/browser/* app/data

# CREATE WEBAPP FILESYSTEM PACKAGE
filesystem.bin: app/data/main.js
	PROPS=$$($(ARDUINO_CLI) compile --fqbn $(BOARDCONFIG) --show-properties devices/p5); \
	BUILD_SPIFFS_BLOCKSIZE=$$(echo "$$PROPS"|grep "^build.spiffs_blocksize"|cut -d= -f2); \
	BUILD_SPIFFS_END=$$(echo "$$PROPS"|grep "^build.spiffs_end"|cut -d= -f2| sed 's/x//'); \
	BUILD_SPIFFS_PAGESIZE=$$(echo "$$PROPS"|grep "^build.spiffs_pagesize"|cut -d= -f2); \
	BUILD_SPIFFS_START=$$(echo "$$PROPS"|grep "^build.spiffs_start"|cut -d= -f2| sed 's/x//'); \
	BUILD_SPIFFS_SIZE=$$(echo "ibase=16;$${BUILD_SPIFFS_END}-$${BUILD_SPIFFS_START}"|bc -q); \
	echo BUILD_SPIFFS_BLOCKSIZE=$${BUILD_SPIFFS_BLOCKSIZE}; \
	echo BUILD_SPIFFS_END=$${BUILD_SPIFFS_END}; \
	echo BUILD_SPIFFS_PAGESIZE=$${BUILD_SPIFFS_PAGESIZE}; \
	echo BUILD_SPIFFS_START=$${BUILD_SPIFFS_START}; \
	echo BUILD_SPIFFS_SIZE=$${BUILD_SPIFFS_SIZE}; \
	$(MKSPIFFS) -c app/data -p $$BUILD_SPIFFS_PAGESIZE -b $$BUILD_SPIFFS_BLOCKSIZE -s $$BUILD_SPIFFS_SIZE $@

# FLASH WEBAPP FILESYSTEM
flash-fs: filesystem.bin
	PROPS=$$($(ARDUINO_CLI) compile --fqbn $(BOARDCONFIG) --show-properties devices/p5); \
	BUILD_SPIFFS_START=$$(echo "$$PROPS"|grep "^build.spiffs_start"|cut -d= -f2); \
	UPLOAD_SPEED=$$(echo "$$PROPS"|grep "^upload.speed"|cut -d= -f2); \
	echo BUILD_SPIFFS_START=$${BUILD_SPIFFS_START}; \
	echo UPLOAD_SPEED=$${UPLOAD_SPEED}; \
	python3 ${ESPTOOL_PY} \
	  --chip auto \
	  --port ${DEVICE} \
	  --baud $$UPLOAD_SPEED \
	  --before default_reset \
	  --after hard_reset \
	  write_flash $$BUILD_SPIFFS_START filesystem.bin

# BUILD AND INSTALL APPLICATIONS
install_app: DIR=app
install_p5: DIR=p5
install_p5_new: DIR=p5_new
install_p10x3: DIR=p10x3
install_p10: DIR=p10
install_siren: DIR=siren
install_p10_semaphore: DIR=p10_semaphore

install_app install_p5 install_p5_new install_p10x3 install_siren install_p10_semaphore:
	rm -fr $(PWD)/devices/${DIR}/data
	ln -s $(PWD)/app/data $(PWD)/devices/${DIR}/data
	$(ARDUINO_CLI) compile --fqbn $(BOARDCONFIG) devices/${DIR}
	$(ARDUINO_CLI) upload -p ${DEVICE} --fqbn $(BOARDCONFIG) devices/${DIR}

build_devcontainer:
	cp /etc/ssl/certs/ca-certificates.crt .
	docker build \
	--build-arg HTTP_PROXY= --build-arg HTTPS_PROXY= --build-arg NO_PROXY= \
	--build-arg UID=$(UID) \
	--build-arg GID=$(GID) \
	--no-cache \
	-f ./Dockerfile \
	. \
	-t semaforky

devcontainer:
	docker run \
	--privileged -v /dev/bus/usb:/dev/bus/usb \
	-it \
	--name semaforky-devcontainer \
	-v $(PWD)/..:/tmp/home \
	-w /tmp/home/web_app \
	-p 4200:4200 \
	-u 0 \
	semaforky /bin/bash

deploy_testing:
	cp -r app/webapp/semaforky/dist/semaforky/browser/* ../docs
