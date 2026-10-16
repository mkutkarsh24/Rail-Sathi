import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Developer-local configuration. local.properties is git-ignored; CI can use environment variables.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun config(name: String, default: String = ""): String =
    localProps.getProperty(name) ?: System.getenv(name) ?: default

android {
    namespace = "com.railsathi.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.railsathi.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "com.railsathi.app.HiltTestRunner"

        // Base URL of YOUR railway data backend (see docs/PROVIDER_SETUP.md). Must be https://.
        // Leave blank to run in clearly-labelled sample-data (mock) mode.
        buildConfigField("String", "RAIL_API_BASE_URL", "\"${config("RAIL_API_BASE_URL")}\"")
    }

    val releaseStoreFile = config("RELEASE_STORE_FILE")
    if (releaseStoreFile.isNotBlank()) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = config("RELEASE_STORE_PASSWORD")
                keyAlias = config("RELEASE_KEY_ALIAS")
                keyPassword = config("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseStoreFile.isNotBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Safety net: a release built without a data-service URL would silently ship in sample-data mode.
tasks.whenTaskAdded {
    if (name == "bundleRelease" || name == "assembleRelease") {
        doFirst {
            if (config("RAIL_API_BASE_URL").isBlank() && !project.hasProperty("allowSampleRelease")) {
                throw GradleException(
                    "RAIL_API_BASE_URL is blank, so this release would ship in sample-data mode. " +
                        "Set it in local.properties, or pass -PallowSampleRelease to build anyway.",
                )
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.gson)

    implementation(libs.kotlinx.coroutines.android)

    // Unit tests (JVM)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    // Instrumented tests (device / emulator)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
