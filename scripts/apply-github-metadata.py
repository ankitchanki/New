"""Apply the reviewed description and add topics using existing gh authentication.

Requires network access to api.github.com and permission to edit this repository.
Never reads, prints, or stores credential values. Existing topics are preserved.
"""
import json
from pathlib import Path
import subprocess

root = Path(__file__).resolve().parents[1]
metadata = json.loads((root / ".github/repository-metadata.json").read_text())
command = ["gh", "repo", "edit", metadata["repository"], "--description", metadata["description"]]
for topic in metadata["topics"]:
    command += ["--add-topic", topic]
subprocess.run(command, check=True)
print("GitHub repository description and topics updated.")
