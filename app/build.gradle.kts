plugins {
    id("app.cash.paparazzi")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
}

val releaseSigningNames = listOf(
    "STASHDROP_RELEASE_KEYSTORE", "STASHDROP_RELEASE_KEY_ALIAS",
    "STASHDROP_RELEASE_STORE_PASSWORD", "STASHDROP_RELEASE_KEY_PASSWORD"
)
val releaseSigningValues = releaseSigningNames.associateWith { providers.environmentVariable(it).orNull }
val hasReleaseSigning = releaseSigningValues.values.all { !it.isNullOrBlank() }

android {
    namespace = "com.vibe.stashdrop"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.vibe.stashdrop"
        minSdk = 28
        targetSdk = 35
        versionCode = 11
        versionName = "0.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; aidl = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    if (hasReleaseSigning) {
        signingConfigs.create("production") {
            storeFile = file(releaseSigningValues.getValue("STASHDROP_RELEASE_KEYSTORE")!!)
            storePassword = releaseSigningValues.getValue("STASHDROP_RELEASE_STORE_PASSWORD")
            keyAlias = releaseSigningValues.getValue("STASHDROP_RELEASE_KEY_ALIAS")
            keyPassword = releaseSigningValues.getValue("STASHDROP_RELEASE_KEY_PASSWORD")
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("production")
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true; unitTests.isIncludeAndroidResources = true }
}
kapt { correctErrorTypes = true }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.03.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.datastore:datastore-preferences:1.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
}

val requireReleaseSigning by tasks.registering {
    doLast {
        val missing = releaseSigningNames.filter { releaseSigningValues[it].isNullOrBlank() }
        check(missing.isEmpty()) { "Release signing is required. Missing environment variable names: ${missing.joinToString()}. See docs/RELEASING.md." }
        check(file(releaseSigningValues.getValue("STASHDROP_RELEASE_KEYSTORE")!!).isFile) { "Release keystore file does not exist." }
    }
}
tasks.matching { it.name == "preReleaseBuild" }.configureEach { dependsOn(requireReleaseSigning) }
