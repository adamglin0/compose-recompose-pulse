# Recompose Pulse Multiplatform Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend Recompose Pulse so its runtime and annotations libraries publish `android`、`iosArm64`、`iosSimulatorArm64`、`js`、`wasmJs`、`jvm`, and make the Gradle plugin apply the compiler plugin to every compilation whenever `enabled = true`.

**Architecture:** Keep `recompose-pulse-runtime` and `recompose-pulse-annotations` as the only Kotlin Multiplatform libraries that change shape, and add Android library configuration only where those modules need it. Keep `recompose-pulse-gradle` as a JVM Gradle plugin, but simplify its applicability policy so KMP target names no longer matter; wiring stays in `commonMainImplementation` while applicability becomes a pure `enabled` decision.

**Tech Stack:** Gradle 8.10.2, Kotlin 2.0.21, Android Gradle Plugin 8.5.2, Compose Multiplatform 1.7.3, JUnit 5, Kotlin test, Gradle TestKit, ASM

---

## File Structure

### Root build configuration

- Modify: `gradle/libs.versions.toml`
  Add the Android Gradle Plugin version and plugin alias.

- Modify: `build.gradle.kts`
  Register `com.android.library` with `apply false` so the KMP library modules can opt in cleanly.

### `recompose-pulse-runtime`

- Modify: `recompose-pulse-runtime/build.gradle.kts`
  Expand the target matrix to `androidTarget`、`iosArm64`、`iosSimulatorArm64`、`js(IR)`、`wasmJs`、`jvm`, and add Android library configuration.

- Create: `recompose-pulse-runtime/src/androidMain/AndroidManifest.xml`
  Provide the minimal manifest required by the Android library target.

### `recompose-pulse-annotations`

- Modify: `recompose-pulse-annotations/build.gradle.kts`
  Mirror the runtime module's target matrix and Android library setup.

- Create: `recompose-pulse-annotations/src/androidMain/AndroidManifest.xml`
  Provide the minimal manifest required by the Android library target.

### `recompose-pulse-gradle`

- Create: `recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicability.kt`
  Isolate the "enabled means every compilation" policy into a tiny internal unit that takes the old decision inputs explicitly.

- Modify: `recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseGradlePlugin.kt`
  Replace the current JVM/debug-based applicability logic with the new policy helper.

- Create: `recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicabilityTest.kt`
  Unit-test the new applicability policy.

- Create: `recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseMultiplatformGradlePluginTest.kt`
  Add a KMP-focused Gradle TestKit smoke test that checks `commonMain` dependency wiring and all configured target compile tasks.

### Documentation

- Modify: `README.md`
  Replace the JVM/Desktop-only framing with a multiplatform support matrix, KMP usage instructions, and the new `enabled` semantics.

## Task 1: Expand The Runtime And Annotation Target Matrix

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `recompose-pulse-runtime/build.gradle.kts`
- Create: `recompose-pulse-runtime/src/androidMain/AndroidManifest.xml`
- Modify: `recompose-pulse-annotations/build.gradle.kts`
- Create: `recompose-pulse-annotations/src/androidMain/AndroidManifest.xml`

- [ ] **Step 1: Run the failing build-smoke commands first**

Run:

```bash
./gradlew \
  :recompose-pulse-runtime:compileKotlinIosArm64 \
  :recompose-pulse-runtime:compileKotlinIosSimulatorArm64 \
  :recompose-pulse-runtime:compileKotlinJs \
  :recompose-pulse-runtime:compileKotlinWasmJs \
  :recompose-pulse-runtime:compileDebugKotlinAndroid \
  :recompose-pulse-annotations:compileKotlinIosArm64 \
  :recompose-pulse-annotations:compileKotlinIosSimulatorArm64 \
  :recompose-pulse-annotations:compileKotlinJs \
  :recompose-pulse-annotations:compileKotlinWasmJs \
  :recompose-pulse-annotations:compileDebugKotlinAndroid
```

Expected: FAIL with missing tasks such as `compileKotlinIosArm64` or Android plugin configuration errors, proving the target matrix does not exist yet.

