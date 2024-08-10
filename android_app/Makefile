publish:
	./publish.sh

clean_app:
	cd server && ./gradlew clean

build_app_apk:
	cd server && ./gradlew assembleRelease

build_debug_app_apk:
	cd server && ./gradlew assembleDebug && ./gradlew installDebug

install_app:
	cd server && adb install app/build/outputs/apk/release/app-release.apk

uninstall_app:
	cd server && adb shell pm uninstall -k com.vajsoft.semaforky

build_sketch:
	arduino-cli compile --fqbn esp8266:esp8266:d1 devices/${CLIENT}_client
	arduino-cli upload -p /dev/ttyUSB0 --fqbn esp8266:esp8266:d1 devices/${CLIENT}_client

build_client_p10:
	$(MAKE) CLIENT=p10 build_sketch

build_client_p5:
	$(MAKE) CLIENT=p5 build_sketch

build_client_p10x3:
	$(MAKE) CLIENT=p10x3 build_sketch

build_client_sirene:
	$(MAKE) CLIENT=sirene build_sketch

build_client_semaforky:
	$(MAKE) CLIENT=semaphore build_sketch

build_client_clock:
	$(MAKE) CLIENT=clock build_sketch
