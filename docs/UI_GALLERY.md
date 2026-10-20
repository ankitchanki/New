# StashDrop visual guide

The repository gallery distinguishes app UI, sample content, shader output, and branding.

## UI previews

The gallery reflects the native 0.7.0 UI. All UI images below are rendered from the production Jetpack Compose components using **Paparazzi 1.3.5**, with Pixel 5 dimensions. The full Settings render uses a taller virtual viewport to expose the settings in one image. No physical phone or emulator was used. The preview data includes sample notes/links plus generated local image/sticker files; it does not contain personal clipboard history.

| Preview | What it shows | Source |
| --- | --- | --- |
| [Home](assets/screenshots/overview.png) | Home header, recent items, edge shelf state and entry points | `DashboardScreen`, tab 0 |
| [Shelf](assets/screenshots/shelf.png) | Sample notes/links, collections, share and delete actions | `DashboardScreen`, tab 1 |
| [Settings](assets/screenshots/controls.png) | Setup access status, Google readiness, assistant and gesture controls | `DashboardScreen`, tab 2 |
| [Full Settings](assets/screenshots/controls-full.png) | Complete setup/search screen and entry to interaction preferences | Same component, taller viewport |
| [Interactions](assets/screenshots/interactions.png) | One transparent strip toggle, native assistant settings link, clipboard preference, and haptics; scroll for springiness | `DashboardScreen`, interaction page |
| [Floating shelf](assets/screenshots/floating-shelf.png) | Compact rectangular panel with real decoded sample media | `IosFanOutStashSheet(compact=true)`, inspection mode |
| [Edge handles](assets/screenshots/edge-handles.png) | One subtle production edge control | `ThumbDockIconComposable(edgeHandle=true)` |
| [Aurora still](assets/screenshots/aurora-wave.png) | Production shader/Canvas composable with sample pull progress | `AuroraWave` |


### Regenerate UI renders

With the normal Android build toolchain installed:

```bash
./gradlew :app:recordPaparazziDebug --tests '*ReadmeScreenshots*'
python scripts/update-readme-images.py
```

The fixture is [ReadmeScreenshots.kt](../app/src/test/java/com/vibe/stashdrop/presentation/ReadmeScreenshots.kt). Recorded baselines live in `app/src/test/snapshots/images`; the README copies are byte-for-byte matches. [The gallery manifest](assets/screenshots/manifest.json) records dimensions and SHA-256 hashes.

Verify against the committed baselines with:

```bash
./gradlew :app:verifyPaparazziDebug --tests '*ReadmeScreenshots*'
```

In the prepared cloud environment, activate the toolchain first:

```bash
cd /workspace/New
source scripts/cloud-env.sh
python scripts/configure-cloud-gradle.py
```

Host renders do not prove overlay lifecycle, permission handling, native Google search eligibility, background clipboard access, ML Kit execution, external drop acceptance, or real-device performance. Those checks remain in [DEVICE_VALIDATION.md](DEVICE_VALIDATION.md).

## Shader animation assets

[Aurora wave](assets/aurora-wave.gif) and [liquid metaball](assets/liquid-metaball.gif) are 48-frame desktop Skia rasterizations of the actual shader strings in `core/shaders`. Uniform values are fixtures. The assets demonstrate color/time/field behavior and do not assert a phone refresh rate.

```bash
python -m pip install skia-python==144.0.post2 numpy Pillow
python scripts/render-motion-assets.py
python scripts/test-shaders.py
```

For the prepared cloud snapshot, use `PYTHONPATH=/workspace/toolchains/shader-validator` before these scripts. The aurora frame is 640×360; the metaball frame is 360×360. GIF playback is approximately 12 fps for efficient documentation delivery.

## Branding artwork

[The StashDrop header](assets/stashdrop-banner.png) is AI-generated promotional artwork commissioned for this README. It is not a screenshot. Its dark navy, periwinkle, mint, stacked trays, and aurora reference the implemented theme and launcher mark. The original generated image was copied without repainting or compositing UI into it.

## Future UI direction

The [roadmap](ROADMAP.md#proposed-screen-direction) describes proposed improvements to Home readiness, shelf search/previews, floating controls, selection results, and accessible Settings. An [editable SVG screen map and interactive browser concept](design/README.md) record the design direction. The browser prototype predates Google-only search and includes obsolete internal capture/recognition concepts; it remains a historical simulation. The images above represent the current Android components, and the banner is separately identified artwork.

## Repository About and topics

[Repository metadata](../.github/repository-metadata.json) contains the desired About description and 20 topic tags. Run `python scripts/apply-github-metadata.py` with GitHub CLI authentication and repository administration access to apply metadata. The helper preserves existing topics and checks GitHub's 20-topic maximum before writing. If the combined list is too large, review the desired/existing tags in GitHub settings before retrying.

Git pushes and release updates have separate permissions from editing repository About/topics. An integration can publish this README while GitHub rejects metadata edits with HTTP 403; the reviewed metadata file remains available for an owner to apply.
