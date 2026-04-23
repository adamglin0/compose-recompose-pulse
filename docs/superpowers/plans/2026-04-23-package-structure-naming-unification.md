# Package Structure Naming Unification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将整个仓库的源码包结构、Gradle 模块名、Maven 坐标和插件标识统一到 `com.adamglin.recompose.pulse` 规则下，不保留旧命名兼容。

**Architecture:** 保持公共 Kotlin API 位于 `com.adamglin.recompose.pulse`，将仓库内部模块名统一为短名 `annotations`、`runtime`、`compiler`、`gradle`，同时把发布层切换到 `com.adamglin.recompose.pulse:<short-artifact>`。Gradle 插件和编译器插件 id 同步切到点号风格，再通过现有测试和本地发布链路做回归验证。

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Gradle Plugin Development, Maven Publish, JUnit 5, Truth

---

## 约束

- 按用户要求，不使用 TDD
- 不保留旧坐标、旧插件 id、旧模块名兼容层
- 不在本计划中创建 git commit，除非用户单独要求

## 文件结构

### 根构建与仓库结构

- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle.properties`
- Modify: `README.md`
- Create: `annotations/build.gradle.kts`
- Create: `runtime/build.gradle.kts`
- Create: `compiler/build.gradle.kts`
- Create: `gradle/build.gradle.kts`
- Delete: `recompose-pulse-annotations/build.gradle.kts`
- Delete: `recompose-pulse-runtime/build.gradle.kts`
- Delete: `recompose-pulse-compiler/build.gradle.kts`
- Delete: `recompose-pulse-gradle/build.gradle.kts`

### 模块源码与测试

- Move: `recompose-pulse-annotations/src/**` -> `annotations/src/**`
- Move: `recompose-pulse-runtime/src/**` -> `runtime/src/**`
- Move: `recompose-pulse-compiler/src/**` -> `compiler/src/**`
- Move: `recompose-pulse-gradle/src/**` -> `gradle/src/**`
- Modify: `sample-desktop/build.gradle.kts`
- Modify: `sample-desktop/src/main/kotlin/com/adamglin/recompose/pulse/sample/App.kt`
- Modify: `sample-desktop/src/main/kotlin/com/adamglin/recompose/pulse/sample/Main.kt`

### 重点校验文件

- Modify: `gradle/src/main/kotlin/com/adamglin/recompose/pulse/gradle/PulseGradlePlugin.kt`
- Modify: `compiler/src/main/kotlin/com/adamglin/recompose/pulse/compiler/PulseCommandLineProcessor.kt`
- Modify: `gradle/src/test/kotlin/com/adamglin/recompose/pulse/gradle/PulseGradlePluginTest.kt`
- Modify: `gradle/src/test/kotlin/com/adamglin/recompose/pulse/gradle/PulseMultiplatformGradlePluginTest.kt`
- Modify: `compiler/src/test/kotlin/com/adamglin/recompose/pulse/compiler/CompilerPluginCompilation.kt`

### Task 1: 重命名 Gradle 模块与根工程引用

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Modify: `gradle.properties`
- Create: `annotations/build.gradle.kts`
- Create: `runtime/build.gradle.kts`
- Create: `compiler/build.gradle.kts`
- Create: `gradle/build.gradle.kts`
- Delete: `recompose-pulse-annotations/build.gradle.kts`
- Delete: `recompose-pulse-runtime/build.gradle.kts`
- Delete: `recompose-pulse-compiler/build.gradle.kts`
- Delete: `recompose-pulse-gradle/build.gradle.kts`

- [ ] **Step 1: 修改根工程 include 与 includeBuild 路径**

```kotlin
// settings.gradle.kts
rootProject.name = "compose-recompose-pulse"

includeBuild("gradle") {
    name = "recompose-pulse-plugin-build"

    dependencySubstitution {
        substitute(module("com.adamglin.recompose.pulse:gradle"))
            .using(project(":"))
    }
}

include(
    ":annotations",
    ":runtime",
    ":compiler",
    ":gradle",
    ":sample-desktop",
)
```

- [ ] **Step 2: 修改根构建中的发布映射与依赖替换**

```kotlin
// build.gradle.kts
val publishedModules = mapOf(
    ":annotations" to PublishedModule(
        artifactId = "annotations",
        pomName = "Compose Recompose Pulse Annotations",
        pomDescription = "Annotations for opting out of Compose Recompose Pulse instrumentation.",
    ),
    ":runtime" to PublishedModule(
        artifactId = "runtime",
        pomName = "Compose Recompose Pulse Runtime",
        pomDescription = "Runtime APIs and pulse overlay rendering for Compose Recompose Pulse.",
    ),
    ":compiler" to PublishedModule(
        artifactId = "compiler",
        pomName = "Compose Recompose Pulse Compiler",
        pomDescription = "Compiler plugin that injects Compose Recompose Pulse instrumentation.",
    ),
    ":gradle" to PublishedModule(
        artifactId = "gradle",
        pomName = "Compose Recompose Pulse Gradle Plugin",
        pomDescription = "Gradle plugin that wires Compose Recompose Pulse dependencies and compiler integration.",
    ),
)

configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("${project.group}:annotations")).using(project(":annotations"))
        substitute(module("${project.group}:compiler")).using(project(":compiler"))
        substitute(module("${project.group}:runtime")).using(project(":runtime"))
    }
}
```

- [ ] **Step 3: 修改发布 group**

```properties
# gradle.properties
GROUP=com.adamglin.recompose.pulse
```

- [ ] **Step 4: 将四个模块的 `build.gradle.kts` 迁移到新目录并更新 project 引用**

Run: `git mv recompose-pulse-annotations annotations && git mv recompose-pulse-runtime runtime && git mv recompose-pulse-compiler compiler && git mv recompose-pulse-gradle gradle`

Expected: 四个模块目录出现在仓库根目录，后续所有 `project(":...")` 都改为短模块名。

### Task 2: 更新模块构建脚本与发布元数据

**Files:**
- Modify: `annotations/build.gradle.kts`
- Modify: `runtime/build.gradle.kts`
- Modify: `compiler/build.gradle.kts`
- Modify: `gradle/build.gradle.kts`

- [ ] **Step 1: 修正 compiler 模块对 annotations 的工程依赖**

```kotlin
// compiler/build.gradle.kts
dependencies {
    implementation(project(":annotations"))
    implementation(libs.kotlin.compiler.embeddable)
    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.truth)
}
```

- [ ] **Step 2: 保持 runtime 与 annotations 的 namespace 为点号风格**

```kotlin
// runtime/build.gradle.kts
android {
    namespace = "com.adamglin.recompose.pulse.runtime"
    compileSdk = 35
}

// annotations/build.gradle.kts
android {
    namespace = "com.adamglin.recompose.pulse.annotations"
    compileSdk = 35
}
```

- [ ] **Step 3: 修改 Gradle 插件模块的生成元数据**

```kotlin
// gradle/build.gradle.kts
file.writeText(
    """
    package com.adamglin.recompose.pulse.gradle

    internal object PulseGradleMetadata {
        const val group = "${project.group}"
        const val version = "${project.version}"
        const val compilerArtifact = "${project.group}:compiler:${project.version}"
        const val runtimeArtifact = "${project.group}:runtime:${project.version}"
        const val annotationsArtifact = "${project.group}:annotations:${project.version}"
    }
    """.trimIndent(),
)
```

- [ ] **Step 4: 修改 Gradle plugin id**

```kotlin
// gradle/build.gradle.kts
gradlePlugin {
    plugins {
        create("recomposePulse") {
            id = "com.adamglin.recompose.pulse"
            implementationClass = "com.adamglin.recompose.pulse.gradle.PulseGradlePlugin"
        }
    }
}
```

### Task 3: 更新插件实现与编译器插件 id

**Files:**
- Modify: `gradle/src/main/kotlin/com/adamglin/recompose/pulse/gradle/PulseGradlePlugin.kt`
- Modify: `compiler/src/main/kotlin/com/adamglin/recompose/pulse/compiler/PulseCommandLineProcessor.kt`

- [ ] **Step 1: 修改 Gradle 插件返回的 compiler plugin id**

```kotlin
// gradle/src/main/kotlin/com/adamglin/recompose/pulse/gradle/PulseGradlePlugin.kt
override fun getCompilerPluginId(): String {
    return "com.adamglin.recompose.pulse.compiler"
}
```

- [ ] **Step 2: 修改编译器插件常量**

```kotlin
// compiler/src/main/kotlin/com/adamglin/recompose/pulse/compiler/PulseCommandLineProcessor.kt
companion object {
    const val PLUGIN_ID = "com.adamglin.recompose.pulse.compiler"
}
```

- [ ] **Step 3: 重新检查所有字符串常量是否仍残留短横线风格**

Run: `rg 'recompose-pulse|com\.adamglin:recompose-pulse|com\.adamglin\.recompose-pulse' README.md settings.gradle.kts build.gradle.kts gradle.properties annotations runtime compiler gradle sample-desktop`

Expected: 仅历史文档或本次设计文档中保留旧命名引用，源码与当前使用文档中不再残留。

### Task 4: 迁移源码与测试到新模块目录

**Files:**
- Move: `recompose-pulse-annotations/src/**` -> `annotations/src/**`
- Move: `recompose-pulse-runtime/src/**` -> `runtime/src/**`
- Move: `recompose-pulse-compiler/src/**` -> `compiler/src/**`
- Move: `recompose-pulse-gradle/src/**` -> `gradle/src/**`
- Move: `recompose-pulse-runtime/src/androidMain/AndroidManifest.xml` -> `runtime/src/androidMain/AndroidManifest.xml`
- Move: `recompose-pulse-annotations/src/androidMain/AndroidManifest.xml` -> `annotations/src/androidMain/AndroidManifest.xml`

- [ ] **Step 1: 迁移 runtime 和 annotations 源码目录**

Run: `git mv recompose-pulse-runtime/src runtime/src && git mv recompose-pulse-annotations/src annotations/src`

Expected: 运行时与注解模块源码位于 `runtime/src`、`annotations/src`。

- [ ] **Step 2: 迁移 compiler 和 gradle 源码目录**

Run: `git mv recompose-pulse-compiler/src compiler/src && git mv recompose-pulse-gradle/src gradle/src`

Expected: 编译器与 Gradle 插件源码位于 `compiler/src`、`gradle/src`。

- [ ] **Step 3: 校验包声明无需改动但物理路径已与新模块对齐**

Run: `rg '^package ' annotations runtime compiler gradle sample-desktop --glob '*.kt'`

Expected: 包声明仍以 `com.adamglin.recompose.pulse` 为主，不出现新的错误包名。

### Task 5: 更新 sample、README 与功能测试 fixture

**Files:**
- Modify: `sample-desktop/build.gradle.kts`
- Modify: `README.md`
- Modify: `gradle/src/test/kotlin/com/adamglin/recompose/pulse/gradle/PulseGradlePluginTest.kt`
- Modify: `gradle/src/test/kotlin/com/adamglin/recompose/pulse/gradle/PulseMultiplatformGradlePluginTest.kt`
- Modify: `compiler/src/test/kotlin/com/adamglin/recompose/pulse/compiler/CompilerPluginCompilation.kt`

- [ ] **Step 1: 修改 sample-desktop 的 classpath 与 plugin id**

```kotlin
// sample-desktop/build.gradle.kts
buildscript {
    dependencies {
        classpath("com.adamglin.recompose.pulse:gradle:0.1.1")
    }
}

apply(plugin = "com.adamglin.recompose.pulse")
```

- [ ] **Step 2: 修改 README 中的安装与发布示例**

```markdown
The Gradle plugin id is `com.adamglin.recompose.pulse`.

```kotlin
plugins {
    id("com.adamglin.recompose.pulse") version "0.1.1"
}

dependencies {
    implementation("com.adamglin.recompose.pulse:runtime:0.1.1")
    implementation("com.adamglin.recompose.pulse:annotations:0.1.1")
}
```

- plugin marker: `com.adamglin.recompose.pulse:com.adamglin.recompose.pulse.gradle.plugin:0.1.1`
```

- [ ] **Step 3: 修改 Gradle 功能测试中的 substitution、插件 id 和依赖断言**

```kotlin
// gradle/src/test/kotlin/com/adamglin/recompose/pulse/gradle/PulseGradlePluginTest.kt
substitute(module("com.adamglin.recompose.pulse:annotations"))
    .using(project(":annotations"))
substitute(module("com.adamglin.recompose.pulse:compiler"))
    .using(project(":compiler"))
substitute(module("com.adamglin.recompose.pulse:runtime"))
    .using(project(":runtime"))

id("com.adamglin.recompose.pulse")

assertThat(dependencies.output).contains("runtime")
assertThat(dependencies.output).contains("annotations")
```

- [ ] **Step 4: 修改 compiler 测试中的内嵌引用与示例坐标**

Run: `rg 'recompose-pulse|com\.adamglin:recompose-pulse|com\.adamglin\.recompose-pulse' compiler/src/test gradle/src/test sample-desktop README.md`

Expected: 这些位置全部切换到新命名。

### Task 6: 运行验证并清理剩余旧引用

**Files:**
- Modify: `README.md` if final examples need correction
- Modify: `sample-desktop/build.gradle.kts` if verification reveals遗漏
- Modify: `gradle/src/test/kotlin/com/adamglin/recompose/pulse/gradle/PulseGradlePluginTest.kt` if dependency assertions need correction

- [ ] **Step 1: 运行快速 grep 验证**

Run: `rg 'com\.adamglin\.recompose-pulse|com\.adamglin:recompose-pulse|recompose-pulse-(annotations|runtime|compiler|gradle)' README.md settings.gradle.kts build.gradle.kts gradle.properties annotations runtime compiler gradle sample-desktop`

Expected: 无匹配。

- [ ] **Step 2: 运行模块测试**

Run: `./gradlew :annotations:jvmTest :runtime:jvmTest :compiler:test :gradle:test`

Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 运行 sample 编译验证**

Run: `./gradlew :sample-desktop:compileKotlin`

Expected: sample 成功解析 `com.adamglin.recompose.pulse` 插件与新坐标。

- [ ] **Step 4: 运行本地发布验证**

Run: `./gradlew publishPulseToMavenLocal`

Expected: `$HOME/.m2/repository/com/adamglin/recompose/pulse/runtime/<version>/runtime-<version>.pom` 等产物存在。

- [ ] **Step 5: 检查最终工作区**

Run: `git status --short`

Expected: 仅包含本次模块重命名、构建脚本、文档和测试更新。
