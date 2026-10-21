# StashDrop UI design · SVG

Vector mockups of every screen, sheet and dialog in the **current** app (0.7.0). They were drawn from the Compose source (`ui/dashboard/DashboardScreen.kt`, `features/floatingdock/*`, `ui/shelf/*`, `ui/theme/Theme.kt`). Unlike the older concept in `../`, they contain no capture, lasso or OCR screens.

- [`stashdrop-all-screens.svg`](stashdrop-all-screens.svg): all 23 screens on one sheet
- [`components.svg`](components.svg): tokens, bubble and edge-handle states, buttons, chips, rows and the selection bar
- [`screens/`](screens): one 390 × 844 dp SVG per screen

| # | Screen | # | Screen |
|---|---|---|---|
| 01 | Home | 13 | Floating shelf · stack selected |
| 02 | Home · edge shelf on | 14 | Floating shelf · empty filter |
| 03 | Home · nothing saved | 15 | Floating shelf · delete confirm |
| 04 | Shelf | 16 | Edge handle + foreground notification |
| 05 | Shelf · selection bar | 17 | Bubble drag · dismiss target |
| 06 | Shelf · more actions menu | 18 | Cross-app drag |
| 07 | Shelf · swipe to delete + snackbar | 19 | Dialog · Keep a thought |
| 08 | Shelf · empty | 20 | Dialog · Move to collection |
| 09 | Settings | 21 | Dialog · Delete items |
| 10 | Settings · Shizuku + assistant expanded | 22 | Dialog · Auto-stash consent |
| 11 | Interactions | 23 | Dialog · Crash report |
| 12 | Floating shelf | | |

The content is sample data. The shapes and text are plain SVG with no scripts, so they open in Figma, Illustrator, Inkscape or a browser. Text uses Roboto, with system sans-serif as the fallback.

## Regenerate

```bash
cd docs/design/svg/generator
python3 stashdrop.py ..
```
