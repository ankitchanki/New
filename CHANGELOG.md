# Changelog

All notable StashDrop changes are recorded here. Versions follow Semantic Versioning.

## [0.1.1] - 2026-10-19

### Added

- GitHub Actions workflow: debug APK, tests, screenshot verification, and lint on every push; signed APK/AAB GitHub release on version tags.

### Fixed

- Taps inside the floating shelf sheet no longer close it.
- Circle Studio Stash, Lens, and Share reuse one saved cutout instead of saving duplicates.
- The Shelf collection filter falls back to "All" when its collection disappears.
- Floating overlays reappear after screen-on on devices without a lock screen.

## [0.1.0] - 2026-10-19

### Added

- Native Android app (`com.vibe.stashdrop`) with minimum SDK 28 and target SDK 35.
- Compose Overview, Shelf, and Controls screens with a live motion playground.
- Floating thumb dock with edge snapping, idle peek, and a one-handed shelf sheet.
- Local Room storage for notes, links, collections, and imported attachments.
- Android Share receivers, multi-selection, stack drag shadows, and outbound sharing.
- Built-in Circle Studio with one-shot capture routing, freehand selection, masked cutouts, bundled Latin-script OCR, and barcode scanning.
- Optional Shizuku lifecycle, shell capture, assistant setting restoration, background text clipboard listening, and best-effort native Google search.
- Tracking-parameter sanitation that preserves raw URL encoding and surrounding punctuation.
- AGSL aurora/metaball shaders, Canvas fallbacks, spring motion, Bezier trajectories, and haptic feedback.
- Branded README, seven source-rendered UI previews, animated shader examples, and reproducible visual baselines.
- Dedicated production signing configuration, release APK/app-bundle build tooling, public certificate verification, and checksum generation.

### Changed

- Project commit attribution uses the neutral identity `StashDrop Contributors`.
- Published branch history was rewritten to remove personal author and committer attribution while preserving file contents.
- Release builds require signing credentials and enable code/resource shrinking.

### Verified

- Debug build, 12 logic tests, seven visual rendering/baseline checks, and Android lint passed before release preparation.
- Signed, minified release APK and app bundle built successfully with a dedicated RSA-4096 identity.
- APK v3 signature and AAB JAR signature verified; public certificate fingerprints match.
- 16 KB APK ZIP alignment and all bundled 64-bit native load-segment alignments verified.
- All 19 logic/visual tests passed; debug and release lint completed with zero errors and 29 warnings each.
- Final public artifact checksums are distributed with the release verification report.

### Known limitations

- Real-device startup, overlay lifecycle, privileged Binder calls, capture permissions, and cross-app drop behavior require phone testing.
- Native Google Circle to Search depends on the ROM, Google software, authorization, and eligibility; Studio is the fallback.
- Background clipboard availability varies by Android/OEM. File/image clipboard monitoring is not implemented.
- Protected/DRM content cannot be captured. Gesture strips do not replace system navigation buttons.
- Host UI renders and desktop shader tests do not establish phone performance or 120 Hz operation.
