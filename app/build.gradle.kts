plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    //id("com.anthonycr.plugins.mezzanine") version "2.4.0"
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
    }

    buildTypes {
        named("debug") {
            multiDexEnabled = true
            isMinifyEnabled = false
            isShrinkResources = false
            setProguardFiles(listOf("proguard-project.txt"))
        }

        named("release") {
            multiDexEnabled = true
            isMinifyEnabled = !isCi
            isShrinkResources = !isCi
            setProguardFiles(listOf("proguard-project.txt"))

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
        abortOnError = false
    }
    
    namespace = "acr.browser.lightning"
}

dependencies {
    val okhttp = "3.12.13"
    val kotlin = "1.9.22"
    val coroutines = "1.7.3"

    // Core AndroidX - API 19 compatible
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core:1.12.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.fragment:fragment:1.6.2")
    implementation("androidx.lifecycle:lifecycle-common:2.7.0")
    implementation("androidx.palette:palette:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("androidx.annotation:annotation:1.8.0")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Networking
    implementation("com.squareup.okhttp3:okhttp:$okhttp")
    implementation("com.squareup.okio:okio:1.15.0")
    
    // Dependency Injection
    implementation("com.google.dagger:dagger:2.51.1")
    kapt("com.google.dagger:dagger-compiler:2.51.1")
    
    // Mezzanine
    //implementation("com.anthonycr.mezzanine:core:2.4.0")
    //ksp("com.anthonycr.mezzanine:processor:2.4.0")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:$coroutines")
    
    // Utilities
    implementation("org.jsoup:jsoup:1.15.4")
    implementation("javax.inject:javax.inject:1")
    implementation("com.jakewharton.timber:timber:5.0.1")
    
    // Debug
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.13")
    compileOnly("javax.annotation:jsr250-api:1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutines")
}

kotlin {
    jvmToolchain(11)
}
