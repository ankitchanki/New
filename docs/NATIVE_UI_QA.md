# Native SVG-design implementation: 0.4.0

The browser SVG concept is implemented as native Compose screens and interactions in version 0.4.0. Layouts adapt to Android system insets, scrolling, typography, and touch targets. The browser's sample permission/recognition/share results remain confined to the separate design prototype.

## What was checked

- Reviewed the original SVG studio/overview and the current production Compose renders of Home, Shelf, Settings, floating shelf, and the new interaction settings page.
- Confirmed dark navy/periwinkle/mint tokens, rounded surface/card hierarchy, selection affordances, reachable sharing actions, navigation, and wrapping access descriptions.
- Regenerated eight Paparazzi baselines and copied them byte-for-byte into the README gallery with dimensions/checksums recorded in its manifest.
- Added four Robolectric/Compose UI tests exercising actual composables and callbacks: blank/valid note entry; multi-item sharing and collection moves; deletion confirmation/cancellation; and sensitive clipboard opt-in/cancellation.
- Retained the existing native-graphics overlay/circle regression suite. The circle results panel is height-bounded and scrollable so result actions remain reachable when space is constrained.
- Release verification covers tests, debug/release lint, production certificate continuity, APK/AAB signatures, shrinking, and ZIP/native-library alignment. See [the exact release report](releases/v0.4.0-verification.json).

## Actual integration boundaries

Dashboard callbacks write through the existing repository and preferences and invoke Android sharing. Batch operations use the current selected items; storage errors surface through the dashboard message. Changing a collection filter clears selection. Permanent deletion has confirmation but no trash or undo.

The floating shelf keeps its existing global drag, service lifecycle, and Shizuku/capture routing. Its deletion confirmation is rendered inside the overlay rather than opening an Activity-only dialog from the service. Native capture/OCR and Google eligibility behavior remain platform-dependent.

## Qualification limits

These checks ran on the host with Paparazzi and Robolectric native graphics. No physical Android device or emulator was attached. TalkBack, larger font/display settings, OEM service policies, system consent, Google eligibility, real WhatsApp/file drops, and phone performance still require the [device checklist](DEVICE_VALIDATION.md). A host render or successful native unit test does not certify those behaviors.

The interactive SVG is a design/reference artifact. The APK uses actual Android services and local data; it does not bundle the prototype's sample content, scripted permission grants, or fixed OCR output.
