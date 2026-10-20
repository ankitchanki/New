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

## Writable Robolectric test runtime

The 27-test suite includes Robolectric service/circle regressions. Its default download lock is under Java's `user.home`, which is read-only in this managed workspace. Configure the test JVM's home and Maven cache inside `/workspace` before running Gradle. This keeps the normal `HOME` environment variable unchanged and forwards the managed proxy and CA trust settings to test JVMs.

The prepared instance already has this Gradle init script outside Git. To recreate it in the documented `/workspace/New` checkout after activating `cloud-env.sh` and running `configure-cloud-gradle.py`:

```sh
mkdir -p /workspace/.gradle/init.d /workspace/.java-test-home /workspace/.m2/repository
cat > /workspace/.gradle/init.d/stashdrop-test-runtime.gradle <<'EOF'
// Workspace-local test runtime configuration; no app/test sources are changed.
gradle.rootProject { root ->
    if (root.projectDir.canonicalPath == '/workspace/New') {
        root.allprojects { project ->
            project.tasks.withType(org.gradle.api.tasks.testing.Test).configureEach {
                systemProperty 'user.home', '/workspace/.java-test-home'
                systemProperty 'maven.repo.local', '/workspace/.m2/repository'
                ['http.proxyHost', 'http.proxyPort', 'https.proxyHost', 'https.proxyPort',
                 'http.nonProxyHosts', 'javax.net.ssl.trustStore'].each { propertyName ->
                    def configuredValue = System.getProperty(propertyName)
                    if (configuredValue != null) systemProperty propertyName, configuredValue
                }
            }
        }
    }
}
EOF
```

The complete signed 0.3.2 build and all 27 tests passed with this configuration. No test sources, assertions, TLS checks, or artifact verification were disabled. The original production signing key remains outside the repository; follow [RELEASING.md](RELEASING.md) for private signing inputs.
