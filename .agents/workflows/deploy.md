---
description: Build, install, and launch the Android application.
---

This workflow will automatically build the Android application, install it to your connected device, and launch it!

// turbo-all

1. Ensure permissions and environment
   `chmod +x ./gradlew`

2. Check for connected ADB device
   `adb devices`

3. Build APK with dynamic JAVA_HOME detection
   `export JH=$(mdfind "kMDItemCFBundleIdentifier == 'com.google.AndroidStudio*'" | xargs -I {} find {} -name "Home" -path "*/jbr/Contents/Home" | head -n 1) && [ -z "$JH" ] && export JH="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; JAVA_HOME="$JH" ./gradlew assembleDebug`

4. Install to device
   `adb install -r ./app/build/outputs/apk/debug/app-debug.apk`

5. Launch
   `adb shell am start -n net.darapu.projectbd/net.darapu.projectbd.MainActivity`
