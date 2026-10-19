# Device validation

Cloud builds and JVM tests cannot prove Android overlay, privileged Binder, shader rendering, or cross-app behavior. No physical Android device or accelerated emulator is attached to the current cloud machine.

Run these checks on API 28, 30, 33, and 35+ devices, with gesture and three-button navigation and at least one Samsung ROM:

- Fresh install: the dashboard opens without Shizuku; denied notifications and overlay permission do not crash it. Returning from overlay settings updates the button.
- Overlay lifecycle: start/stop repeatedly, use the notification Stop action, rotate, lock/unlock, revoke overlay permission, and kill the app. No orphan window or clipboard listener should remain.
- Dock: tap opens the shelf, hold starts Circle to Search, moving only repositions it (edge snap/peek, haptic detents). Verify touches outside the dock and gesture strips reach the underlying app on Android 12+.
- Gestures: enable modes separately and together. Verify threshold, diagonal pulls, canceled drags, 400 ms hold, and double-tap. Check fullscreen and landscape behavior; disable strips if they interfere with an OEM's navigation.
- Capture: test Shizuku grant/deny/death/reconnect, accessibility screenshots on 30+, MediaProjection accept/cancel, timeout, rotation, and protected content. An OS denial must never be described as a successful screenshot.
- Assistant: set StashDrop as default assistant, long-press home, and confirm Circle to Search opens; restore the previous assistant.
- Circle to Search: lasso a small/large area, a Latin text block, QR, and barcode. Confirm mask/crop alignment, copying, privacy sanitation, image persistence, and share grants. Rotate during selection and OCR.
- Shelf: add text and files via Share, restart the process, move collections, select multiple items, delete, and verify the private backing file is removed. Test oversized and revoked URI imports.
- Sharing: single image, multiple images, plain text, and mixed selection to several apps, with and without WhatsApp installed. Cancel the chooser and reject a drag; source items must remain. Confirm no invisible full-screen window blocks the target after a drag.
- Clipboard: off means no storage. With opt-in, test foreground and background copy, duplicate notifications, sensitive-text awareness, Shizuku restarts, and stopping the service. Confirm restrictions are visible when an OEM denies clipboard reads.
- Native search: test supported and unsupported Google/ROM combinations; verify whether the requested interface actually opens. Switching assistant requires an explicit button action; restore must recover previous values. Do not equate Binder success with functional search.
- Graphics/accessibility: AGSL compilation/rendering on 33+, Canvas fallback on 28–32, unavailable haptic primitives, font scaling, TalkBack labels, narrow screens, and RTL. Profile real frame timing and memory with large screenshots and attachments before claiming 120 Hz.

Release work also includes signing, privacy/store disclosures, foreground-service policy review, an accessibility declaration where distributed through Play, and device performance/stability qualification.
