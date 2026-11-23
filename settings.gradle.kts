pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.7.3"
        id("org.jetbrains.kotlin.android") version "2.2.21"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
        id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // Robolectric's Android system jars (about 150 MB each) from Google's mirror of Maven Central: Maven
        // Central itself often answers these big downloads with 429 Too Many Requests, which failed the build.
        exclusiveContent {
            forRepository {
                maven("https://maven-central.storage-download.googleapis.com/maven2/") { name = "MavenCentralMirror" }
            }
            filter { includeModule("org.robolectric", "android-all-instrumented") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "AIWidgetMaker"
include(":app")
