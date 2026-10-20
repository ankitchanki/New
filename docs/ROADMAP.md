# Improvements and future UI

These are **proposed directions**, not features shipped in 0.3.2. There are no committed delivery dates or promised version numbers. Current behavior is described in the [feature guide](FEATURE_GUIDE.md); implemented visuals are in the [gallery](UI_GALLERY.md).

The goal is a shelf that is easy to reach, predictable when permissions or services change, and safe for useful personal material. Reliability and accessibility should guide visual changes.

## Current foundation

The app already has Home / Shelf / Settings, a draggable edge-peeking bubble, a stacked floating shelf, collections, sharing, optional copied-text saving, Google search routing, a built-in capture/lasso/OCR flow, local crash reports, and spring/shader motion. The signed 0.3.2 build preserves the original production certificate. Host tests and renders are available; broad real-device qualification is still outstanding.

## Priorities

| Priority | Problem today | Proposed improvement | Acceptance evidence |
| --- | --- | --- | --- |
| 1 — Reliability | Overlay, capture, and assistant behavior vary by Android/OEM | Test launch/dismiss/reconnect/reboot, permission denial, lock/unlock, rotation, and service death; surface actionable errors | Recorded device matrix with reproducible results and regression coverage for discovered bugs |
| 1 — Setup clarity | A connected bridge or accepted Google request can look more conclusive than it is | Show separate overlay, capture, Google-service, and clipboard readiness; identify the active route | A first-time user can start a shelf and complete built-in capture without guessing |
| 1 — Accessibility | Dense one-line settings and visual motion can make actions hard to use | Support large text, TalkBack labels/focus, 48 dp targets, reduced motion, and higher contrast | Manual accessibility checks at large font/display sizes; readable labels and reachable actions |
| 2 — Shelf management | Larger collections become difficult to navigate | Local text search, type filters, richer previews, selection counts, and explicit batch actions | Find and share an item from a large fixture without losing selection or collection context |
| 2 — Safer retention | Deletion is immediate and no archive restore exists | Undo/trash policy, storage usage, sensitive-copy controls, explicit local export/import | Recovery and round-trip restore checks; clear handling of private files, duplicates, and canceled imports |
| 2 — Capture quality | Latin OCR and manual cropping limit some content | Better crop handles, result editing, language/script options, and clearer barcode actions | Quality checks on representative images/scripts; explain download/offline behavior if models change |
| 3 — Visual refinement | Current dark styling and motion offer limited customization | Adaptive themes, subtle glass, adjustable bubble size/edge behavior, responsive layouts | Device screenshots and frame/memory/battery measurements alongside accessible fallbacks |

## Proposed screen direction

An [interactive SVG concept and nine-screen vector overview](design/README.md) demonstrate the existing flows with proposed layout refinements. The concept simulates platform services; its screen designs have not been implemented in the Android app.

Keep **Home**, **Shelf**, and **Settings** as stable destinations. Place frequent actions in the lower reachable area and reserve decorative effects for moments that explain an interaction.

### Home: readiness and one clear next step

Show a compact bubble state, recent items, and a primary action that changes between **Allow bubble**, **Show bubble**, and **Open shelf** as appropriate. Add a small search-mode summary such as **Built-in · capture consent needed** or **Google service ready · eligibility decided by Google**. Separate optional setup suggestions from the action needed to start using the app.

A future readiness card could explain why an action is blocked, link to its system setting, and refresh after returning to the app. Success should mean the capability was checked, rather than simply that a settings command returned.

### Shelf: easier scanning and safer actions

Add a local search field, collection/type filters, image thumbnails, readable link domains, and attachment names/sizes where available. Use a sticky selection toolbar near the bottom with a visible count and Share / Move / Delete. Add an undo affordance or a documented trash retention policy before batch deletion.

Empty states should explain how to share into StashDrop and offer Add note. Loading, import failure, and unavailable-file states should retain the user's context. Search/filter/selection state should survive navigation and rotation where appropriate. A local export/import design must specify private-file handling, encryption choices, and recovery before implementation.

### Floating shelf: reach and feedback

Explore adjustable bubble size, left/right handed placement, configurable edge peek, and a stable quick-action arrangement. Make selection distinguishable through shape and labels as well as color. Improve drop-to-dismiss feedback without implying that another app accepted a global drag.

