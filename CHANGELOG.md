# Changelog

All notable StashDrop changes are recorded here. Versions follow Semantic Versioning.

## [0.6.0] - 2026-10-20

### Fixed

- Hide idle decorative overlay windows and lower combined active opacity below Android’s untrusted-touch limit, addressing scrolling/touch blocking in other apps.
- Raise corner-search markers above stable navigation insets and away from system Back edges; directional arrows distinguish them from Home/Recents gestures.
- Bound floating panel input to the rectangular panel and dismiss on outside touch. Canceled handle drags do not open the panel.

### Added

- Slim handles on both screen edges with tap/inward pull to a compact panel, hold for Google search, reposition and drag-to-X.
- Actual local media thumbnails with bounded off-thread decoding, EXIF rotation, representative-frame video previews, cache limits and unavailable-media placeholders.
- Images, Videos, Stickers, URLs, OTP, Text and Files filters in both shelf surfaces; collections combine with type filters and filter changes clear selection.
- Media/EXIF, category, selection, overlay-opacity and panel-input regressions; eight refreshed production renders including edge controls.

### Changed

- Gesture markers support landscape as well as portrait. Google-only search, saved shelf data, original signing identity and Room schema are retained. Version code 9.

## [0.5.0] - 2026-10-20

### Changed

- All Circle to Search entry points request Google only. Removed the internal screenshot/lasso/OCR/barcode UI, fallback, capture services/permission setup and ML Kit models; existing shelf data is retained.
- Added a Google contextual-session request under shell identity through connected Shizuku, with actionable setup guidance on rejection. Google controls Circle to Search eligibility and may display Gemini instead.
- Visible center/corner gesture markers replace invisible touch strips; unchanged settings preserve pending taps. Added platform-timed hold/double-tap recognition, tap distance checks, diagonal up/inward corner validation, one-trigger semantics and cancellation/multi-touch cleanup.
- Gesture navigation reporting an invisible navigation bar no longer hides markers. Markers hide for keyboard, lock, landscape and search hand-off.
- Basic Shizuku setup changes overlay/notification access only. Explicit advanced StashDrop assistant selection also attempts to reconnect Google's voice service.
- Updated current guides, gallery and Windows device-test instructions. Added Google-only failure and API 28/35 gesture regressions.
- Version code 8; package, Room schema and original production signing certificate retained.

## [0.4.0] - 2026-10-20

### Added

- Native Compose implementation of the SVG UI direction across Home, Shelf, Settings, interaction preferences, and built-in Circle to Search.
- Main Shelf multi-selection with a reachable selection bar for Share, WhatsApp, batch collection moves, clear selection, and confirmed deletion.
- Explicit sensitive-text confirmation before enabling clipboard auto-stash; deletion confirmation in the floating shelf.
- Four native UI interaction regressions and an additional production render for the interaction settings page.

### Changed

- Setup uses individual access cards with clearer Google voice-service eligibility text. Digital assistant details expand separately; gestures, clipboard, and feel have their own page.
- Floating shelf selection/sharing controls sit below the scrolling card list.
- Collection changes clear the selection; opening a recent Home item resets the filter to reveal it. Navigation, note drafts, and interaction page state use saved Compose state.
- Version code is 7. Package, Room schema, and original production signing identity are retained for compatible updates.

### Design

- Added an editable nine-screen SVG overview, a standalone scripted SVG, and an offline browser studio for the main app flows. The design simulates Android services and external actions; release APKs are unchanged.

### Documentation

- Refreshed the project description, download/first-run instructions, feature explanations, defaults, and published release evidence.
- Added complete feature, architecture/data-flow, and prioritized improvement/future UI guides. Proposed capabilities are explicitly identified.
- Updated the visual guide, platform boundaries, contribution guidance, and 0.3.2 release explanation. Existing release binaries, version tags, and signing identity are unchanged.
- Repository metadata helper checks the 20-topic limit before writing and verifies applied metadata while preserving existing topics.

## [0.3.2] - 2026-10-20

### Changed

- Retains the original StashDrop production signing identity for compatible updates. The certificate fingerprint is in the release's `release-verification.json` and `StashDrop-release-certificate.pem`.
- App content is the same as 0.3.1.
- Release signing input checks run before Gradle and require the original production certificate. Whitespace and accidental extra Base64 encoding are handled; invalid keystores, passwords, and aliases fail early.

### Verified

- Signed APK and AAB built with the original RSA-4096 key; both signatures and certificate continuity verified.
- All 27 tests passed; debug and release lint each completed with zero errors and 42 warnings.
- APK ZIP alignment and bundled 64-bit native load-segment alignments passed 16 KB checks.

## [0.3.1] - 2026-10-19

### Fixed

- "Background task failed" and the earlier force close a few seconds after touching the bubble: the bubble's edge-snap, idle-peek and aurora animations ran without a Compose frame clock (`MonotonicFrameClock is not available`). They now run on `AndroidUiDispatcher.Main`. Found from the in-app crash report on a Xiaomi 23049PCD8I, Android 15.
- Tests now fail on any error the bubble records, and cover the idle peek, Circle to Search from the bubble, and dropping the bubble on the dismiss target.

