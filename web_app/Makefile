BOARDCONFIG := esp8266:esp8266:d1:eesz=4M1M
ARDUINO_CLI ?= arduino-cli
MKSPIFFS    ?= mkspiffs
BC          ?= bc
DEVICE      := /dev/ttyUSB0
ESPTOOL_PY  := ${HOME}/.arduino15/packages/esp8266/hardware/esp8266/3.1.2/tools/esptool/esptool.py
UID         ?= $(shell id -u)
GID         ?= $(shell id -g)
DOCKER_IMAGE ?= semaforky
DOCKER_DEVICE ?=
DOCKER_BUILD_ARGS ?=
VERSION_FILE := app/webapp/semaforky/src/app/version.ts
NG           := node_modules/.bin/ng

# DEPENDENCISE AND PREREQUISITES
.PHONY: install_common install_deps serve_ng setup_ng build_webapp build test clean build-fs flash-fs \
	install_app install_p5 install_p5_new install_p10x3 install_p10 install_siren install_p10_semaphore \
	build_devcontainer rebuild_devcontainer devcontainer deploy_testing userguide

install_common:
	mkdir -p ~/Arduino/libraries/
	ln -sf ${PWD}/devices/semaforky_webui_common ~/Arduino/libraries/

install_deps: install_common
	$(ARDUINO_CLI) config init --overwrite
	$(ARDUINO_CLI) config add board_manager.additional_urls https://arduino.esp8266.com/stable/package_esp8266com_index.json
	$(ARDUINO_CLI) core install esp8266:esp8266@3.1.2
	$(ARDUINO_CLI) config set library.enable_unsafe_install true
	$(ARDUINO_CLI) lib install --git-url https://github.com/bblanchon/ArduinoJson.git#1cd478f48e7c4cd142a0bd26ee17d63d70d04e3f
	$(ARDUINO_CLI) lib install --git-url https://github.com/me-no-dev/ESPAsyncTCP.git#191bdeb2555bf86e213ecb816227d125753b608e
	$(ARDUINO_CLI) lib install --git-url https://github.com/me-no-dev/ESPAsyncWebServer.git#ad3741d159f9cfd50c567e81b67f3bef1dc6d89f
	$(ARDUINO_CLI) lib install --git-url https://github.com/vajicek/PxMatrix.git#4f67a75bea468715e183312b1c43b4d6530cedcb
	$(ARDUINO_CLI) lib install --git-url https://github.com/freetronics/DMD2.git#b78bd7b9b5287238f90e810e93a97d5d291bca86
	$(ARDUINO_CLI) lib install --git-url https://github.com/adafruit/Adafruit-GFX-Library.git#ac6d7c3869a693d406f77b9bfcd486b0673169f0
	$(ARDUINO_CLI) lib install --git-url https://github.com/adafruit/Adafruit_BusIO.git#3b8364267c3ee6e16bad91bc2101aefbd5b5915f

# SERVER WEBAPP
serve_ng:
	cd app/webapp/semaforky && $(NG) serve --host 0.0.0.0

# BUILD WEBAPP
setup_ng:
	cd app/webapp/semaforky && npm ci

build_webapp:
	sed -e "s/gitHash = \"\(.*\)\"/gitHash = \"$(shell git log -1 --format='%H'|cut -c1-6)\"/" $(VERSION_FILE).temp > $(VERSION_FILE)
	sed -i -e "s/gitDate = \"\(.*\)\"/gitDate = \"$(shell git log -1 --format='%cd' --date=iso)\"/" $(VERSION_FILE)
	cd app/webapp/semaforky && $(NG) build --optimization true --output-hashing none
	mkdir -p app/data && cp -r app/webapp/semaforky/dist/semaforky/browser/* app/data

build: setup_ng build_webapp

test:
	cd app/webapp/semaforky && $(NG) test --watch=false

clean:
	rm -rf app/data app/webapp/semaforky/.angular app/webapp/semaforky/dist app/webapp/semaforky/out-tsc filesystem.bin app/webapp/semaforky/src/app/version.ts

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

build-fs: build_webapp filesystem.bin

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
	docker build \
	--build-arg UID=$(UID) \
	--build-arg GID=$(GID) \
	$(DOCKER_BUILD_ARGS) \
	-f ./Dockerfile \
	. \
	-t $(DOCKER_IMAGE)

rebuild_devcontainer:
	$(MAKE) build_devcontainer DOCKER_BUILD_ARGS=--no-cache

devcontainer:
	docker run \
	--rm \
	-it \
	-v $(PWD)/..:/tmp/home \
	-v semaforky-arduino:/home/myuser/.arduino15 \
	-v semaforky-arduino-libraries:/home/myuser/Arduino \
	-w /tmp/home/web_app \
	-p 4200:4200 \
	-u $(UID):$(GID) \
	$(if $(DOCKER_DEVICE),--device=$(DOCKER_DEVICE),) \
	$(DOCKER_IMAGE) /bin/bash

deploy_testing:
	cp -r app/webapp/semaforky/dist/semaforky/browser/* ../docs

userguide:
	npm i md-to-pdf
	node_modules/.bin/md-to-pdf \
	--pdf-options '{ "margin": "20mm 25mm 20mm 25mm", "printBackground": true }' \
	UserGuide_cz.md