- [ ] **Step 2: Add the Android plugin version and root plugin alias**

```toml
# File: gradle/libs.versions.toml
[versions]
kotlin = "2.0.21"
compose = "1.7.3"
agp = "8.5.2"
junit = "5.11.3"
truth = "1.4.4"
asm = "9.7.1"
compileTesting = "0.7.0"

[libraries]
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
kotlin-compiler-embeddable = { module = "org.jetbrains.kotlin:kotlin-compiler-embeddable", version.ref = "kotlin" }
kotlin-gradle-plugin-api = { module = "org.jetbrains.kotlin:kotlin-gradle-plugin-api", version.ref = "kotlin" }
kotlin-compile-testing = { module = "dev.zacsweers.kctfork:core", version.ref = "compileTesting" }
truth = { module = "com.google.truth:truth", version.ref = "truth" }
asm = { module = "org.ow2.asm:asm", version.ref = "asm" }
asm-commons = { module = "org.ow2.asm:asm-commons", version.ref = "asm" }
asm-tree = { module = "org.ow2.asm:asm-tree", version.ref = "asm" }

[plugins]
androidLibrary = { id = "com.android.library", version.ref = "agp" }
kotlinJvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "compose" }
kotlinCompose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

```kotlin
// File: build.gradle.kts
plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinCompose) apply false
}

tasks.register("publishPulseToMavenLocal") {
    dependsOn(
        ":recompose-pulse-annotations:publishToMavenLocal",
        ":recompose-pulse-runtime:publishToMavenLocal",
        ":recompose-pulse-compiler:publishToMavenLocal",
        ":recompose-pulse-gradle:publishToMavenLocal",
    )
}

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()

    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("${project.group}:recompose-pulse-annotations"))
                .using(project(":recompose-pulse-annotations"))
            substitute(module("${project.group}:recompose-pulse-compiler"))
                .using(project(":recompose-pulse-compiler"))
            substitute(module("${project.group}:recompose-pulse-runtime"))
                .using(project(":recompose-pulse-runtime"))
        }
    }
}
```

- [ ] **Step 3: Expand `recompose-pulse-runtime` to the full KMP target set**

```kotlin
// File: recompose-pulse-runtime/build.gradle.kts
@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.android.build.gradle.LibraryExtension
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinCompose)
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
        commonMain.dependencies {
            api(compose.runtime)
            api(compose.ui)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(kotlin("test-junit5"))
            implementation(compose.desktop.currentOs)
            implementation(compose.uiTestJUnit4)
        }
    }
}

