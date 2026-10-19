"""Compile and rasterize the actual runtime shader strings with desktop Skia.

Optional host check: pip install skia-python==144.0.post2 numpy==2.5.4
This is a SkSL smoke test, not Android GPU or AGSL device certification.
"""
from pathlib import Path
import re
import numpy as np
import skia

root = Path(__file__).resolve().parents[1]
source_dir = root / "app/src/main/java/com/vibe/stashdrop/core/shaders"
output = root / "app/build/reports/shaders"
output.mkdir(parents=True, exist_ok=True)


def shader(name, uniforms):
    text = (source_dir / f"{name}.kt").read_text()
    source = re.search(r'const val source = """(.*?)"""', text, re.S).group(1)
    effect = skia.RuntimeEffect.MakeForShader(source)
    builder = skia.RuntimeShaderBuilder(effect)
    builder.setUniform("u_resolution", [256.0, 256.0])
    for key, value in uniforms.items():
        builder.setUniform(key, value)
    surface = skia.Surface(256, 256)
    surface.getCanvas().clear(skia.ColorTRANSPARENT)
    surface.getCanvas().drawPaint(skia.Paint(Shader=builder.makeShader()))
    return surface.makeImageSnapshot()


def aurora(progress, time=1.0):
    return shader("GeminiAuroraWaveShader", {"u_progress": progress, "u_time": time, "u_touchPos": [128.0, 200.0]})


idle = aurora(0.0).toarray()
wave = aurora(0.75)
wave_array = wave.toarray()
assert np.max(idle[:, :, 3]) == 0, "Idle wave must be transparent"
assert np.count_nonzero(wave_array[:, :, 3]) > 5000, "Pulled wave must render a visible body"
assert np.count_nonzero(wave_array != aurora(0.75, 2.0).toarray()) > 1000, "Time must animate the wave"
wave.save(str(output / "aurora.png"), skia.kPNG)


def dock(absorption):
    return shader("LiquidMetaballDockShader", {"u_absorption": absorption, "u_time": 1.0})


rest = dock(0.0).toarray()
expanded = dock(1.0)
expanded_array = expanded.toarray()
assert rest[128, 128, 3] > 200 and rest[0, 0, 3] == 0, "Dock must have an opaque core and transparent exterior"
assert np.count_nonzero(expanded_array[:, :, 3]) > np.count_nonzero(rest[:, :, 3]), "Absorption must expand the metaball field"
expanded.save(str(output / "metaball.png"), skia.kPNG)
print("2 runtime shader programs compiled; 5 raster behavior assertions passed")
