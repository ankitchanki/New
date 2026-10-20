# Platform support

StashDrop 0.5.0 supports Android 9+ (API 28), targeting API 35. The shelf works independently of Google’s native feature availability.

| Feature | Requirements and limits |
| --- | --- |
| Floating shelf | Overlay permission and foreground service; OEM battery rules apply |
| Google Circle to Search | Google app and active Google voice service; Google/device/account eligibility and phone navigation settings control the UI |
| Privileged Google request | Running authorized Shizuku; shell voice-interaction access can still be rejected by an OEM |
| App Google request | Best effort without Shizuku; often rejected without a valid session token |
| Gesture markers | Floating service running, strips and relevant trigger enabled, portrait/unlocked/keyboard hidden |
| System corner/assistant gestures | OS navigation mode, assistant selection and OEM support; distinct from StashDrop’s markers |
| Clipboard | Opt-in; Shizuku or permitted foreground access, subject to platform restrictions |
| Global drag | Destination must accept content and URI grants; use Share when unsupported |
| Shaders | AGSL on API 33+, Canvas fallbacks below; no guaranteed frame rate |

## Google-only search

The internal screenshot, lasso, OCR/barcode and MediaProjection workflows were removed in 0.5.0. All triggers request Google. Rejected requests show setup guidance. An accepted request may show Gemini/assistant; StashDrop cannot detect or override Google’s choice. Installing the app cannot unlock Circle to Search on unsupported phones or tablets.

Choose Google in the system assistant picker and enable native Circle to Search in navigation settings if available. Connect Shizuku if app-level requests are blocked. Advanced assistant routing through StashDrop is optional and device dependent. See the [feature guide](FEATURE_GUIDE.md#digital-assistant-and-shizuku).

## Permissions and privacy

Overlay access enables the bubble/markers, notifications expose foreground-service controls, vibration supplies haptics, and optional Shizuku supports explicit setup, Google requests and clipboard monitoring. StashDrop no longer declares screenshot accessibility or media-projection services. No account or backend API key is needed for the local shelf. Google and sharing receivers apply their own policies.

No physical-device compatibility is implied by host tests. Use the [Windows ADB helper](WINDOWS_DEVICE_TESTING.md) and [device checklist](DEVICE_VALIDATION.md) to validate an actual phone/tablet. The cloud cannot access a laptop’s local ADB connection merely because laptop and tablet share Wi-Fi.
