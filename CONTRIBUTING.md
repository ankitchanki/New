# Contributing to StashDrop

Build the app with JDK 17+ and Android SDK 35. Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` before sending a change. Keep local SDK paths, signing keys, real clipboard payloads, and captured personal content out of commits.

For visual changes, regenerate the README renders using [UI_GALLERY.md](docs/UI_GALLERY.md). Keep promotional artwork, component renders, and real-device captures clearly identified. Do not claim device compatibility or performance from a host-rendered image.

Useful device reports include the Android version, manufacturer, navigation mode, StashDrop version/commit, whether Shizuku or accessibility was enabled, reproduction steps, and expected versus actual behavior. Use the [device checklist](docs/DEVICE_VALIDATION.md); redact private content from logs and screenshots.

For privileged APIs, keep the built-in fallback usable. Test permission denial, binder death, unsupported APIs, and canceled capture/share flows. Preserve package signatures, checksums, and TLS verification when changing setup tooling.

A license has not yet been selected. The repository owner decides licensing and release distribution.
