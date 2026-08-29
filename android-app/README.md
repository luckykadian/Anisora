# Anisora Android (Gradle)

Requires **JDK 17** and an **Android SDK** (`compileSdk 34`).

```bash
# Windows (PowerShell), SDK at D:\Android\Sdk:
copy local.properties.example local.properties
# local.properties should contain:  sdk.dir=D:/Android/Sdk

# generate the wrapper once if ./gradlew is missing:
gradle wrapper --gradle-version 8.4

.\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

This module compiles:

- Java UI in `../android-native/src`
- Kotlin Aniyomi source-api + extension loader (`ExtBridge`)
- Media3 ExoPlayer
