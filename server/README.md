# Android APP to control Semaforky clients

## Build from command-line

### Clean
```bash
./gradlew clean
```

### Build release apk
```bash
./gradlew assembleRelease
```

### Build debug apk
```bash
./gradlew assembleDebug
./gradlew installDebug
```

### Install to Android device
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

### Uninstall semaforky from Android device
```bash
adb shell pm uninstall -k com.vajsoft.semaforky
```