# StashDrop feature guide

This guide describes **released version 0.5.0**. See the [README](../README.md) for the signed APK, [platform support](PLATFORM_SUPPORT.md) for Android restrictions, and the [roadmap](ROADMAP.md) for proposed improvements.

## What StashDrop does

StashDrop keeps useful material in a local shelf and brings that shelf close to your thumb through a floating bubble. Save a link while browsing, request Google screen search, organize a few notes, then share selected items to another app. Every search entry point requests Google; there is no built-in search screen.

## Home, Shelf, and Settings

| Screen | Current purpose | Typical action |
| --- | --- | --- |
| Home | Bubble status, show/hide control, recent saved items | Start the floating shelf and return to another app |
| Shelf | Saved notes, links, attachments, collection filters | Add a note, move an item, share, or delete |
| Settings | Access/setup status, Google search status, and expandable assistant integration | Configure only the access you need |
| Gestures, clipboard & feel | Dedicated interaction preferences, reached from Settings | Tune gestures, clipboard, haptics, and springiness |

The [visual gallery](UI_GALLERY.md) shows the implemented components with sample content. Its images are host renders; actual device permission screens come from Android.

## Floating bubble and shelf

1. Allow **Floating bubble** in Settings → Setup, then tap **Show floating bubble** on Home.
2. Tap the bubble to open the floating shelf. Hold the bubble to request Google Circle to Search.
3. Drag the bubble to a comfortable position. It snaps toward the edge and partially peeks away when idle.
4. Drag onto the bottom **X** to hide it, or hide it from Home. Show it again from Home when needed.

The overlay runs as a foreground service with a notification and search entry point. It needs Android's display-over-other-apps access. Some manufacturers also restrict background services; review their battery/autostart settings if the bubble disappears. Hiding the bubble does not delete the shelf.

The floating shelf uses stacked cards and selection controls. Select items for Share or WhatsApp, or begin a global drag with supported content. Android and the receiving app decide whether a cross-app drop is accepted. A visible drag shadow cannot prove that another app received an attachment.

## Saving and organizing

### Notes and clean links

Open **Shelf → Note**, type text or a link, and save. From another app, choose **Share → StashDrop** for shared text. Saved text is limited to 100,000 characters. The sanitizer removes recognized URL tracking keys such as `utm_*`, `fbclid`, `gclid`, `si`, `igshid`, and `ref`; it preserves other query values, their encoding, and surrounding prose punctuation.

Cleaning a URL does not guarantee anonymity or remove every site's tracking mechanism. Opened websites still follow the browser's privacy behavior.

### Files and images

Share a photo, document, or other readable attachment to StashDrop. Android's single/multiple share receiver accepts up to **30 attachments**, with a **50 MB limit per attachment**. Each accepted file is copied into private app storage under an app-generated filename, so the saved copy does not rely on the original app keeping its URI available. Storage usage grows with your saved files; failed copies are removed.

### Collections and deletion

Tap an item's collection label to move it into a named collection; a blank name becomes **Inbox**. Collection names are limited to 80 characters. Use collection chips to filter the shelf. Select cards to show the bottom selection bar. Share/WhatsApp sends the selection; **More selection actions** offers Move to collection and Delete. Confirming deletion removes the selected database entries and cached attachments, when present; canceling keeps them. Changing a collection filter clears selection. There is currently no undo, trash, in-app backup/restore, or cloud sync. Share important items out before uninstalling or replacing an incompatible build.

## Google Circle to Search

Every search button, bubble hold, notification action, enabled bottom marker gesture and StashDrop assistant entry requests Google’s contextual search session. The built-in capture/lasso/OCR/barcode workflow and its toggle were removed in 0.5.0. Existing saved items stay in the shelf.

1. Install/enable the Google app and select Google as digital assistant in Android’s default-app settings.
2. Enable Circle to Search in your phone’s navigation settings if available.
3. Try the Home search action or **Settings → Search & assistant → Try Google search**.
4. If Android rejects the session request, connect running, authorized Shizuku and retry. Its shell bridge uses the privileged active voice-service entry point. Without Shizuku, the app tries a best-effort session request that some systems reject.

Android accepting the request means only that it was dispatched. Google controls device/account eligibility and the actual UI, and can show Gemini/assistant instead of Circle to Search. StashDrop cannot detect or override that choice. A missing Google app, inactive Google voice service or rejected request shows setup guidance; no internal search screen opens. No screenshot permission is requested by StashDrop.

## Digital assistant and Shizuku

On Android 10+, Settings → Digital assistant expands the assistant controls; choose **Open assistant picker** for the system picker. Choosing StashDrop lets supported system assistant gestures enter StashDrop's search router. The OS, navigation mode, and manufacturer determine available gestures, including corner swipe or long-press home. The app does not intercept every phone's home/power controls.

With a running, authorized Shizuku connection:

