# Platform support

StashDrop targets Android 9+ with different capabilities depending on OS permissions, hardware, and receiving apps.


- Native Google Circle to Search is a best-effort hidden-API request. Android version alone does not establish support. Google's package, ROM permissions, app version, and server/device eligibility matter. A rejected invocation routes to Studio. A Binder call's success cannot prove that Google displayed the requested visual interface.
- Switching the default assistant is an explicit Controls action through Shizuku, with previous values saved for restoration. No assistant setting is changed during app startup. No private API can guarantee universal “zero-settings” Google support.
- A normal app cannot capture `FLAG_SECURE`, DRM, or other protected content. Capture is asynchronous, not zero latency. No `SurfaceControl` signature-permission bypass is claimed.
- Android 9–10 require Shizuku capture or MediaProjection consent. Accessibility screenshots require API 30+, user enablement, and a permitted target window.
- Background clipboard availability varies by Android release and OEM. The shell user service attempts the platform listener under shell identity; failed access does not silently claim success. The regular app listener only works where Android permits foreground clipboard access. File/image clipboard monitoring is not implemented; use Share-to-StashDrop for URI grants.
- Global drag/drop acceptance belongs to the receiver. Mixed text/file shares may be ignored by WhatsApp. An overlay cannot request Activity drag permissions for incoming foreign file URIs; text drops work, and files should be shared to StashDrop. The app retains shelf items after outbound drags.
- Android does not expose other apps' global drag locations or drop-target bounds to the source app. The framework stack shadow follows the drag; the app does not fabricate a landing in WhatsApp. The Bezier/ripple flight is used for in-app stash feedback.
- Gesture strips are best-effort overlays, not system navigation replacements. Landscape hides them; immersive visibility follows delivered navigation insets. OEMs may omit these insets. No DRM/game classification is claimed.
- AGSL is used on API 33+; older versions use Canvas gradients/paths. RenderEffect softens the dock's own shader on API 31+; overlays cannot sample arbitrary apps' pixels for true backdrop glass. The app requests 120 Hz where available; display hardware, power mode, and frame cost determine actual refresh rate. Performance is not benchmarked here.


## Permission map

| Access | Used for | Required? |
| --- | --- | --- |
| Display over other apps | Floating dock and opt-in gesture strips | For the floating shelf |
| Notifications | Foreground shelf/capture status and Stop action | Requested on Android 13+ |
| Shizuku authorization | Shell capture, best-effort native Google search, assistant settings, background text clipboard | Optional |
| Accessibility screenshot capability | One-shot screenshot on API 30+ | Optional alternative to Shizuku |
| MediaProjection consent | One-shot screen capture when other routes are unavailable | Per capture fallback |
| URI read grants | Importing/sharing selected attachments | Granted through Share / FileProvider |

There is no required backend account or API key for the bundled Latin-script OCR and barcode workflow.

See [the device validation checklist](DEVICE_VALIDATION.md) for checks still required on real devices.
