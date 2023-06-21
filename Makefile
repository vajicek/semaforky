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

build_client_p10:
	echo TODO

build_client_sirene:
	echo TODO

build_client_semaforky:
	echo TODO

build_client_clock:
	echo TODO
