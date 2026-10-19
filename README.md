# StashDrop

A native Kotlin / Jetpack Compose productivity shelf for Android 9+ (min SDK 28, target SDK 35). Package: `com.vibe.stashdrop`.

## Build

Use JDK 17 or newer, Android SDK platform 35, and build-tools 35.0.0. Point `ANDROID_HOME` at your SDK or set `sdk.dir` in the ignored `local.properties`.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it with `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Release builds require your own signing configuration; no signing credentials are included.

## Use

1. Open StashDrop, allow overlay access, return, and start the floating shelf. The notification's Stop action removes the overlays and clipboard listener.
2. Tap the dock to open the shelf. Drag it to reposition; release to snap to the nearest edge. It peeks after three seconds. Swipe inward or hold to search; drag down to invoke Studio.
3. Share text, images, or files **to StashDrop** from another app, add notes in the dashboard, or stash a Circle Studio cutout. Collections and deletion are available in the dashboard's Shelf tab.
4. Studio tries Shizuku, then an enabled accessibility screenshot service on API 30+, then asks for one-shot screen-sharing consent. Draw a closed lasso; the selected pixels are masked to the lasso path. Text recognition (Latin script) and barcode scanning use bundled ML Kit models. Copy results, stash the image, share it, or hand it to a compatible image-search app.
5. Select multiple shelf cards and hold one to initiate an Android global drag. Android renders the stack shadow over the receiving app. Use WhatsApp or Share when an app doesn't accept drops or mixed content.
6. Optional: connect Shizuku in Controls. The user must install/start Shizuku separately and authorize StashDrop. Auto-stash is off by default and operates while the floating service is running. Android/OEM background clipboard restrictions can still apply.
7. Optional: enable individual bottom gesture strips. They sit above system navigation, consume touches in their small regions, and are disabled by default. They cannot intercept the real system home button or replay touches to other apps.

## Architecture

`core/` contains Shizuku lifecycle and shell user service, screen capture, AGSL shaders with pre-33 Canvas fallback, spring specs, cubic trajectory math, and haptics. `features/` contains the dock, global drag shadow, Studio, Room shelf, DataStore preferences, URL sanitation, share receiver, and opt-in clipboard listener. `ui/` contains the theme and dashboard/playground.

All application data is stored in the app's private directory. Files are exposed only through a FileProvider URI with transient read grants. URI imports are limited to 50 MB per attachment and 30 attachments per incoming operation. URL query sanitation preserves raw percent encoding and removes only configured tracking keys. Clipboard values are not logged. The history table retains at most 100 SHA-256 digests and does not duplicate the clipboard payload.

## Platform boundaries

- Native Google Circle to Search is a best-effort hidden-API request. Android version alone does not establish support. Google's package, ROM permissions, app version, and server/device eligibility matter. A rejected invocation routes to Studio. A Binder call's success cannot prove that Google displayed the requested visual interface.
- Switching the default assistant is an explicit Controls action through Shizuku, with previous values saved for restoration. No assistant setting is changed during app startup. No private API can guarantee universal “zero-settings” Google support.
- A normal app cannot capture `FLAG_SECURE`, DRM, or other protected content. Capture is asynchronous, not zero latency. No `SurfaceControl` signature-permission bypass is claimed.
- Android 9–10 require Shizuku capture or MediaProjection consent. Accessibility screenshots require API 30+, user enablement, and a permitted target window.
- Background clipboard availability varies by Android release and OEM. The shell user service attempts the platform listener under shell identity; failed access does not silently claim success. The regular app listener only works where Android permits foreground clipboard access. File/image clipboard monitoring is not implemented; use Share-to-StashDrop for URI grants.
- Global drag/drop acceptance belongs to the receiver. Mixed text/file shares may be ignored by WhatsApp. An overlay cannot request Activity drag permissions for incoming foreign file URIs; text drops work, and files should be shared to StashDrop. The app retains shelf items after outbound drags.
- Android does not expose other apps' global drag locations or drop-target bounds to the source app. The framework stack shadow follows the drag; the app does not fabricate a landing in WhatsApp. The Bezier/ripple flight is used for in-app stash feedback.
- Gesture strips are best-effort overlays, not system navigation replacements. Landscape hides them; immersive visibility follows delivered navigation insets. OEMs may omit these insets. No DRM/game classification is claimed.
- AGSL is used on API 33+; older versions use Canvas gradients/paths. RenderEffect softens the dock's own shader on API 31+; overlays cannot sample arbitrary apps' pixels for true backdrop glass. The app requests 120 Hz where available; display hardware, power mode, and frame cost determine actual refresh rate. Performance is not benchmarked here.

See [device validation](docs/DEVICE_VALIDATION.md) before using this as a release candidate. This is an initial implementation, not a device-certified production release.
