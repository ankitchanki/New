# Validation performed

- Gradle wrapper 8.11.1 (SHA-256 pinned), JDK 21.0.12.1, Android SDK 35.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`: successful.
- 12 JVM tests executed: 12 passed, 0 failed, 0 skipped.
- Android lint: 0 errors, 28 warnings. Remaining diagnostics include best-effort private APIs, platform/resource metadata, physical edge coordinates, backup/launcher refinements, annotation-processor choice, and Kotlin API/style suggestions. No lint rules were disabled.
- Actual shader source strings: 2 Skia runtime programs compiled, with 5 raster assertions covering transparency, visible output, time response, and metaball expansion. Desktop Skia is not Android GPU validation.
- Final debug APK: package `com.vibe.stashdrop`, min SDK 28, target SDK 35; APK signature verification and 16 KB ZIP alignment passed. All bundled arm64-v8a/x86_64 native libraries have 16 KB load-segment alignment.

The debug APK uses a development key. Release signing and minification were not part of the initial debug validation; the completed release checks are recorded below. Installation, runtime interaction, privileged calls, and real device performance remain unverified. No physical Android device or emulator is attached. Follow DEVICE_VALIDATION.md before treating this as a release candidate. GitHub publication is separate from these build and device-validation results.

## Repository presentation validation

- Added seven high-resolution Paparazzi component renders using production Compose code and sample content.
- `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:verifyPaparazziDebug`: successful.
- 19 tests executed: 12 logic tests and 7 visual rendering/baseline checks; all passed, none skipped.
- Latest lint result: 0 errors, 29 warnings.
- Seven published gallery copies match their recorded image hashes and dimensions. Both source-rendered shader animations contain 48 frames.
- 47 local documentation references resolved when preparing the presentation update.
- GitHub About/topic metadata remains pending because the environment proxy denies `api.github.com`; the proposed description and 20 topics are saved in `.github/repository-metadata.json`. This is independent of pushing the README and assets with Git.

## Signed version 0.1.0 preparation

- A dedicated RSA-4096 production key was created with neutral StashDrop certificate metadata. Private signing files and their backup are outside the repository and outside the public release asset directory.
- `python scripts/build-release.py --credentials <private-config> --verify`: successful. Both APK and Android App Bundle were signed; R8 and resource shrinking completed.
- APK v3 signature verified for the app's minimum API 28; AAB JAR signature verified. Both match the exported public signing certificate.
- Final APK ZIP alignment and all bundled arm64-v8a/x86_64 native load-segment alignments passed 16 KB checks.
- 19 tests passed, none failed/skipped. Debug and release lint each completed with zero errors and 29 warnings.
- [Public verification report](releases/v0.1.0-verification.json) records the certificate fingerprint and exact validation scope.
- Existing commits were rewritten with neutral project author/committer identities and unchanged trees. Old local reflogs/unreachable objects were pruned. Cached pages, forks, clones, account profiles, and chat history are outside repository cleanup scope.
- Release creation/upload requires `api.github.com` and `uploads.github.com`; their domain additions are saved in the environment draft. GitHub publication is not established by these local signing checks.
