import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// Auto-incrementing build version, persisted in app/version.properties.
// Every `assemble` bumps the counter: 1st build -> 1.0, 2nd -> 1.1, 3rd -> 1.2 ...
val versionPropsFile = file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) versionPropsFile.inputStream().use { load(it) }
}
var buildNumber = (versionProps.getProperty("build") ?: "0").toInt()
if (gradle.startParameter.taskNames.any { it.contains("assemble", ignoreCase = true) }) {
    buildNumber += 1
    versionProps.setProperty("build", buildNumber.toString())
    versionPropsFile.outputStream().use { versionProps.store(it, "Auto-incremented on each assemble") }
}
val appVersionCode = maxOf(buildNumber, 1)
val appVersionName = "1.${appVersionCode - 1}"

android {
    namespace = "com.example.slsHrms"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.slsHrms"
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Ship only the ABIs real handsets use. The offline-sync work added two
        // native libraries (SQLCipher ~5.8 MB, TFLite ~3.4 MB) and they were
        // being packaged four times over — x86/x86_64 exist for emulators and
        // cost ~38 MB in an APK that gets sideloaded onto shop-floor phones.
        // Testing on an x86 emulator? Add "x86_64" back here.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    // Release signing. Keystore + passwords live in keystore.properties (gitignored,
    // never commit it) — without that file the release build stays unsigned.
    val keystoreProps = Properties().apply {
        val f = rootProject.file("keystore.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }

    // Phones in the field carry the RELEASE build (release keystore), and a
    // debug APK cannot be installed over it without wiping the app's data. So
    // the on-device tests (run.ps1) target the release variant instead.
    testBuildType = "release"

    // The TFLite face model must stay uncompressed so it can be memory-mapped
    // straight out of the APK instead of being unpacked to disk first.
    androidResources {
        noCompress += "tflite"
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "VowHrms_v$appVersionName.apk"
        }
    }
}

// Room schema exports — needed for migration tests and for diffing what a
// schema change actually did.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core:1.15.0")
        force("androidx.core:core-ktx:1.15.0")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.cardview)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // Charts
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Live camera + on-device face detection (auto face validation)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.face.detection)

    // Device location for attendance geo-tagging
    implementation(libs.play.services.location)

    // Offline-first: local queue + background sync + on-device face embedding
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.work.runtime.ktx)
    implementation(libs.coroutines.android)
    implementation(libs.tensorflow.lite)
    // Face embeddings are biometric data — the Room file is SQLCipher-encrypted
    // with a key held in the Android Keystore (see DatabaseKey.kt).
    implementation(libs.sqlcipher)
    implementation(libs.androidx.sqlite)
    implementation(libs.security.crypto)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}