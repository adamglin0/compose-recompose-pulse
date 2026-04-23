package com.adamglin.recompose.pulse.gradle

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class PulseMultiplatformGradlePluginTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun multiplatformProjectWiresCommonMainDependenciesAndConfiguresAllTargets() {
        val repoRoot = File("..").canonicalFile

        writeFile(
            "settings.gradle.kts",
            """
            pluginManagement {
                includeBuild(${repoRoot.toKotlinString()})
                repositories {
                    google()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }

            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }

            includeBuild(${repoRoot.toKotlinString()}) {
                dependencySubstitution {
                    substitute(module("com.adamglin.recompose.pulse:annotations"))
                        .using(project(":annotations"))
                    substitute(module("com.adamglin.recompose.pulse:compiler"))
                        .using(project(":compiler"))
                    substitute(module("com.adamglin.recompose.pulse:runtime"))
                        .using(project(":runtime"))
                }
            }

            rootProject.name = "mpp-plugin-test"
            """.trimIndent(),
        )

        writeFile(
            "gradle.properties",
            """
            android.useAndroidX=true
            org.jetbrains.compose.experimental.jscanvas.enabled=true
            """.trimIndent(),
        )

        writeFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("multiplatform") version "2.3.20"
                id("com.android.library") version "8.5.2"
                id("com.adamglin.recompose.pulse")
            }

            repositories {
                google()
                mavenCentral()
            }

            kotlin {
                androidTarget()
                iosArm64()
                iosSimulatorArm64()
                js(IR)
                @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
                wasmJs()
                jvm()
            }

            android {
                namespace = "sample.mpp"
                compileSdk = 35
                defaultConfig {
                    minSdk = 24
                }
            }

            recomposePulse {
                enabled.set(true)
                debugOnly.set(true)
                includePackages.add("sample")
            }
            """.trimIndent(),
        )

        val dependencies = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("dependencies", "--configuration", "commonMainImplementation")
            .build()

        assertThat(dependencies.output).contains("com.adamglin.recompose.pulse:runtime")
        assertThat(dependencies.output).contains("com.adamglin.recompose.pulse:annotations")

        val tasks = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("tasks", "--all")
            .build()

        assertThat(tasks.output).contains("compileKotlinIosArm64")
        assertThat(tasks.output).contains("compileKotlinIosSimulatorArm64")
        assertThat(tasks.output).contains("compileKotlinJs")
        assertThat(tasks.output).contains("compileKotlinWasmJs")
        assertThat(tasks.output).contains("compileKotlinJvm")
        assertThat(tasks.output).contains("compileDebugKotlinAndroid")
    }

    private fun writeFile(relativePath: String, content: String) {
        val file = projectDir.resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun File.toKotlinString(): String {
        return "\"${invariantSeparatorsPath}\""
    }
}
