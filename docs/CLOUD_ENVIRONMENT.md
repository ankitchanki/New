# Prepared cloud toolchain

The prepared filesystem contains checksum-verified Gradle 8.11.1 and Oracle JDK 21.0.12.1 under `/workspace/toolchains`, Android command-line tools 19, platform 35, build-tools 35.0.0, and platform-tools under `/workspace/android-sdk`. Java/Android/Gradle caches use writable workspace directories.

In a task restored from the prepared snapshot:

```sh
cd /workspace/New
. scripts/cloud-env.sh
python scripts/configure-cloud-gradle.py
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Use the existing isolated checkout. Do not create a worktree unless explicitly requested. There is no host web server or database service to start: Room runs inside the Android app. Install and launch the APK on an attached Android device to exercise the application.

`configure-cloud-gradle.py` selects the managed proxy hostname/port from `HTTPS_PROXY` and the system Java CA trust store. It preserves unrelated Gradle properties, stores no credential values, and does not disable TLS verification. Run it when restoring to a task with a different proxy configuration.

These workspace paths refer to the prepared snapshot, not a universal SDK installation. On a separate machine, install your own JDK/Android SDK and follow the standard build instructions in the README. No Android emulator or physical device is attached to this cloud instance. Snapshot publication and restoration into a fresh task are controlled by the environment UI and have not been verified by the application build.
