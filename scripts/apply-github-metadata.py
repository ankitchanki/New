"""Apply reviewed metadata through gh, preserving existing repository topics.

Requires repository administration access. Does not inspect credential values.
GitHub permits at most 20 topics; an oversized union is rejected before writing.
"""
import json
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[1]
metadata = json.loads((root / ".github/repository-metadata.json").read_text())
repo = metadata["repository"]
desired = metadata["topics"]
if (len(set(desired)) > 20 or any(not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,49}", topic) for topic in desired)
        or len(metadata["description"]) > 350):
    raise SystemExit("Metadata must have at most 20 valid topics and a description of at most 350 characters.")


def api(path, method="GET", payload=None):
    command = ["gh", "api", path, "--method", method]
    if payload is not None:
        command += ["--input", "-"]
    result = subprocess.run(command, input=None if payload is None else json.dumps(payload),
                            text=True, capture_output=True)
    if result.returncode:
        raise SystemExit(result.stderr.strip() or "GitHub metadata request failed.")
    return json.loads(result.stdout)


current = api(f"repos/{repo}")
combined = sorted(set(current["topics"]) | set(desired))
if len(combined) > 20:
    raise SystemExit(
        f"Existing and desired topics total {len(combined)}; GitHub allows 20. "
        "Review the topic lists in repository settings and .github/repository-metadata.json before retrying. "
        "No metadata was changed."
    )
api(f"repos/{repo}", "PATCH", {"description": metadata["description"]})
api(f"repos/{repo}/topics", "PUT", {"names": combined})
updated = api(f"repos/{repo}")
if updated["description"] != metadata["description"] or set(updated["topics"]) != set(combined):
    raise SystemExit("GitHub metadata readback did not match; inspect repository settings before retrying.")
print("GitHub description and topics updated and verified; existing topics preserved.")
