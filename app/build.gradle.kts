plugins {
    id("com.android.application")
    id("com.github.ben-manes.versions")
    id("com.google.devtools.ksp") version "2.3.9"
    id("com.anthonycr.plugins.mezzanine") version "2.4.0"
    id("com.autonomousapps.dependency-analysis") version "3.16.0"
    id("com.squareup.sort-dependencies") version "0.19.0"
}

android {
    compileSdk = 35

    defaultConfig {
        minSdk = 19  // Android 4.4.2 (KitKat)
        targetSdk = 35
        versionName = "5.2.0"
        vectorDrawables.useSupportLibrary = true
    }

    val isCi = System.getenv("CI") == "true"

    sourceSets {
        create("lightningPlus").apply {
            setRoot("src/LightningPlus")
        }
        if (!isCi) {
            create("lightningLite").apply {
                setRoot("src/LightningLite")
            }
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = false  // Disabled - not compatible with API 19
    }

    buildTypes {
        named("debug") {
            multiDexEnabled = true
            isMinifyEnabled = false
            isShrinkResources = false
            setProguardFiles(listOf("proguard-project.txt"))
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false
        }

        named("release") {
            multiDexEnabled = true
            isMinifyEnabled = !isCi
            isShrinkResources = !isCi
            setProguardFiles(listOf("proguard-project.txt"))
            enableUnitTestCoverage = false
            enableAndroidTestCoverage = false

            ndk {
                abiFilters.add("armeabi-v7a")
                abiFilters.add("arm64-v8a")
                abiFilters.add("x86")
            }
        }
    }

    flavorDimensions.add("capabilities")

    productFlavors {
        create("lightningPlus") {
            dimension = "capabilities"
            buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"true\")")
            applicationId = "acr.browser.lightning"
            versionCode = 101
        }

        if (!isCi) {
            create("lightningLite") {
                dimension = "capabilities"
                buildConfigField("boolean", "FULL_VERSION", "Boolean.parseBoolean(\"false\")")
                applicationId = "acr.browser.barebones"
                versionCode = 102
            }
        }
    }
    
    packaging {
        resources {
            excludes += listOf(".readme")
        }
    }
    
    lint {
        abortOnError = false  // Relaxed for older API compatibility
    }
    
    namespace = "acr.browser.lightning"
}

dependencies {
    val robolectric = "4.13.0"
    val mezzanineVersion = "2.4.0"
    val daggerVersion = "2.45"
    val kotlin = "1.9.22"
    val coroutines = "1.7.3"
    val lifecycle = "2.7.0"
    val okhttp = "3.12.13"  // Last version supporting API 19

    // Core AndroidX libraries (API 19 compatible)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.annotation:annotation:1.8.0")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.fragment:fragment:1.6.2")
    implementation("androidx.lifecycle:lifecycle-common:$lifecycle")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.webkit:webkit:1.10.0")
    
    // Material Design for API 19
    implementation("com.google.android.material:material:1.11.0")
    
    // Networking (OkHttp 3.x is the last to support API 19)
    implementation("com.squareup.okhttp3:okhttp:$okhttp")
    implementation("com.squareup.okio:okio:1.15.0")
    
    // Dependency Injection
    implementation("com.google.dagger:dagger:$daggerVersion")
    
    // Mezzanine for HTML/JS embedding
    implementation("com.anthonycr.mezzanine:core:$mezzanineVersion")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines")
    
    // Other utilities
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("javax.inject:javax.inject:1")
    
    // Logging
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
    compileOnly("javax.annotation:jsr250-api:1.0")

    // Testing
    testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:3.24.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.robolectric:annotations:4.10.3")
    testImplementation("org.robolectric:robolectric:4.10.3")

    // Code generation
    ksp("com.anthonycr.mezzanine:processor:$mezzanineVersion")
    ksp("com.google.dagger:dagger-compiler:$daggerVersion")
}

mezzanine {
    files = files(
        "src/main/html/list.html",
        "src/main/html/bookmarks.html",
        "src/main/html/homepage.html",
        "src/main/js/InvertPage.js",
        "src/main/js/TextReflow.js",
        "src/main/js/ThemeColor.js"
    )
}

kotlin {
    jvmToolchain(11)  // Reduced from 21 for better compatibility
}
