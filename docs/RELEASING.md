# Releasing StashDrop

Release builds use a dedicated production signing key. Debug keys must never sign a public release. Future updates of the same Android package require the same signing identity.

## Signing inputs

Keep the keystore and credentials outside the repository. Supply these local environment variables to the build process:

- `STASHDROP_RELEASE_KEYSTORE`: path to the private PKCS12/JKS keystore.
- `STASHDROP_RELEASE_KEY_ALIAS`: alias within that keystore.
- `STASHDROP_RELEASE_STORE_PASSWORD`: keystore password.
- `STASHDROP_RELEASE_KEY_PASSWORD`: private-key password.

Alternatively, use `scripts/build-release.py --credentials /private/path/release-signing.json`. Its private JSON configuration contains `storeFile`, `keyAlias`, `storePassword`, and `keyPassword`. Relative keystore paths resolve beside the configuration file. The helper requires owner-only configuration permissions on Unix, passes credentials through the child environment, and never prints their values or puts them in command arguments.

No private signing configuration or keystore belongs in Git, a README, CI logs, or public release assets. Retain a secure offline backup of the first release's signing identity; losing it prevents compatible APK updates.

## Build on GitHub Actions

`.github/workflows/android.yml` builds a debug APK, runs the unit/screenshot tests and lint on every push and pull request, and uploads the APK as the `StashDrop-debug-apk` workflow artifact.

Signed releases run when a `v*` tag is pushed, or manually from the Actions tab. Add these repository secrets first (Settings → Secrets and variables → Actions):

- `STASHDROP_KEYSTORE_BASE64`: the keystore file, base64-encoded (`base64 -w0 release.p12`).
- `STASHDROP_RELEASE_KEY_ALIAS`, `STASHDROP_RELEASE_STORE_PASSWORD`, `STASHDROP_RELEASE_KEY_PASSWORD`.

The production key already exists and signed the prepared 0.1.0 build. **Restore that key and its credentials from the private owner backup for 0.1.1 and later updates. Do not generate a replacement.** Keep an offline backup. The workflow rejects a certificate that differs from the original public verification report.

Only for a completely new application that has never had a signing identity, create a key with `keytool -genkeypair -keystore release.p12 -storetype PKCS12 -alias stashdrop -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=StashDrop"`.

An owner with repository Secrets write permission can install all four secrets from the private JSON backup without displaying their values:

```bash
chmod 600 /private/path/release-signing.json
python scripts/configure-actions-signing.py --credentials /private/path/release-signing.json
```

Use an existing authorized `gh` connection. Secret values are sent on stdin and encrypted by the GitHub CLI; they are not placed in command arguments or printed. Alternatively, enter the four values securely in GitHub Settings. This cloud session's integration returned HTTP 403 for the Actions secrets API, so that connection cannot configure them. A fine-grained owner token needs repository Secrets write permission for setup, plus Contents write for release operations; never paste it into chat or Git.

A tag push builds and tests the signed APK and AAB, verifies their signatures and original certificate, checks alignment, writes `SHA256SUMS.txt` and `release-verification.json`, and publishes a GitHub release using `docs/releases/<tag>.md`. The workflow must exist on the tagged commit. Version 0.1.1 uses `versionName = "0.1.1"` and `versionCode = 2`; the tag must be `v0.1.1`. The older `v0.1.0` tag predates the workflow and is preserved.

If a tag run failed because secrets were missing, configure the secrets and rerun that tag's workflow from Actions. Do not move or delete the version tag. If the workflow itself needs correction, run the updated workflow on `main` with its `release_tag` input set to the existing tag. Both jobs check out that tag, validate its version, and publish its artifacts. Leaving the input empty produces artifacts without publication. CI uses its own repository-scoped `github.token` with Contents write permission for publication.

For 0.1.1, the initial tag run stopped before compilation because `sdkmanager` was unavailable on PATH. The corrected workflow pins Ubuntu 24.04 and uses Android SDK setup before installing SDK 35. Run it from Actions → Android build → Run workflow, choose `main`, and set `release_tag` to `v0.1.1`; or use an authorized CLI connection:

```bash
gh workflow run android.yml --repo ankitchanki/StashDrop --ref main -f release_tag=v0.1.1
```

## Build and validate

Activate the prepared cloud toolchain when using the cloud snapshot:

```bash
source scripts/cloud-env.sh
python scripts/configure-cloud-gradle.py
python scripts/build-release.py --credentials /private/path/release-signing.json --verify
```

Collect the public outputs and verify them with:

```bash
python scripts/prepare-release.py --credentials /private/path/release-signing.json --output /path/to/public/release-assets --version 0.1.1
```

On another machine, use your own Java/Android SDK environment. The helper runs `assembleRelease` and `bundleRelease`; `--verify` also runs the logic/visual tests and debug/release lint. Builds fail rather than silently producing an unsigned release if signing input is missing.

Verify the final APK with SDK `apksigner verify --verbose --print-certs` and `zipalign -c -P 16 4`. Verify the AAB with JDK `jarsigner -verify`. Match both signing certificate fingerprints to the published public certificate. Generate checksums only after all artifacts are final.

## Publish

1. Update `CHANGELOG.md` and the versioned notes in `docs/releases/`.
2. Commit with the repository's neutral project attribution. Review the staged files; private signing material must be absent.
3. Create an annotated version tag on the verified source commit and push it.
4. Let the version-tag workflow publish, or use `python scripts/publish-release.py --version 0.1.1 --assets /path/to/public/release-assets` from an authorized connection if CI publication is not being used. Do not race both publishers.
5. Verify the resulting GitHub release and every uploaded asset.

The publishing script verifies the remote tag, checks public artifact checksums, and uploads only its explicit public-file allowlist. It does not create credentials, publish private signing material, overwrite release assets, or bypass network policy.

GitHub release creation needs `api.github.com` and asset upload needs `uploads.github.com`. Domain additions are saved in the cloud environment draft; review/save and publish environment changes when required. The latest direct cloud publication attempt reached the API but uploads returned HTTP 401; repository metadata and Actions-secret operations returned HTTP 403. Existing Git push access does not establish API/upload authorization. GitHub Actions runs with its own scoped token.

## Attribution cleanup limits

The project uses `StashDrop Contributors` for local commit/tag metadata. The two original commits were rewritten with neutral author/committer fields and unchanged file trees. Old local reflogs and unreachable objects were pruned. Force-updating a branch removes the old identities from reachable project history; it does not erase third-party clones, forks, GitHub caches, account profiles, or this chat. GitHub support/account controls may be needed for externally retained copies.
