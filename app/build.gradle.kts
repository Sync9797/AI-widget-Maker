import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.sidequest.widgetmaker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sidequest.widgetmaker"
        minSdk = 26
        targetSdk = 35
        versionCode = 36
        versionName = "0.10.4"
        // Which commit this is (set by the GitHub build), so the app can tell when a newer build is out.
        buildConfigField("String", "GIT_SHA", "\"${System.getenv("GITHUB_SHA") ?: "dev"}\"")
    }

    signingConfigs {
        // A fixed debug key checked into the repo, so every CI build installs over the previous one.
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("debug") }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
        }
    }

    lint { checkReleaseBuilds = false }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests { isIncludeAndroidResources = true }
    }

    packaging {
        resources {
            // The Claude SDK's dependencies (OkHttp, Jackson) ship overlapping license files.
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/INDEX.LIST",
                "META-INF/versions/9/previous-compilation-data.bin",
                "/META-INF/{AL2.0,LGPL2.1}",
            )
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    // The widgets themselves.
    implementation("androidx.glance:glance-appwidget:1.1.1")

    // Live data: background refresh and HTTP for weather, crypto, traffic, news and events.
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Exercise and health: Samsung Health, WHOOP, Google Fit and others share through Health Connect.
    implementation("androidx.health.connect:connect-client:1.1.0-alpha10")

    // Claude, which designs the widgets.
    implementation("com.anthropic:anthropic-java:3.8.0")

    testImplementation("junit:junit:4.13.2")
    // Renders previews on the JVM, to look at designs without a phone.
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

// -PshotsOnly="Title,Title" draws just those starters in StarterShots.
tasks.withType<Test>().configureEach {
    (project.findProperty("shotsOnly") as String?)?.let { systemProperty("shots.only", it) }
    // -PshotsApp draws each section of the app (AppShots).
    if (project.hasProperty("shotsApp")) systemProperty("shots.app", "1")
}
