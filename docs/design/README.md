# StashDrop SVG UI design

> Historical UI concept: capture, selection and OCR screens in this package predate 0.5.0 and were removed from the Android app. Current search routes only to Google.

An interactive **UI concept** grounded in StashDrop's current Home / Shelf / Settings structure and dark navy, periwinkle, and mint palette. Layout refinements are design proposals. This package does not change the Android app or its signed APK.

## Open the design

Download [StashDrop-UI-Design.zip](downloads/StashDrop-UI-Design.zip), extract it, and open **index.html** in a current desktop browser. The prototype has no external libraries, account, or network requirement. If your browser restricts local SVG objects, use the optional local HTTP server below. Its sample data lasts only for the current session; refresh or Reset demo clears it.

- [Screen overview SVG](stashdrop-ui-overview.svg): nine editable vector artboards for visual review and import into vector design tools.
- [Interactive SVG](stashdrop-interactive.svg): the working prototype; open the downloaded file directly in a browser to enable its script.
- [Browser studio](index.html): wraps the SVG with screen shortcuts, instructions, and an interaction log.
- [Design QA](design-qa.md): browser checks, visual review, and limitations.

GitHub and image viewers disable SVG scripts. Viewing the SVG as an image shows a static preview; download/extract the package to use the interactions. The overview has no scripts or external assets. The interactive file uses JavaScript and embedded HTML form controls, so its behavior is intended for a browser rather than a vector editor.

## Screens and interactions

| Area | Try this | Visible result |
| --- | --- | --- |
| Home | Allow access, then show/hide the bubble | Simulated access dialog, active bubble, and confirmation |
| Navigation | Tap Home, Shelf, Settings, or a studio shortcut | Screen changes while session data is retained |
| Recent item | Tap a saved card on Home | Opens Shelf with that card selected |
| Bubble | Tap, hold, or drag | Floating shelf, search route, or edge snap |
| Bubble dismissal | Drag onto the bottom X | Bubble hides; saved items remain |
| Shelf note | Add an empty note, then enter text and save | Empty input prompts for text; valid input adds an Inbox card |
| Import | Import the sample tracked link or image | New sample item; link example removes its tracking parameter |
| Collections | Choose a filter; select a card → More actions → Move to Ideas or New collection | Filtered list and updated/custom collection label; a blank name uses Inbox |
| Selection | Tap cards, then Share or More actions | Selection count and batch actions |
| Pagination | Use previous/next controls when more cards exist | Other saved cards become accessible |
| Deletion | Delete selected; cancel or confirm | Items remain on cancel or are removed on confirmation |
| Sharing | Share or WhatsApp; cancel or complete | Simulated receiver sheet and outcome feedback |
| Floating stack | Select cards, then drag the stack upward | Accepted/canceled sample drop feedback |
| Access settings | Allow bubble, notifications, Shizuku, or screenshots | Status changes only in the demo |
| Native search | Keep Google mode on and invoke search | Honest Google hand-off state with built-in recovery action |
| Built-in search | Turn Google mode off, then search | Capture consent when no simulated direct access is available |
| Capture consent | Cancel, then retry and allow | Returns to the current screen or opens the selection canvas |
| Lasso | Draw around the sample text, or use Select sample region | Glowing selection and fixed demo OCR text |
| Result actions | Search / Lens / Copy / Stash / Share / Again | External-action dialog, demo copy feedback, saved cutout, or reset |
| Cutout reuse | Stash, then Lens or Share the same selection | One saved image per selection rather than repeated duplicates |
| Assistant | Select StashDrop, connect Google, restore previous | Simulated assistant status; Connect requires demo Shizuku access |
| Gesture switches | Enable/disable strips, swipe, corners, hold, double tap | Preferences change and affect the live bottom strip |
| Bottom strip | With bubble running, swipe up; enable and try hold/double tap/corner swipe | Opens the selected search route |
| Clipboard | Enable auto-stash, then cancel/confirm its privacy prompt | Off remains off on cancel; confirming enables the preference |
| Haptics / springiness | Toggle feedback or change the slider | Preference and slider value update; no device vibration |
| Keyboard | Tab, Enter/Space, slider arrow keys, Escape | Focus/activation, adjusted slider, dismissal or return Home |
| Crash report | Use Sample crash report in the studio, then copy/dismiss | Simulated local diagnostic dialog; no clipboard write |
| Reset | Use Reset demo | Original three sample items and defaults restored |

## Scope and simulation

All Android permissions, Shizuku operations, assistant role changes, Google sessions, screenshots, OCR/barcodes, clipboard writes, external search/Lens, sharing, and drop acceptance are **simulated**. No system setting, real clipboard, device service, or external app is accessed. Recognition always returns the visible sample text; it does not inspect your screen. The design's local import is a sample action rather than a real file picker.

The Android app's actual limits and storage behavior are in the [feature guide](https://github.com/ankitchanki/StashDrop/blob/main/docs/FEATURE_GUIDE.md) and [platform support](https://github.com/ankitchanki/StashDrop/blob/main/docs/PLATFORM_SUPPORT.md). Motion in this SVG illustrates state changes; it is not the app's AGSL implementation or a performance benchmark. Shelf pagination is a prototype presentation choice; the Android app uses its native list.

## Design tokens

| Token | Value |
| --- | --- |
| Background | `#0C0E15` |
| Surface | `#171B26` |
| Raised surface | `#242A39` |
| Primary / periwinkle | `#B4C9FF` |
| Secondary / mint | `#9DE3CF` |
| Primary text | `#F1F3FA` |
| Secondary text | `#A4AABE` |
| Interactive artboard | `390 × 844` |
| Typography | Arial / Helvetica / system sans-serif |

SVG shapes, text, paths, symbols, and styles remain editable. The overview flattens its slider into ordinary vector shapes, contains no scripts or foreignObject controls, and shares a single definitions block. Native SVG text may use a substitute font in tools without the specified fonts.

## Local development

Edit `stashdrop-interactive.svg` and `index.html`. A local server is optional:

```bash
python -m http.server 8765 --directory docs/design
```

Use the server internally for browser checks. Direct-from-disk opening depends on browser policy; it could not be tested in this cloud browser because file navigation is administratively blocked. The local HTTP route was verified. After changes, repeat the flows above, regenerate the overview from current rendered SVG scenes, refresh QA, and recreate the downloadable ZIP from the reviewed source files. Keep sample content synthetic and private signing material outside this package.
