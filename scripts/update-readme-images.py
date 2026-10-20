"""Copy unmodified source-rendered Paparazzi baselines into the README gallery."""
from pathlib import Path
import hashlib
import json
import shutil
import struct

root = Path(__file__).resolve().parents[1]
source = root / "app/src/test/snapshots/images"
target = root / "docs/assets/screenshots"
target.mkdir(parents=True, exist_ok=True)
images = {
    "overview": "overview_overview",
    "shelf": "shelf_shelf",
    "controls": "controls_controls",
    "interactions": "interactions_interactions",
    "controls-full": "controlsFull_controls-full",
    "edge-handles": "edgeHandles_edge-handles",
    "floating-shelf": "floatingShelf_floating-shelf",
    "aurora-wave": "auroraWave_aurora-wave",
}
manifest = {"renderer": "Paparazzi 1.3.5 / production Compose components", "content": "Sample data; not phone screenshots", "images": []}
for name, suffix in images.items():
    candidate = source / f"com.vibe.stashdrop.presentation_ReadmeScreenshots_{suffix}.png"
    if not candidate.is_file():
        raise SystemExit(f"Missing render: {candidate.name}; run recordPaparazziDebug first")
    destination = target / f"{name}.png"
    shutil.copy2(candidate, destination)
    data = destination.read_bytes()
    width, height = struct.unpack(">II", data[16:24])
    manifest["images"].append({"file": destination.name, "width": width, "height": height, "sha256": hashlib.sha256(data).hexdigest()})
(target / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
print(f"Copied {len(images)} unmodified component renders")
