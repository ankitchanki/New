# Validation performed

- Gradle wrapper 8.11.1 (SHA-256 pinned), JDK 21.0.12.1, Android SDK 35.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: successful.
- 12 JVM tests executed: 12 passed, 0 failed, 0 skipped.
- Android lint: 0 errors, 28 warnings. Remaining diagnostics include best-effort private APIs, platform/resource metadata, physical edge coordinates, backup/launcher refinements, annotation-processor choice, and Kotlin API/style suggestions. No lint rules were disabled.
- Actual shader source strings: 2 Skia runtime programs compiled, with 5 raster assertions covering transparency, visible output, time response, and metaball expansion. Desktop Skia is not Android GPU validation.
- Final debug APK: package `com.vibe.stashdrop`, min SDK 28, target SDK 35; APK signature verification and 16 KB ZIP alignment passed. All bundled arm64-v8a/x86_64 native libraries have 16 KB load-segment alignment.

The debug APK uses a development key. Release signing, release minification, installation, runtime interaction, privileged calls, and real device performance have not been validated. No physical Android device or emulator is attached. Follow DEVICE_VALIDATION.md before treating this as a release candidate. GitHub publication is separate from these build and device-validation results.
