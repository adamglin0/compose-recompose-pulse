@file:OptIn(
    org.jetbrains.compose.ExperimentalComposeLibrary::class,
    org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class,
)

import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    js(IR)
    wasmJs()
    jvm()
    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
            implementation(compose.desktop.currentOs)
            implementation(libs.compose.ui.test.junit4)
        }
    }
}

android {
    namespace = "com.adamglin.recompose.pulse.runtime"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
