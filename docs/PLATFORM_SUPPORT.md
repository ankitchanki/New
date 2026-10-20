# Platform support and fallbacks

StashDrop 0.3.2 supports **Android 9+ (API 28)** and targets API 35. Feature availability depends on permissions, system services, Google eligibility, OEM restrictions, and the receiving app. See the [feature guide](FEATURE_GUIDE.md) for usage and the [device checklist](DEVICE_VALIDATION.md) for hardware qualification.

## Android version boundaries

| Capability | Boundary and fallback |
| --- | --- |
| Local notes, clean links, collections, normal sharing | Available across supported versions; readable attachment URIs and compatible receivers are required |
| Floating bubble and gesture strips | Overlay access and a running foreground service; OEM background/battery policies can affect persistence |
| Shizuku integration | Optional running/authorized Shizuku with connected user service; supported shell operations still vary by system |
| Accessibility screenshot | Android 11+ (API 30), with the screenshot service explicitly enabled |
| Built-in capture on Android 9–10 | Shizuku capture where permitted, otherwise MediaProjection consent |
| MediaProjection | Android-controlled screen-sharing consent and capture service; cancel/denial prevents capture |
| Assistant role management | Android 10+ (API 29); assistant gesture availability depends on OEM and navigation mode |
| AGSL runtime shaders | Android 13+ (API 33); Canvas fallbacks on older supported versions |
| RenderEffect blur | Android 12+ (API 31), applied to app-rendered content |
| Notification permission | Android 13+ runtime permission; foreground-service behavior also follows the platform's rules |

## Native Google search versus built-in selection

The native route requests a contextual session through Android's voice-interaction service. It needs Google's app and active voice service; Google decides whether Circle to Search is available. A successful request may open Gemini/the assistant instead. Turn off **Use Google Circle to Search** to force StashDrop's built-in capture, lasso, OCR/barcode, and result actions.

The usual native setup is Google as the digital assistant. Alternatively, on supported systems, an explicit Shizuku **Connect** action can keep Google's voice service active while assistant presses enter StashDrop. Selecting StashDrop manually without that connection can lead to the built-in route. This is best-effort system integration and requires device validation.

Assistant role changes occur through explicit Settings actions, with a saved previous role holder for restoration when available. App startup does not automatically replace the assistant. System home/power controls are not universally interceptable.

## Permissions and data access

| Access | Used for | Alternative or limit |
| --- | --- | --- |
| Display over other apps | Bubble, floating shelf, gesture touch regions | The dashboard shelf and normal share receiver remain usable without the overlay |
| Notifications | Foreground-service visibility and actions | Permission handling follows the Android version; check system notification settings |
| Shizuku runtime permission | Shell capture, clipboard listener, supported setup and assistant operations | Manual settings, foreground clipboard access where permitted, and consent-based capture |
| Accessibility screenshot service | Capture on API 30+ | Shizuku or MediaProjection; requires explicit enablement |
| MediaProjection consent | Capture when direct routes cannot provide a frame | Canceling leaves capture unavailable; no protected-content bypass |
| Incoming content URI grant | Copy a shared file into private storage | Sender must provide readable content; 50 MB per attachment and 30 per shared batch |
| Outgoing temporary URI grants | Share or drag selected attachments | Receiver must accept the MIME types and URI permissions |

No app account or backend API key is needed. Latin-script OCR and barcode processing use bundled on-device ML Kit models. Browser search, Google/Lens, and sharing are external actions whose receiving apps may use the network.

## Boundaries that permissions cannot remove

- **Protected screens:** no bypass for `FLAG_SECURE`, DRM surfaces, or system capture denial. Capture is asynchronous, with no zero-latency guarantee.
- **Clipboard:** true background access depends on shell/OEM policy. Text monitoring is opt-in and off by default. File/image clipboard history is not implemented; use Android Share for attachments.
- **External drag:** the receiver owns MIME acceptance and drop handling. An overlay cannot obtain an Activity's drag permissions for arbitrary foreign file drops; text drops and normal sharing have different capabilities.
- **Cross-app landing:** the app cannot inspect another app's internal drop bounds or guarantee a WhatsApp landing animation. Its flight overlay is local feedback.
- **Gesture regions:** strips hide in landscape and while locked; immersive-mode/inset behavior is best effort. They can interfere with other controls, so the Settings toggle remains important.
- **Glass and performance:** blur affects the app's own rendered content; general blur of arbitrary apps is unavailable. High-refresh requests and host shader renders are not performance benchmarks.
- **Data recovery:** there is no in-app archive restore, cloud sync, or deletion undo. Android app backup is disabled. Share out important items before uninstalling.

The signed release passed build/test/signature/alignment checks. Broad phone compatibility and performance still need documented results; see [verification](VERIFICATION.md) and the [future work](ROADMAP.md).
