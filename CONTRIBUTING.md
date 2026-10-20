# Contributing to StashDrop

Build the app with JDK 17+ and Android SDK 35. Run `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` before sending a change. Keep local SDK paths, signing keys, real clipboard payloads, and captured personal content out of commits.

Use the [feature guide](docs/FEATURE_GUIDE.md) and [architecture](docs/ARCHITECTURE.md) to understand current behavior. Use the [roadmap](docs/ROADMAP.md) to propose a scoped improvement, explain its user benefit, and define evidence that it works. Keep proposed capabilities separate from released features.

For visual changes, regenerate the README renders using [UI_GALLERY.md](docs/UI_GALLERY.md). Keep promotional artwork, component renders, and real-device captures clearly identified. Do not claim device compatibility or performance from a host-rendered image.

Useful device reports include the Android version, manufacturer, navigation mode, StashDrop version/commit, whether Shizuku was connected and Google selected as assistant, reproduction steps, and expected versus actual behavior. Use the [device checklist](docs/DEVICE_VALIDATION.md); redact private content from logs and screenshots.

For privileged APIs, keep Google-only search routing and actionable rejection guidance. Do not reintroduce an internal capture/search fallback. Test permission denial, binder death, unsupported APIs, Google readiness and canceled share flows. Preserve package signatures, checksums, and TLS verification when changing setup tooling.

Documentation changes should update the relevant feature instructions, defaults, platform limits, and release notes when applicable. Check relative links and keep current-release evidence separate from historical validation. For documentation-only edits, a full Android rebuild is unnecessary.

A license has not yet been selected. The repository owner decides licensing and release distribution.
