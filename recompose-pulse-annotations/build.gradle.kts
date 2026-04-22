@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
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
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
        }
    }
}

android {
    namespace = "com.adamglin.recompose.pulse.annotations"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

publishing {
    repositories {
        mavenLocal()
    }
}
