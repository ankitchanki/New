"""Publish verified public StashDrop artifacts using existing GitHub CLI access."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--version", required=True)
parser.add_argument("--assets", type=Path, required=True)
args = parser.parse_args()
if not re.fullmatch(r"\d+\.\d+\.\d+", args.version):
    raise SystemExit("Use a stable semantic version such as 0.1.0")
root = Path(__file__).resolve().parents[1]
assets = args.assets.resolve(strict=True)
version = args.version
names = [f"StashDrop-v{version}.apk", f"StashDrop-v{version}.aab", "StashDrop-release-certificate.pem", "release-verification.json"]
required = names + ["SHA256SUMS.txt"]
for name in required:
    if not (assets / name).is_file():
        raise SystemExit("Missing public asset: " + name)
checks = {}
for line in (assets / "SHA256SUMS.txt").read_text().splitlines():
    digest, name = line.split(maxsplit=1)
    checks[name.strip()] = digest
if set(checks) != set(names):
    raise SystemExit("Checksum list must contain exactly the public-file allowlist")
for name in names:
    if hashlib.sha256((assets / name).read_bytes()).hexdigest() != checks[name]:
        raise SystemExit("Checksum mismatch: " + name)
report = json.loads((assets / "release-verification.json").read_text())
if report.get("version") != version or report.get("apkSignatureVerified") is not True or report.get("bundleSignatureVerified") is not True:
    raise SystemExit("Signing verification must pass before publication")
tag = "v" + version
remote = subprocess.check_output(["git", "ls-remote", "origin", "refs/tags/" + tag], cwd=root, text=True)
if not remote.strip():
    raise SystemExit("Push the verified version tag before publishing")
# The gh CLI uses the configured HTTPS proxy and existing credentials normally.
subprocess.run(["gh", "api", "repos/ankitchanki/StashDrop", "--jq", ".full_name"], cwd=root, check=True)
command = ["gh", "release", "create", tag, "--repo", "ankitchanki/StashDrop", "--verify-tag", "--latest",
           "--title", "StashDrop " + version, "--notes-file", str(root / "docs/releases" / (tag + ".md"))]
command += [str(assets / name) for name in required]
subprocess.run(command, cwd=root, check=True)
subprocess.run(["gh", "release", "view", tag, "--repo", "ankitchanki/StashDrop", "--json", "url,tagName,isDraft,isPrerelease,assets"], cwd=root, check=True)