- **Use StashDrop now** can save the previous role holder, change the assistant role, and connect Google’s voice service. Check the returned message; either operation can fail on an OEM build.
- When StashDrop is selected and Google's voice service is inactive, **Connect Google** can route assistant presses through StashDrop while restoring Google's voice service for the native search request.
- **Restore previous** attempts to restore the saved assistant role; the system assistant picker remains available for manual changes.
- **Set up with Shizuku** attempts overlay and notification setup; it does not change the assistant. Check the displayed status afterward; an OEM can reject a privileged operation.

These are explicit user actions. Normal app startup does not silently replace the digital assistant. Shizuku must already be installed, running, and authorized; the app cannot supply its authority itself. Its binder/service can stop or need reconnection after a reboot. The shelf and normal sharing remain available without Shizuku; native search requests may be rejected without its privileged bridge.

## Gestures, motion, and haptics

Bottom gesture strips are overlay touch regions above system navigation, separate from Android's own assistant gesture. They run with the floating service, hide in landscape, when locked and while the keyboard is visible, and can conflict with other navigation or app controls on some devices. Disable them in Settings if they interfere.

| Setting | Fresh-install default | Behavior |
| --- | --- | --- |
| Gesture strips | On | Enables the bottom overlay touch regions while the service runs |
| Swipe up | On | Pull up about 64 dp, or flick upward, to search |
| Corner swipe | Off | Swipe diagonally up and inward from a visible side marker |
| Press and hold | Off | Hold the center marker for Android’s long-press timeout |
| Double tap | Off | Two nearby quick taps on the center marker within Android’s double-tap timeout |
| Auto-stash copied text | Off | Saves permitted copied text while the monitor is running |
| Haptic feedback | On | Feedback for pulls, selection, dragging, and confirmation |
| Springiness | 1.0 | Adjusts motion intensity within 0.5–1.5 |

Aurora waves and the liquid bubble use AGSL on Android 13+, with Canvas fallbacks below that version. Spring animation and velocity-projected paths drive motion; actual smoothness depends on the device. Blur/specular effects operate on the app's own content and do not provide general blur of arbitrary apps behind the overlay.

## Clipboard auto-stash

Enable **Settings → Gestures, clipboard & feel → Auto-stash copied text** only if you want copied text retained on the shelf. The monitor uses a Shizuku clipboard listener when permitted and an ordinary foreground listener as a fallback. Background clipboard access depends on Android and OEM restrictions; the app displays monitor status. It handles text, rather than arbitrary image/file clipboard entries.

Enabling the preference requires confirming a sensitive-text retention prompt. Text passes through link cleaning before storage. Recent automatic copies are deduplicated using a bounded history of the last 100 digests. The digest history avoids duplicate payloads in that table, but the shelf itself still contains the saved text. Passwords, codes, addresses, and other sensitive copies can therefore remain until you delete their shelf items. Turning auto-stash off stops collection; it does not erase earlier saved items.

## Sharing, WhatsApp, and global drag

Use Share for Android's chooser, or the floating shelf's WhatsApp action for its targeted sharing flow. Multiple cards can be packaged into a selection. Attachments use FileProvider content URIs with temporary read grants; text is included through Android sharing/ClipData.

WhatsApp and other receivers decide supported MIME types, attachment combinations, and drop targets. If a drag is ignored or a mixed selection is rejected, use the normal Share action and try a compatible item separately. StashDrop cannot observe another app's internal landing coordinates or guarantee its final animation. The flight feedback implemented inside StashDrop is local visual feedback.

## Crash reports and privacy

After an unexpected close, the dashboard can display the latest locally stored crash report with **Copy report** and **Dismiss**. Reports are not automatically sent to a server. Review/redact a copied report before submitting it; include app version, Android/OEM, navigation mode, Shizuku state, and reproducible steps.

Shelf content lives in Room and private files. Android app backup is disabled. There is no StashDrop account, application backend, or automatic cloud shelf sync in the implementation. Native Google search and explicit sharing hand content to external apps, whose network/privacy behavior applies. See [architecture and data boundaries](ARCHITECTURE.md#data-and-privacy-boundaries).

## Troubleshooting

| Symptom | What to try |
| --- | --- |
| Bubble never appears | Allow overlay access, return to Home, and show the bubble; check notification/service and OEM battery restrictions |
| Gemini opens instead of circles | Enable native Circle to Search in phone settings, update Google, and check device eligibility; StashDrop cannot force Google’s UI |
| Google route unavailable while StashDrop is assistant | Use Digital assistant → Connect with authorized Shizuku, or choose Google manually; eligibility still applies |
| Clipboard items do not arrive | Confirm auto-stash is on, monitor status, Shizuku authorization, and OEM background limits |
| File drag fails in another app | Use Share; the destination must support MIME types and URI grants |
| An update will not install | Use the official APK signed with the original certificate; different signing identities cannot update in place |

For proposed fixes and future screen changes, see the [roadmap](ROADMAP.md). For repeatable hardware checks, use [device validation](DEVICE_VALIDATION.md).
