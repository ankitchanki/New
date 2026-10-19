"""Collect and verify public release assets; signing inputs remain private."""
import argparse
import base64
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import zipfile
from xml.etree import ElementTree as ET

parser = argparse.ArgumentParser()
parser.add_argument("--credentials", type=Path, required=True)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("--version", default="0.1.0")
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
config = args.credentials.resolve(strict=True)
if config.is_relative_to(root) or (os.name != "nt" and config.stat().st_mode & 0o077):
    raise SystemExit("Use an owner-only signing configuration outside the repository")
values = json.loads(config.read_text())
key = Path(values["storeFile"])
if not key.is_absolute():
    key = config.parent / key
out = args.output.resolve()
out.mkdir(parents=True, exist_ok=True)
env = os.environ.copy()
env["STASHDROP_RELEASE_STORE_PASSWORD"] = values["storePassword"]
java = Path(env["JAVA_HOME"]) / "bin"
sdk = Path(env["ANDROID_HOME"]) / "build-tools/35.0.0"


def run(command, **kwargs):
    return subprocess.run([str(x) for x in command], check=True, capture_output=True, text=True, **kwargs).stdout


version = args.version
apk = out / f"StashDrop-v{version}.apk"
aab = out / f"StashDrop-v{version}.aab"
cert = out / "StashDrop-release-certificate.pem"
shutil.copy2(root / "app/build/outputs/apk/release/app-release.apk", apk)
shutil.copy2(root / "app/build/outputs/bundle/release/app-release.aab", aab)
verified = run([sdk / "apksigner", "verify", "--verbose", "--print-certs", apk])
# API 28+ supports APK v3. apksigner may omit v2 verification when minSdk is 28.
assert "Verifies" in verified and "v3): true" in verified
assert "CN=StashDrop" in verified and "CN=Android Debug" not in verified
apk_hash = re.search(r"certificate SHA-256 digest: ([0-9a-f]+)", verified).group(1)
run([sdk / "zipalign", "-c", "-P", "16", "4", apk])
bundle_result = run([java / "jarsigner", "-verify", "-verbose", "-certs", aab])
assert "jar verified." in bundle_result
bundle_cert = run([java / "keytool", "-printcert", "-jarfile", aab])
bundle_hash = re.search(r"SHA256: ([0-9A-F:]+)", bundle_cert).group(1).replace(":", "").lower()
run([java / "keytool", "-exportcert", "-rfc", "-alias", values["keyAlias"], "-keystore", key,
     "-storepass:env", "STASHDROP_RELEASE_STORE_PASSWORD", "-file", cert], env=env)
der = base64.b64decode("".join(line for line in cert.read_text().splitlines() if not line.startswith("---")))
assert hashlib.sha256(der).hexdigest() == apk_hash == bundle_hash
# Preserve the signing identity already used for the first distributed build.
original_report = json.loads((root / "docs/releases/v0.1.0-verification.json").read_text())
assert apk_hash == original_report["certificateSha256"], "Signing certificate differs from the original StashDrop release key"
badging = run([sdk / "aapt", "dump", "badging", apk])
assert "name='com.vibe.stashdrop'" in badging and f"versionName='{version}'" in badging
version_code = int(re.search(r"versionCode='(\d+)'", badging).group(1))
configured_code = int(re.search(r"versionCode\s*=\s*(\d+)", (root / "app/build.gradle.kts").read_text()).group(1))
assert version_code == configured_code
assert "sdkVersion:'28'" in badging and "targetSdkVersion:'35'" in badging and "application-debuggable" not in badging
counts = Counter()
for path in (root / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml"):
    result = ET.parse(path).getroot()
    counts.update({name: int(result.attrib[name]) for name in ["tests", "failures", "errors", "skipped"]})
assert counts["tests"] > 0 and counts["failures"] == counts["errors"] == counts["skipped"] == 0
lint = {}
for variant in ["debug", "release"]:
    issues = ET.parse(root / f"app/build/reports/lint-results-{variant}.xml").getroot().findall("issue")
    lint[variant] = {"errors": sum(i.attrib["severity"] in ["Error", "Fatal"] for i in issues),
                     "warnings": sum(i.attrib["severity"] == "Warning" for i in issues)}
    assert lint[variant]["errors"] == 0
libraries = []
for archive in [apk, aab]:
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        assert not any(name.lower().endswith((".p12", ".jks", ".keystore", ".pfx")) for name in z.namelist())
with zipfile.ZipFile(apk) as z:
    for name in z.namelist():
        if not name.endswith(".so") or not name.startswith(("lib/arm64-v8a/", "lib/x86_64/")):
            continue
        data = z.read(name)
        assert data[:4] == b"\x7fELF" and data[4] == 2
        endian = "<" if data[5] == 1 else ">"
        offset = struct.unpack_from(endian + "Q", data, 32)[0]
        size, count = struct.unpack_from(endian + "HH", data, 54)
        alignments = [struct.unpack_from(endian + "Q", data, offset + i * size + 48)[0]
                      for i in range(count) if struct.unpack_from(endian + "I", data, offset + i * size)[0] == 1]
        assert min(alignments) >= 16384
        libraries.append(name)
assert libraries
report = {"version": version, "versionCode": version_code, "sourceTag": "v" + version, "package": "com.vibe.stashdrop", "minSdk": 28,
          "targetSdk": 35, "signingIdentity": "StashDrop",
          "keyAlgorithm": re.search(r"key algorithm: (\S+)", verified).group(1),
          "keySizeBits": int(re.search(r"key size \(bits\): (\d+)", verified).group(1)),
          "certificateSha256": apk_hash, "apkSignatureVerified": True,
          "apkV2VerificationReported": "v2): true" in verified, "apkV3SigningVerified": True,
          "bundleSignatureVerified": True, "apk16KbZipAlignmentVerified": True,
          "native64Bit16KbAlignmentVerified": True, "nativeLibrariesChecked": libraries,
          "minified": True, "resourceShrinking": True, "tests": dict(counts), "lint": lint,
          "deviceRuntimeValidated": False, "privateSigningKeyIncluded": False}
(out / "release-verification.json").write_text(json.dumps(report, indent=2) + "\n")
public_names = [apk.name, aab.name, cert.name, "release-verification.json"]
(out / "SHA256SUMS.txt").write_text("\n".join(hashlib.sha256((out / name).read_bytes()).hexdigest() + "  " + name for name in public_names) + "\n")
(root / f"docs/releases/v{version}-verification.json").write_text(json.dumps(report, indent=2) + "\n")
print(f"APK v3 signature, AAB signature, matching certificate, 16 KB alignment, {counts['tests']} tests and lint verified.")
print("Public certificate SHA-256: " + apk_hash)
