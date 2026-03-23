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
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

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
