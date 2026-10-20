# Device validation

Use the [Windows install/log helper](WINDOWS_DEVICE_TESTING.md) on the laptop actually connected to the phone.

Cloud builds and JVM tests cannot prove Android overlay, privileged Binder, shader rendering, or cross-app behavior. No physical Android device or accelerated emulator is attached to the current cloud machine.

Run these checks on API 28, 30, 33, and 35+ devices, with gesture and three-button navigation and at least one Samsung ROM:

- Fresh install: the dashboard opens without Shizuku; denied notifications and overlay permission do not crash it. Returning from overlay settings updates the button.
- Overlay lifecycle: start/stop repeatedly, use the notification Stop action, rotate, lock/unlock, revoke overlay permission, and kill the app. No orphan window or clipboard listener should remain.
- Dock: tap opens the shelf, hold starts Circle to Search, moving only repositions it (edge snap/peek, haptic detents). Verify touches outside the dock and gesture strips reach the underlying app on Android 12+.
- Gestures: enable modes separately and together. Verify threshold, diagonal pulls, canceled drags, system-timed hold, and double-tap. Check fullscreen and landscape behavior; disable strips if they interfere with an OEM's navigation.
- Assistant: set StashDrop as default assistant, long-press home, and confirm Circle to Search opens; restore the previous assistant.
- Shelf: add text and files via Share, restart the process, move collections, select multiple items, delete, and verify the private backing file is removed. Test oversized and revoked URI imports.
- Sharing: single image, multiple images, plain text, and mixed selection to several apps, with and without WhatsApp installed. Cancel the chooser and reject a drag; source items must remain. Confirm no invisible full-screen window blocks the target after a drag.
- Clipboard: off means no storage. With opt-in, test foreground and background copy, duplicate notifications, sensitive-text awareness, Shizuku restarts, and stopping the service. Confirm restrictions are visible when an OEM denies clipboard reads.
- Native search: test supported and unsupported Google/ROM combinations; verify whether the requested interface actually opens. Switching assistant requires an explicit button action; restore must recover previous values. Do not equate Binder success with functional search.
- Graphics/accessibility: AGSL compilation/rendering on 33+, Canvas fallback on 28–32, unavailable haptic primitives, font scaling, TalkBack labels, narrow screens, and RTL. Profile real frame timing and memory with large screenshots and attachments before claiming 120 Hz.

Release work also includes signing, privacy/store disclosures, foreground-service policy review, an accessibility declaration where distributed through Play, and device performance/stability qualification.

## Google-only search and marker checks (0.5.0)

- Select Google as digital assistant and enable native Circle to Search where the device offers it. Test button, edge handle hold, notification and optional StashDrop assistant entry with Shizuku connected/disconnected.
- Record whether Google shows Circle to Search, Gemini, no UI, or a rejection message. An accepted Binder request alone is not success. No StashDrop capture, lasso or consent screen should appear, including after updating from old Google-mode-off preferences.
- Show the edge handle and enable each marker gesture. Test center upward pull/flick, both diagonal up/inward corner swipes, center long-press and nearby double-tap. Each should request search once. Single tap, expired/distant taps, horizontal/outward/downward swipes, cancellation and multi-touch must not trigger search.
- Check markers above three-button and gesture navigation, after preference changes, rotation, lock/unlock, keyboard open/close and service restart. They must not cover or replace system navigation.
- Use [Windows ADB installation and logging](WINDOWS_DEVICE_TESTING.md). Review logs before sharing; cloud tests do not validate the connected tablet.

## Current user device: POCO F5 / Android 15

The user reported this model/OS; physical installation and gesture/Google outcomes have **not** been verified by the cloud. Android 15 alone does not establish Circle to Search eligibility.

1. Update the Google app. In Android Settings → Apps → Default apps → Digital assistant app, choose Google and enable **Use screen and app data**. Menu names can vary by ROM. See [Google’s setup guide](https://support.google.com/websearch/answer/14508957?hl=en).
2. Search phone Settings for **Circle to Search** and enable it if offered. Record if the setting is absent; do not infer native support from Android version alone.
3. Allow StashDrop’s overlay access and show the edge handle. If the service stops on HyperOS, check the app’s background/battery restrictions and available background pop-up permission.
4. With the keyboard closed, enable the desired marker gestures individually. Use center for swipe/hold/double-tap and the sides for diagonal up/inward corner swipes. Test with and without authorized Shizuku.
5. Use the Windows helper to install the official compatible update and capture logs. Record whether each request shows Google Circle to Search, Gemini, rejection guidance or no UI. None should open an internal StashDrop capture screen.

A local ADB connection on the Windows laptop is not automatically reachable from the cloud. Device logs must be captured there and reviewed before sharing.

## 0.6.0 input, panel and media regressions

- With handles enabled and panel closed, scroll/tap other apps across the entire display, especially the lower portion previously blocked. Repeat during aurora animation and after dismissing the panel.
- Open the panel from each edge using tap and inward pull, close with Back/Close/outside touch, and verify the outside touch reaches the other app. Vertical reposition, cancel and drag-to-X must remain distinct.
- Test raised corner markers above the navigation region; separately confirm physical bottom swipes still invoke system navigation. Cover gesture and three-button navigation, portrait/landscape, keyboard and lock/unlock.
- Import recognizably different images, EXIF-rotated photos, GIF/WebP stickers and supported/unsupported videos. Check inline thumbnails, missing/corrupt media placeholders, rapid scrolling and memory on a large shelf.
- Combine collections with all type chips. Check URLs embedded in prose, OTP-like text, ordinary numerical notes, sticker heuristics and the Images/Photos/Stickers collection overrides. Changing filters must clear selection without deleting content.
