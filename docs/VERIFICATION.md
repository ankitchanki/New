# Validation performed

- Gradle wrapper 8.11.1 (SHA-256 pinned), JDK 21.0.12.1, Android SDK 35.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: successful.
- 12 JVM tests executed: 12 passed, 0 failed, 0 skipped.
- Android lint: 0 errors, 28 warnings. Remaining diagnostics include best-effort private APIs, platform/resource metadata, physical edge coordinates, backup/launcher refinements, annotation-processor choice, and Kotlin API/style suggestions. No lint rules were disabled.
- Actual shader source strings: 2 Skia runtime programs compiled, with 5 raster assertions covering transparency, visible output, time response, and metaball expansion. Desktop Skia is not Android GPU validation.
- Final debug APK: package `com.vibe.stashdrop`, min SDK 28, target SDK 35; APK signature verification and 16 KB ZIP alignment passed. All bundled arm64-v8a/x86_64 native libraries have 16 KB load-segment alignment.

The debug APK uses a development key. Release signing, release minification, installation, runtime interaction, privileged calls, and real device performance have not been validated. No physical Android device or emulator is attached. Follow DEVICE_VALIDATION.md before treating this as a release candidate. GitHub publication is separate from these build and device-validation results.

## Repository presentation validation

- Added seven high-resolution Paparazzi component renders using production Compose code and sample content.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyPaparazziDebug`: successful.
- 19 tests executed: 12 logic tests and 7 visual rendering/baseline checks; all passed, none skipped.
- Latest lint result: 0 errors, 29 warnings.
- Seven published gallery copies match their recorded image hashes and dimensions. Both source-rendered shader animations contain 48 frames.
- 47 local documentation references resolved when preparing the presentation update.
- GitHub About/topic metadata remains pending because the environment proxy denies `api.github.com`; the proposed description and 20 topics are saved in `.github/repository-metadata.json`. This is independent of pushing the README and assets with Git.
