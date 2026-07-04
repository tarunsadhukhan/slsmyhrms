import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
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
    }

    buildTypes {
        release {
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

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "VowHrms_v$appVersionName.apk"
        }
    }
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

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}