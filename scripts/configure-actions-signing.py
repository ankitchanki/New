"""Install the existing private release identity as four encrypted Actions secrets.

Run with repository Secrets write access through an existing gh connection.
Secret values are sent only on stdin, never printed or placed in command arguments.
"""
import argparse
import base64
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--credentials", type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
config = args.credentials.resolve(strict=True)
if config.is_relative_to(root) or (os.name != "nt" and config.stat().st_mode & 0o077):
    raise SystemExit("Use an owner-only signing configuration outside the repository")
# Check access before reading any signing secrets.
subprocess.run(["gh", "api", "repos/ankitchanki/StashDrop/actions/secrets/public-key", "--jq", ".key_id"],
               cwd=root, check=True, stdout=subprocess.DEVNULL)
values = json.loads(config.read_text())
keystore = Path(values["storeFile"])
if not keystore.is_absolute():
    keystore = config.parent / keystore
keystore = keystore.resolve(strict=True)
if keystore.is_relative_to(root):
    raise SystemExit("Keep the production keystore outside the repository")
secrets = {
    "STASHDROP_KEYSTORE_BASE64": base64.b64encode(keystore.read_bytes()).decode("ascii"),
    "STASHDROP_RELEASE_KEY_ALIAS": values["keyAlias"],
    "STASHDROP_RELEASE_STORE_PASSWORD": values["storePassword"],
    "STASHDROP_RELEASE_KEY_PASSWORD": values["keyPassword"],
}
if any(not value for value in secrets.values()):
    raise SystemExit("All four signing values are required")
for name, value in secrets.items():
    subprocess.run(["gh", "secret", "set", name, "--repo", "ankitchanki/StashDrop", "--app", "actions"],
                   cwd=root, input=value, text=True, check=True)
    print("Configured Actions secret: " + name)
subprocess.run(["gh", "api", "repos/ankitchanki/StashDrop/actions/secrets", "--jq", ".secrets[].name"],
               cwd=root, check=True)
