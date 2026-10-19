#!/usr/bin/env bash
# Source this file in the prepared Codex cloud snapshot.
export JAVA_HOME="${STASHDROP_JAVA_HOME:-/workspace/toolchains/jdk-21.0.12.1}"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/android-sdk}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-/workspace/.android}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/.gradle}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:/workspace/toolchains/gradle-8.11.1/bin:$PATH"
