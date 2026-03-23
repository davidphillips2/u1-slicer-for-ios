plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.2"
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets
    val iosFrameworkName = "SharedModule"
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    // Configure iOS framework export
    iosTargets.forEach {
        it.binaries.framework {
            baseName = iosFrameworkName
            isStatic = true

            // Export dependencies for iOS
            export(libs.kotlinx.coroutines.core)
            export(libs.kotlinx.serialization.json)
            export(libs.ktor.client.core)

            // Transitive exports
            export(libs.ktor.client.content.negotiation)
            export(libs.ktor.serialization.kotlinx.json)
            export(libs.sql.delight.native.driver)
        }
    }

    // CocoaPods integration (optional, for manual Podfile setup)
    cocoapods {
        summary = "U1 Slicer shared code"
        homepage = "https://github.com/davidphillips2/u1-slicer-for-ios"
        ios.deploymentTarget = "14.0"
        podfile = project.file("../ios/Podfile")
        framework {
            baseName = iosFrameworkName
            isStatic = true
            // Export dependencies
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            export("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
        }
    }

    // Create a shared iOS source set
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)

                // Ktor for networking
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific implementations
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.room.runtime)
                implementation(libs.androidx.room.ktx)
                implementation(libs.androidx.datastore)
                implementation(libs.sql.delight.android.driver)
            }
        }

        // Create iOS source set from all iOS targets
        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // iOS-specific implementations
                implementation(libs.sql.delight.native.driver)
            }
        }

        // Make all iOS targets depend on iosMain
        iosTargets.forEach { target ->
            target.compilations.getByName("main").defaultSourceSet.dependsOn(iosMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

// Android configuration
android {
    namespace = "com.u1.slicer.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// SQLDelight configuration
sqldelight {
    databases {
        create("U1SlicerDatabase") {
            packageName.set("com.u1.slicer.shared.database")
        }
    }
}
