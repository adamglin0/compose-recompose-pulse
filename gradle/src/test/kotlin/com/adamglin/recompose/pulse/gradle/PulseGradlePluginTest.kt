package com.adamglin.recompose.pulse.gradle

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

class PulseGradlePluginTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun compileClasspathIncludesRuntimeAndAnnotationsAndProjectCompiles() {
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

            rootProject.name = "test-project"
            """.trimIndent(),
        )

        writeFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("jvm") version "2.3.20"
                id("com.adamglin.recompose.pulse")
            }

            repositories {
                google()
                mavenCentral()
            }

            recomposePulse {
                enabled.set(true)
                includePackages.add("sample")
            }
            """.trimIndent(),
        )

        writeFile(
            "src/main/kotlin/androidx/compose/runtime/Composable.kt",
            """
            package androidx.compose.runtime

            @Target(AnnotationTarget.FUNCTION)
            annotation class Composable
            """.trimIndent(),
        )

        writeFile(
            "src/main/kotlin/androidx/compose/ui/Modifier.kt",
            """
            package androidx.compose.ui

            interface Modifier {
                infix fun then(other: Modifier): Modifier = CombinedModifier(this, other)

                companion object : Modifier {
                    val Default: Modifier = this
                }
            }

            data class CombinedModifier(
                val outer: Modifier,
                val inner: Modifier,
            ) : Modifier
            """.trimIndent(),
        )

        writeFile(
            "src/main/kotlin/sample/App.kt",
            """
            package sample

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @Composable
            fun App(modifier: Modifier = Modifier.Default) {
                modifier.hashCode()
            }
            """.trimIndent(),
        )

        val dependencies = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("dependencies", "--configuration", "compileClasspath")
            .build()

        assertThat(dependencies.output).contains("com.adamglin.recompose.pulse:runtime")
        assertThat(dependencies.output).contains("com.adamglin.recompose.pulse:annotations")
        assertThat(dependencies.output).contains("BUILD SUCCESSFUL")

        val compile = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("compileKotlin")
            .build()

        assertThat(compile.output).contains("BUILD SUCCESSFUL")
    }

    @Test
    fun plainJvmCompilationInjectsPulseModifierWhenDebugOnlyUsesDefault() {
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

            rootProject.name = "plain-jvm-test-project"
            """.trimIndent(),
        )

        writeFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("jvm") version "2.3.20"
                id("com.adamglin.recompose.pulse")
            }

            repositories {
                google()
                mavenCentral()
            }

            recomposePulse {
                enabled.set(true)
                includePackages.add("sample")
            }
            """.trimIndent(),
        )

        writeFile(
            "src/main/kotlin/androidx/compose/runtime/Composable.kt",
            """
            package androidx.compose.runtime

            @Target(
                AnnotationTarget.FUNCTION,
                AnnotationTarget.TYPE,
                AnnotationTarget.TYPE_PARAMETER,
            )
            annotation class Composable
            """.trimIndent(),
        )

        writeFile(
            "src/main/kotlin/androidx/compose/ui/Modifier.kt",
            """
            package androidx.compose.ui

            interface Modifier {
                infix fun then(other: Modifier): Modifier = CombinedModifier(this, other)

                companion object : Modifier
            }

            data class CombinedModifier(
                val outer: Modifier,
                val inner: Modifier,
            ) : Modifier
            """.trimIndent(),
        )

        writeFile(
            "src/main/kotlin/sample/App.kt",
            """
            package sample

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @Composable
            fun PulseTarget(modifier: Modifier = Modifier) {
                modifier.hashCode()
            }

            @Composable
            fun App() {
                PulseTarget()
            }
            """.trimIndent(),
        )

        val compile = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("compileKotlin")
            .build()

        assertThat(compile.output).contains("BUILD SUCCESSFUL")
        assertThat(
            methodContainsPulseModifierCall(
                classFile = projectDir.resolve("build/classes/kotlin/main/sample/AppKt.class"),
                methodName = "App",
            ),
        ).isTrue()
    }

    private fun writeFile(relativePath: String, content: String) {
        val file = projectDir.resolve(relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun File.toKotlinString(): String {
        return "\"${invariantSeparatorsPath}\""
    }

    private fun methodContainsPulseModifierCall(classFile: File, methodName: String): Boolean {
        val classNode = ClassNode()
        ClassReader(classFile.readBytes()).accept(classNode, 0)
        return classNode.methods
            .firstOrNull { it.name == methodName }
            ?.instructions
            ?.asSequence()
            ?.filterIsInstance<MethodInsnNode>()
            ?.any { it.name == "recomposePulseModifier" } == true
    }
}
