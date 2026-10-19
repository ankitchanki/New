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

## Build and validate

Activate the prepared cloud toolchain when using the cloud snapshot:

```bash
source scripts/cloud-env.sh
python scripts/configure-cloud-gradle.py
python scripts/build-release.py --credentials /private/path/release-signing.json --verify
```

Collect the public outputs and verify them with:

```bash
python scripts/prepare-release.py --credentials /private/path/release-signing.json --output /path/to/public/release-assets --version 0.1.0
```

On another machine, use your own Java/Android SDK environment. The helper runs `assembleRelease` and `bundleRelease`; `--verify` also runs the logic/visual tests and debug/release lint. Builds fail rather than silently producing an unsigned release if signing input is missing.

Verify the final APK with SDK `apksigner verify --verbose --print-certs` and `zipalign -c -P 16 4`. Verify the AAB with JDK `jarsigner -verify`. Match both signing certificate fingerprints to the published public certificate. Generate checksums only after all artifacts are final.

## Publish

1. Update `CHANGELOG.md` and the versioned notes in `docs/releases/`.
2. Commit with the repository's neutral project attribution. Review the staged files; private signing material must be absent.
3. Create an annotated version tag on the verified source commit and push it.
4. Run `python scripts/publish-release.py --version 0.1.0 --assets /path/to/public/release-assets`.
5. Verify the resulting GitHub release and every uploaded asset.

The publishing script verifies the remote tag, checks public artifact checksums, and uploads only its explicit public-file allowlist. It does not create credentials, publish private signing material, overwrite release assets, or bypass network policy.

GitHub release creation needs `api.github.com` and asset upload needs `uploads.github.com`. These destinations were blocked by the active cloud network policy during preparation. Domain additions are saved in the environment draft; review/save and publish the environment settings before retrying when required. Existing Git push access does not establish GitHub API access.

## Attribution cleanup limits

The project uses `StashDrop Contributors` for local commit/tag metadata. The two original commits were rewritten with neutral author/committer fields and unchanged file trees. Old local reflogs and unreachable objects were pruned. Force-updating a branch removes the old identities from reachable project history; it does not erase third-party clones, forks, GitHub caches, account profiles, or this chat. GitHub support/account controls may be needed for externally retained copies.