A larger preview sheet could expose filename, text, and collection before sharing. Any new overlay must be tested against navigation gestures, keyboards, cutouts, rotation, and locked-screen behavior. Avoid expanding touch regions that steal controls from the foreground app.

### Circle screen: explain the selected content

Keep the capture visible while making selection bounds and the next action clearer. Proposed additions include draggable crop handles, editable extracted text, distinct text/barcode results, copy confirmation, and progress/cancel/retry states. Explain when Lens or Share will also save a cutout so retention is predictable.

Offer an obvious switch between Google's route and the built-in workflow. A native request that opens Gemini needs a clear user recovery path because the app cannot reliably inspect Google's resulting screen.

### Settings: capability groups and honest status

Group access into **Essential for bubble**, **Capture options**, and **Optional integrations**. Show Shizuku authorization separately from its running/connected service, assistant role separately from Google's voice service, and clipboard monitoring separately from its preference switch. Explain the consequence of changing assistant settings and how to return to the system picker.

Keep copied-text saving visibly opt-in and explain that it can retain passwords or codes. Add concise storage/privacy controls when their behavior is implemented, rather than displaying controls with no working effect.

## Visual language and interaction

| Area | Current foundation | Proposed direction |
| --- | --- | --- |
| Color | Dark navy surfaces, periwinkle and mint accents | Light/dark and adaptive color options with checked contrast |
| Glass | App-local specular/blur effects | Subtle depth with opaque/high-contrast fallback; preserve text readability |
| Motion | Tuned springs, aurora pull, metaball field | Reduced-motion mode, system animation preference handling, measured frame pacing |
| Cards | Stacked shelf and collection labels | Better previews, consistent type icons, visible selection and failure states |
| Layout | Phone-oriented Compose screens | Large-font wrapping, landscape, tablets, foldables, keyboard and switch access |
| Haptics | Progressive pull ticks and confirmation | Consistent event mapping, hardware-aware intensity, independent preference |

Any future mockup must be labeled as a concept. Keep implemented component renders, real-device captures, and promotional artwork separately identified. New visual assets should be generated from the implemented screens once the change is built and verified.

## Technical improvements worth investigating

- **Capture and lifecycle:** cancellation, timeouts, interrupted consent, large screenshots, binder death, and stale selection state. Measure memory use with repeated crops and ML Kit sessions.
- **Background operation:** clarify foreground-service/notification states, reconnect after Shizuku restarts, and document OEM battery restrictions. Capture and clipboard denial need usable fallbacks.
- **Persistence:** plan explicit Room migrations before schema changes; design any backup/restore format before adding it. Do not replace the production signing key during unrelated work.
- **Sharing:** validate text, images, files, mixed selections, canceled shares, URI-grant lifetime, and receiver rejection. Prefer standard sharing when drag behavior is unreliable.
- **Build quality:** reduce the documented lint warning set without suppressing useful rules; keep certificate, checksum, TLS, shrinking, and alignment verification intact.
- **Performance:** profile startup, bubble drag, shelf opening, capture, recognition, and long lists on representative low/mid/high-range devices. Record refresh rate and frame-time evidence before advertising performance.

## Device qualification targets

Test representative devices across supported Android versions and manufacturers, including Pixel, Samsung One UI, Xiaomi/HyperOS, and other available OEMs. This is a target matrix; it is not a compatibility certification. Cover gesture and three-button navigation, Shizuku unavailable/connected/restarted, accessibility off/on, projection canceled/approved, Google eligible/unavailable, TalkBack, large fonts, and low-memory conditions.

Use [DEVICE_VALIDATION.md](DEVICE_VALIDATION.md) to record actual results. A proposed improvement becomes a shipped claim only after implementation, appropriate automated checks, device evidence for platform-sensitive behavior, updated screenshots/help, and release notes describing its actual scope.

## Constraints to preserve

Google controls native Circle to Search eligibility. Protected content cannot be captured by bypassing Android security. External apps own their drop targets and animation. App-local glass does not provide universal cross-app backdrop blur. Offline OCR script coverage depends on bundled/selected models. An accepted privileged command alone cannot prove an end-to-end workflow.

Keep the local-first shelf usable without an account or backend key, preserve consent and explicit external sharing, and keep signing credentials outside Git. These constraints should shape future designs and implementation choices.
