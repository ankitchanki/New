"""Build signed release artifacts from a private local signing configuration.

The configuration must remain outside Git. Credentials are passed to Gradle
through the child process environment and are never printed or put in arguments.
"""
import argparse
import json
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--credentials", type=Path, help="Private JSON signing configuration, stored outside the checkout")
parser.add_argument("--verify", action="store_true", help="Also run logic, visual-baseline, and lint checks")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
env = os.environ.copy()
if args.credentials:
    path = args.credentials.resolve(strict=True)
    if path.is_relative_to(root):
        raise SystemExit("Keep signing credentials outside the repository")
    if os.name != "nt" and path.stat().st_mode & 0o077:
        raise SystemExit("Signing configuration must be readable only by its owner (chmod 600)")
    credentials = json.loads(path.read_text())
    keystore = Path(credentials["storeFile"])
    if not keystore.is_absolute():
        keystore = path.parent / keystore
    env.update({
        "STASHDROP_RELEASE_KEYSTORE": str(keystore.resolve(strict=True)),
        "STASHDROP_RELEASE_KEY_ALIAS": credentials["keyAlias"],
        "STASHDROP_RELEASE_STORE_PASSWORD": credentials["storePassword"],
        "STASHDROP_RELEASE_KEY_PASSWORD": credentials["keyPassword"],
    })
required = ["STASHDROP_RELEASE_KEYSTORE", "STASHDROP_RELEASE_KEY_ALIAS", "STASHDROP_RELEASE_STORE_PASSWORD", "STASHDROP_RELEASE_KEY_PASSWORD"]
missing = [name for name in required if not env.get(name)]
if missing:
    raise SystemExit("Missing release signing variable names: " + ", ".join(missing))
command = [str(root / "gradlew"), ":app:assembleRelease", ":app:bundleRelease"]
if args.verify:
    command += [":app:testDebugUnitTest", ":app:verifyPaparazziDebug", ":app:lintDebug", ":app:lintRelease"]
subprocess.run(command, cwd=root, env=env, check=True)
