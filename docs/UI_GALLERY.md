# StashDrop visual guide

The repository gallery distinguishes app UI, sample content, shader output, and branding.

## UI previews

All UI images below are rendered from the production Jetpack Compose components using **Paparazzi 1.3.5**, with Pixel 5 dimensions. The full Controls render uses a taller virtual viewport to expose the settings in one image. No physical phone or emulator was used. The preview data includes three sample notes/links; it does not contain personal clipboard history.

| Preview | What it shows | Source |
| --- | --- | --- |
| [Overview](assets/screenshots/overview.png) | Dashboard hero, motion playground, floating-shelf and search entry points | `DashboardScreen`, tab 0 |
| [Shelf](assets/screenshots/shelf.png) | Sample notes/links, collections, share and delete actions | `DashboardScreen`, tab 1 |
| [Controls](assets/screenshots/controls.png) | Connections, optional access, gesture controls | `DashboardScreen`, tab 2 |
| [Full Controls](assets/screenshots/controls-full.png) | Gesture toggles, clipboard opt-in, spring intensity, assistant controls | Same component, taller viewport |
| [Floating shelf](assets/screenshots/floating-shelf.png) | Production fan-out shelf sheet after its opening motion | `IosFanOutStashSheet`, inspection mode |
| [Circle selection canvas](assets/screenshots/circle-canvas.png) | Production lasso canvas over synthetic sample artwork | `AuroraCircleOverlayCanvas`, initial sample selection |
| [Aurora still](assets/screenshots/aurora-wave.png) | Production shader/Canvas composable with sample pull progress | `AuroraWave` |

The Studio render shows its selection canvas, not a completed screen-capture or OCR session. The sample path is supplied as component state. Callback functions in the presentation fixture are inert because this is rendering, not end-to-end interaction testing.

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

## Repository About and topics

[Repository metadata](../.github/repository-metadata.json) is the source for the proposed About description and 20 topic tags. Run `python scripts/apply-github-metadata.py` with existing GitHub CLI authentication and permission to administer `ankitchanki/New` to apply the description and add topics while preserving existing topics.

During preparation, the environment proxy denied `api.github.com`. The required domain addition was saved to the environment draft; saving the draft alone does not apply network access. Repository file publication and GitHub About/topic metadata are separate operations.