### Added

- Chat-head style dismiss target: drag the bubble to the X at the bottom of the screen to hide it, with a magnetic snap and haptics.
- Google Circle to Search with StashDrop as the digital assistant: Settings → Digital assistant → Connect (Shizuku) routes the corner swipe / long-press home to StashDrop while keeping Google's voice service on, so StashDrop opens Google Circle to Search.

## [0.3.0] - 2026-10-19

### Changed

- Circle to Search opens **Google's own Circle to Search** again by default. The request now goes from StashDrop's own process to the system voice-interaction service with the contextual-search entry point, the same request a long-press of the navigation handle sends, instead of the earlier Shizuku-shell bridge that ended up in Gemini. It needs Google as the digital assistant; StashDrop's built-in circle screen is the fallback and a switch in Settings.
- Settings has a Circle to Search section (Google on/off, readiness, "Try it now") and a Digital assistant section that opens the system picker so StashDrop or Google can be chosen by hand; with StashDrop chosen, the corner swipe or long-press home opens StashDrop's circle screen.
- Bottom gesture strips (swipe up to Circle to Search) are on by default.

### Fixed

- Every floating-bubble touch, drag, shelf and drag-and-drop path runs guarded; a failure is shown and recorded instead of closing the app. The foreground service starts with its explicit special-use type.
- Circle to Search no longer recycles bitmaps that are still on screen, which could crash when circling again or capturing twice.
- Back closes the floating shelf.
- Auto-stash no longer saves the same copied text twice.

### Added

- Crash reporter: if StashDrop closes unexpectedly, the next launch shows the report with a Copy button. Release stack traces keep line numbers, and CI keeps the R8 mapping file with the build artifacts.
- Robolectric tests that tap, hold and drag the bubble, open the shelf, and circle/reset in Circle to Search with native graphics.

## [0.2.0] - 2026-10-19

### Fixed

- Starting the floating bubble no longer force-closes the app. Overlay windows were software-rendered and the dock's AGSL shader threw `Software rendering doesn't support RuntimeShader`; overlays now request hardware acceleration and shaders fall back to Canvas drawing on software canvases.
- Swiping up (and every other trigger) opens StashDrop's own Circle to Search instead of handing off to Gemini or the system assistant.
- Moving the bubble no longer launches search when you drag it inward or downward, and it can be placed anywhere from 12% to 85% of the screen height.

### Added

- Default digital assistant support: StashDrop can hold the assistant role so long-pressing home opens Circle to Search. Set or restore it from Settings (one tap with Shizuku).
- Settings → Setup shows overlay, notification, Shizuku, screenshot and assistant status with one-tap fixes, plus "Set up everything with Shizuku".
- Haptic feedback while dragging the bubble, drawing a circle, selecting and stacking cards, and on drops and search launch, with an in-app toggle.
- Circle to Search actions: Google search of circled text, Lens image search, copy, stash, and share in one row; a Circle to Search button in the floating shelf and the bubble's notification.
- Robolectric regression test that starts the floating service and draws every overlay with native graphics.

### Changed

- Circle Studio is merged into Circle to Search; the native Google/Gemini bridge is removed.
- New round floating bubble with gradient rim, mint count badge, and drop glow.
- Titles, labels, and list rows sit on a single line; Controls is now Settings.
- Android version name is `0.2.0` and version code is `3`.

## [0.1.1] - 2026-10-19

### Added

- GitHub Actions debug checks for pushes/pull requests and signed release publication for version tags.
- Encrypted Actions signing-secret setup helper, using the existing private production key.
- Release certificate continuity, tag/version consistency, signature, checksum, and 16 KB alignment checks before CI publication.
- Version 0.1.1 release notes and public build verification report.

### Fixed

- Taps inside the floating shelf no longer fall through to its dismiss backdrop.
- Circle Studio reuses the saved cutout for Stash, Lens, and Share instead of inserting duplicate copies.
- The floating overlay is restored on screen-on when no keyguard is active.
- Deleting the last item in a selected collection restores the Shelf's All filter.

### Changed

- Android version name is `0.1.1` and version code is `2`.
- Production signing continues to use the same certificate as the prepared 0.1.0 build.
- Release workflows publish the signed APK/AAB, public certificate, checksums, and verification report, and remove private signing files even when a build fails.

### Verified

- Signed 0.1.1 APK and AAB built locally with the original RSA-4096 certificate; signatures and matching fingerprints verified.
- All 19 logic/visual tests passed; debug and release lint each completed with zero errors and 29 warnings.
- APK ZIP alignment and bundled 64-bit native load-segment alignments passed 16 KB checks.
- GitHub Actions workflow passed `actionlint`; signing restoration preserved the key and owner-only permissions and rejected missing secrets.
- These checks establish the local build results; GitHub publication and phone behavior require their own verification.

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
