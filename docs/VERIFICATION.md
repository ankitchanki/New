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
- GitHub About/topic metadata remains pending. The initial attempt was blocked by network policy; a later API attempt returned HTTP 403 for the current integration. The proposed description and 20 topics are saved in `.github/repository-metadata.json`. This is independent of pushing the README and assets with Git.

## Signed version 0.1.0 preparation

- A dedicated RSA-4096 production key was created with neutral StashDrop certificate metadata. Private signing files and their backup are outside the repository and outside the public release asset directory.
- `python scripts/build-release.py --credentials <private-config> --verify`: successful. Both APK and Android App Bundle were signed; R8 and resource shrinking completed.
- APK v3 signature verified for the app's minimum API 28; AAB JAR signature verified. Both match the exported public signing certificate.
- Final APK ZIP alignment and all bundled arm64-v8a/x86_64 native load-segment alignments passed 16 KB checks.
- 19 tests passed, none failed/skipped. Debug and release lint each completed with zero errors and 29 warnings.
- [Public verification report](releases/v0.1.0-verification.json) records the certificate fingerprint and exact validation scope.
- Existing commits were rewritten with neutral project author/committer identities and unchanged trees. Old local reflogs/unreachable objects were pruned. Cached pages, forks, clones, account profiles, and chat history are outside repository cleanup scope.
- Release creation/upload requires `api.github.com` and `uploads.github.com`; their domain additions are saved in the environment draft. GitHub publication is not established by these local signing checks.

## Signed version 0.1.1 preparation

- Integrated the release-workflow branch and its shelf, cutout reuse, screen-on, and collection-filter fixes. Version name is `0.1.1`, version code is `2`.
- Built signed, minified APK and AAB using the original production key. Signature verification and public certificate SHA-256 continuity checks passed.
- All 19 logic/visual tests passed, none failed or skipped. Debug and release lint each completed with zero errors and 29 warnings.
- APK ZIP alignment and all bundled arm64-v8a/x86_64 native load-segment alignments passed 16 KB checks.
- [Public verification report](releases/v0.1.1-verification.json) records the version and certificate facts.
- The workflow passed `actionlint`. Its signing restoration was exercised with the existing private inputs: key bytes and owner-only permissions were preserved, and missing-secret input was rejected. Private test copies were removed.
- Actions signing-secret setup returned HTTP 403 for this integration. A repository owner must configure the four documented secrets or provide authorized Secrets write access. This does not establish whether another owner has already configured them.
- These are current-instance build results. A successful GitHub Actions run, published GitHub Release, fresh-environment restoration, and phone behavior each require separate evidence.

## Signed version 0.3.2 with the original key

- Built the minified APK and AAB locally with version code `6` and the original RSA-4096 production identity. APK v3 and AAB JAR signatures verified; the certificate SHA-256 matches 0.1.0.
- All 27 tests passed, with no failures, errors, or skips. Debug/release lint each reported zero errors and 42 warnings.
- Final APK ZIP alignment and all bundled 64-bit native load-segment alignments passed 16 KB checks. See the [public verification report](releases/v0.3.2-verification.json).
- The new Robolectric tests initially could not create `/home/agent/.robolectric-download-lock` in this managed workspace. A Gradle init script outside Git now assigns writable Java/Maven cache paths and preserves the configured proxy/CA trust for test JVMs; the complete release checks then passed. App/test sources and checks were not weakened.
- Six signing-input checks passed: the original key survives CRLF, wrapped whitespace, and accidental double encoding; ZIP data, a wrong password, and a wrong alias are rejected before Gradle. The workflow also passed `actionlint`.
- The remote workflow passed debug checks, but the current Actions secret decoded to an invalid keystore (3,263 bytes, first byte `0x0e`). Restoring the four original signing values is needed for CI. This integration returned HTTP 403 for Actions-secret setup. Local signing is verified independently of GitHub publication.
