buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    extra.apply {
        set("minSdkVersion", 19)  // Android 4.4.2 support
        set("targetSdkVersion", 30)
        set("buildToolsVersion", 30)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "9.2.1" apply false
    id("com.github.ben-manes.versions") version "0.54.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.4.0" apply false
}
