---
description: Build, Deploy, and Run the app
---

This workflow will automatically build the Android application, install it to your connected device, and launch it!

// turbo-all

1. Build the APK using Gradle
   `./gradlew assembleDebug`
2. Install the APK to your connected device
   `adb install -r ./app/build/outputs/apk/debug/app-debug.apk`
3. Launch the app on your device
   `adb shell monkey -p net.darapu.projectbd -c android.intent.category.LAUNCHER 1`