extensions.configure<LibraryExtension> {
    namespace = "com.adamglin.recomposepulse.runtime"
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
```

```xml
<!-- File: recompose-pulse-runtime/src/androidMain/AndroidManifest.xml -->
<manifest package="com.adamglin.recomposepulse.runtime" />
```

- [ ] **Step 4: Mirror the same target matrix in `recompose-pulse-annotations`**

```kotlin
// File: recompose-pulse-annotations/build.gradle.kts
@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import com.android.build.gradle.LibraryExtension
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
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

extensions.configure<LibraryExtension> {
    namespace = "com.adamglin.recomposepulse.annotations"
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
```

```xml
<!-- File: recompose-pulse-annotations/src/androidMain/AndroidManifest.xml -->
<manifest package="com.adamglin.recomposepulse.annotations" />
```

- [ ] **Step 5: Re-run the same build-smoke commands and verify they pass**

Run:

```bash
./gradlew \
  :recompose-pulse-runtime:compileKotlinIosArm64 \
  :recompose-pulse-runtime:compileKotlinIosSimulatorArm64 \
  :recompose-pulse-runtime:compileKotlinJs \
  :recompose-pulse-runtime:compileKotlinWasmJs \
  :recompose-pulse-runtime:compileDebugKotlinAndroid \
  :recompose-pulse-annotations:compileKotlinIosArm64 \
  :recompose-pulse-annotations:compileKotlinIosSimulatorArm64 \
  :recompose-pulse-annotations:compileKotlinJs \
  :recompose-pulse-annotations:compileKotlinWasmJs \
  :recompose-pulse-annotations:compileDebugKotlinAndroid
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the library target-matrix changes**

```bash
git add build.gradle.kts gradle/libs.versions.toml recompose-pulse-runtime/build.gradle.kts recompose-pulse-runtime/src/androidMain/AndroidManifest.xml recompose-pulse-annotations/build.gradle.kts recompose-pulse-annotations/src/androidMain/AndroidManifest.xml
git commit -m "build: add multiplatform library targets"
```

## Task 2: Make The Gradle Plugin Apply To Every Enabled Compilation

**Files:**
- Create: `recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicability.kt`
- Modify: `recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseGradlePlugin.kt`
- Create: `recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicabilityTest.kt`
- Create: `recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseMultiplatformGradlePluginTest.kt`

- [ ] **Step 1: Write the failing tests first**

```kotlin
// File: recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicabilityTest.kt
package com.adamglin.recomposepulse.gradle

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PulseCompilerApplicabilityTest {
    @Test
    fun `enabled applies compiler plugin to ios main even when debugOnly is true`() {
        assertTrue(
            PulseCompilerApplicability.shouldApply(
                enabled = true,
                debugOnly = true,
                targetName = "iosArm64",
                compilationName = "main",
            ),
        )
    }

    @Test
    fun `enabled applies compiler plugin to js main even when debugOnly is true`() {
        assertTrue(
            PulseCompilerApplicability.shouldApply(
                enabled = true,
                debugOnly = true,
                targetName = "js",
                compilationName = "main",
            ),
        )
    }

    @Test
    fun `disabled skips compiler plugin for every compilation`() {
        assertFalse(
            PulseCompilerApplicability.shouldApply(
                enabled = false,
                debugOnly = false,
                targetName = "jvm",
                compilationName = "main",
            ),
        )
    }
}
```

```kotlin
// File: recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseMultiplatformGradlePluginTest.kt
package com.adamglin.recomposepulse.gradle

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
                    substitute(module("com.adamglin:recompose-pulse-annotations"))
                        .using(project(":recompose-pulse-annotations"))
                    substitute(module("com.adamglin:recompose-pulse-compiler"))
                        .using(project(":recompose-pulse-compiler"))
                    substitute(module("com.adamglin:recompose-pulse-runtime"))
                        .using(project(":recompose-pulse-runtime"))
                }
            }

            rootProject.name = "mpp-plugin-test"
            """.trimIndent(),
        )

        writeFile(
            "build.gradle.kts",
            """
            plugins {
                kotlin("multiplatform") version "2.0.21"
                id("com.android.library") version "8.5.2"
                id("com.adamglin.recompose-pulse")
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
                includePackages.add("sample")
            }
            """.trimIndent(),
        )

        writeFile(
            "src/commonMain/kotlin/androidx/compose/runtime/Composable.kt",
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
            "src/commonMain/kotlin/androidx/compose/ui/Modifier.kt",
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
            "src/commonMain/kotlin/sample/App.kt",
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

        writeFile(
            "src/androidMain/AndroidManifest.xml",
            """
            <manifest package="sample.mpp" />
            """.trimIndent(),
        )

        val dependencies = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("dependencies", "--configuration", "commonMainCompileDependenciesMetadata")
            .build()

        assertThat(dependencies.output).contains("recompose-pulse-runtime")
        assertThat(dependencies.output).contains("recompose-pulse-annotations")

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
```

- [ ] **Step 2: Run the tests and verify they fail for the expected reasons**

Run:

```bash
./gradlew :recompose-pulse-gradle:test --tests "com.adamglin.recomposepulse.gradle.PulseCompilerApplicabilityTest" --tests "com.adamglin.recomposepulse.gradle.PulseMultiplatformGradlePluginTest"
```

Expected: FAIL because `PulseCompilerApplicability` does not exist yet, which proves the new applicability policy is not implemented.

- [ ] **Step 3: Add the applicability policy helper**

```kotlin
// File: recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicability.kt
package com.adamglin.recomposepulse.gradle

internal object PulseCompilerApplicability {
    fun shouldApply(
        enabled: Boolean,
        debugOnly: Boolean,
        targetName: String,
        compilationName: String,
    ): Boolean {
        return enabled
    }
}
```

- [ ] **Step 4: Simplify `PulseGradlePlugin.isApplicable()` to use the new policy**

```kotlin
// File: recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseGradlePlugin.kt
package com.adamglin.recomposepulse.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class PulseGradlePlugin : KotlinCompilerPluginSupportPlugin, Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("recomposePulse", PulseGradleExtension::class.java)

        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            target.dependencies.add("implementation", PulseGradleMetadata.runtimeArtifact)
            target.dependencies.add("implementation", PulseGradleMetadata.annotationsArtifact)
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.dependencies.add("commonMainImplementation", PulseGradleMetadata.runtimeArtifact)
            target.dependencies.add("commonMainImplementation", PulseGradleMetadata.annotationsArtifact)
        }
    }

    override fun getCompilerPluginId(): String {
        return "com.adamglin.recompose-pulse.compiler"
    }

    override fun getPluginArtifact(): SubpluginArtifact {
        return PulseGradleMetadata.compilerArtifact.toSubpluginArtifact()
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PulseGradleExtension::class.java)
        return PulseCompilerApplicability.shouldApply(
            enabled = extension.enabled.get(),
            debugOnly = extension.debugOnly.get(),
            targetName = kotlinCompilation.target.targetName,
            compilationName = kotlinCompilation.compilationName,
        )
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PulseGradleExtension::class.java)
        return project.provider {
            buildList {
                add(SubpluginOption("enabled", extension.enabled.get().toString()))
                extension.includePackages.get().forEach { add(SubpluginOption("includePackage", it)) }
                extension.excludePackages.get().forEach { add(SubpluginOption("excludePackage", it)) }
            }
        }
    }

    private fun String.toSubpluginArtifact(): SubpluginArtifact {
        val coordinates = split(":")
        require(coordinates.size == 3) { "Invalid artifact coordinates: $this" }
        return SubpluginArtifact(
            groupId = coordinates[0],
            artifactId = coordinates[1],
            version = coordinates[2],
        )
    }
}
```

- [ ] **Step 5: Re-run the plugin tests and verify they pass**

Run:

```bash
./gradlew :recompose-pulse-gradle:test --tests "com.adamglin.recomposepulse.gradle.PulseCompilerApplicabilityTest" --tests "com.adamglin.recomposepulse.gradle.PulseMultiplatformGradlePluginTest" --tests "com.adamglin.recomposepulse.gradle.PulseGradlePluginTest"
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit the plugin applicability changes**

```bash
git add recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicability.kt recompose-pulse-gradle/src/main/kotlin/io/github/yourorg/recomposepulse/gradle/PulseGradlePlugin.kt recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseCompilerApplicabilityTest.kt recompose-pulse-gradle/src/test/kotlin/io/github/yourorg/recomposepulse/gradle/PulseMultiplatformGradlePluginTest.kt
git commit -m "fix: apply pulse plugin to all enabled compilations"
```

## Task 3: Document The Multiplatform Matrix And Run The Final Verification Suite

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Rewrite the README around multiplatform support**

```markdown
# Compose Recompose Pulse

`Compose Recompose Pulse` 是一个本地可安装的 Compose 重组脉冲高亮插件方案，包含 4 个发布物：

- `recompose-pulse-annotations`
- `recompose-pulse-runtime`
- `recompose-pulse-compiler`
- `recompose-pulse-gradle`

当前版本：`0.1.0-SNAPSHOT`

## 支持矩阵

当前仓库对以下 Kotlin Multiplatform targets 提供库产物与 Gradle 插件接线支持：

- `android`
- `iosArm64`
- `iosSimulatorArm64`
- `js`
- `wasmJs`
- `jvm`

这里的“支持”表示：

- `recompose-pulse-runtime` 与 `recompose-pulse-annotations` 已声明这些 targets
- `com.adamglin.recompose-pulse` 会自动补入 runtime 与 annotations 依赖
- 编译器插件会在 `enabled = true` 时应用到所有 compilations

当前仓库没有同时提供 Android、iOS、JS、Wasm 的完整 sample 应用，因此支持矩阵的验证方式以库构建与插件接线为主。

## 发布到 Maven Local

先把全部发布物安装到本机 `~/.m2/repository`：

```bash
./gradlew --no-daemon publishPulseToMavenLocal
```

这条命令会发布：

- `com.adamglin:recompose-pulse-annotations:0.1.0-SNAPSHOT`
- `com.adamglin:recompose-pulse-runtime:0.1.0-SNAPSHOT`
- `com.adamglin:recompose-pulse-compiler:0.1.0-SNAPSHOT`
- `com.adamglin:recompose-pulse-gradle:0.1.0-SNAPSHOT`
- Gradle 插件 marker：`com.adamglin.recompose-pulse:com.adamglin.recompose-pulse.gradle.plugin:0.1.0-SNAPSHOT`

如果你重复发布同一个 `0.1.0-SNAPSHOT`，消费项目建议加一次依赖刷新：

```bash
./gradlew --refresh-dependencies compileKotlin
```

## 在其他项目中安装

### 1. 在 `settings.gradle.kts` 中启用仓库

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
```

### 2. 在消费项目中应用插件

下面示例以 Kotlin Multiplatform 为例：

```kotlin
plugins {
    kotlin("multiplatform") version "2.0.21"
    id("com.android.library") version "8.5.2"
    id("com.adamglin.recompose-pulse") version "0.1.0-SNAPSHOT"
}

repositories {
    mavenLocal()
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
    namespace = "com.example.app"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
}

recomposePulse {
    enabled.set(true)
    includePackages.add("com.example.app")
}
```

应用插件后，会自动补入：

- `recompose-pulse-runtime`
- `recompose-pulse-annotations`
- 编译器插件 `recompose-pulse-compiler`

当 `enabled = true` 时，Gradle 插件会把编译器插件应用到所有 targets 的 compilations。`debugOnly` 字段目前保留在 DSL 中仅用于兼容旧配置，不再作为多平台过滤条件。

### 3. 在代码中启用 Provider

```kotlin
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.adamglin.recomposepulse.ProvideRecomposePulse
import com.adamglin.recomposepulse.RecomposePulseStyle

@Composable
fun App() {
    ProvideRecomposePulse(
        enabled = true,
        style = RecomposePulseStyle(),
    ) {
        MaterialTheme {
            Surface {
            }
        }
    }
}
```

### 4. 可选：跳过某些函数或子树

跳过整个函数或类：

```kotlin
import com.adamglin.recomposepulse.NoRecomposePulse

@NoRecomposePulse
@Composable
fun ExpensiveComposable() {
}
```

只禁用某个子树：

```kotlin
import androidx.compose.runtime.Composable
import com.adamglin.recomposepulse.DisableRecomposePulse

@Composable
fun Screen() {
    DisableRecomposePulse {
    }
}
```

## 只手动使用 Runtime API

如果你暂时不想用 Gradle 插件，也可以只引入 runtime：

```kotlin
dependencies {
    implementation("com.adamglin:recompose-pulse-runtime:0.1.0-SNAPSHOT")
    implementation("com.adamglin:recompose-pulse-annotations:0.1.0-SNAPSHOT")
}
```

但推荐优先使用 `com.adamglin.recompose-pulse` Gradle 插件，因为它会自动完成运行时和编译器接线。
```

- [ ] **Step 2: Run the final verification suite**

Run:

```bash
./gradlew :recompose-pulse-runtime:test :recompose-pulse-annotations:test :recompose-pulse-compiler:test :recompose-pulse-gradle:test publishPulseToMavenLocal
```

Expected: `BUILD SUCCESSFUL`, and the output publishes `recompose-pulse-runtime` plus `recompose-pulse-annotations` to Maven Local without dropping the new target matrix.

- [ ] **Step 3: Commit the README and final verification state**

```bash
git add README.md
git commit -m "docs: describe multiplatform pulse support"
```
