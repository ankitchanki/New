"""Render documentation animations from the app's actual runtime-shader sources.

Host tooling: skia-python==144.0.post2, numpy, Pillow.
These renders document the math; they do not measure Android GPU performance.
"""
from pathlib import Path
import re
import skia
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/assets"
OUT.mkdir(parents=True, exist_ok=True)
SOURCE = ROOT / "app/src/main/java/com/vibe/stashdrop/core/shaders"


def render(name, size, extra, target):
    source = re.search(r'const val source = """(.*?)"""', (SOURCE / f"{name}.kt").read_text(), re.S).group(1)
    effect = skia.RuntimeEffect.MakeForShader(source)
    frames = []
    for index in range(48):
        builder = skia.RuntimeShaderBuilder(effect)
        builder.setUniform("u_resolution", [float(size[0]), float(size[1])])
        builder.setUniform("u_time", index / 12.0)
        for key, value in extra.items():
            builder.setUniform(key, value)
        surface = skia.Surface(*size)
        canvas = surface.getCanvas()
        canvas.clear(skia.ColorSetRGB(12, 14, 21))
        canvas.drawPaint(skia.Paint(Shader=builder.makeShader()))
        png = surface.makeImageSnapshot().encodeToData()
        import io
        frames.append(Image.open(io.BytesIO(bytes(png))).convert("RGB"))
    frames[0].save(OUT / target, save_all=True, append_images=frames[1:], duration=83, loop=0, optimize=True)
    print(f"{target}: 48 source-rendered frames")


render("GeminiAuroraWaveShader", (640, 360), {"u_touchPos": [320.0, 280.0], "u_progress": 0.72}, "aurora-wave.gif")
render("LiquidMetaballDockShader", (360, 360), {"u_absorption": 0.85}, "liquid-metaball.gif")
