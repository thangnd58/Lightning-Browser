buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    extra.apply {
        set("minSdkVersion", 19)
        set("targetSdkVersion", 35)
        set("buildToolsVersion", 35)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
