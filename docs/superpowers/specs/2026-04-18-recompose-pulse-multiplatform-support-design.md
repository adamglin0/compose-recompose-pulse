# Recompose Pulse Multiplatform Support Design

## 背景

当前仓库包含多个模块，其中 `recompose-pulse-runtime` 与 `recompose-pulse-annotations` 已经使用 Kotlin Multiplatform 插件，但两个模块都只声明了 `jvm()` target。`recompose-pulse-gradle` 作为 JVM Gradle 插件，也只在当前实现里对 JVM 或带有 `debug` 命名特征的编译做 compiler plugin 适配。

这导致仓库虽然具备 KMP 形态，但实际无法对 `iosArm64`、`iosSimulatorArm64`、`android`、`wasm`、`js`、`jvm` 提供一致的库产物与插件接线能力。

## 目标

- 让 `recompose-pulse-runtime` 支持 `iosArm64`、`iosSimulatorArm64`、`android`、`wasm`、`js`、`jvm`
- 让 `recompose-pulse-annotations` 支持同一组 targets，并与运行时模块保持一致的发布矩阵
- 让 `com.adamglin.recompose-pulse` Gradle 插件在 KMP consumer 上能把 compiler plugin 统一应用到这些 targets 的所有 compilations
- 补齐 README 中的支持矩阵、KMP 使用方式与必要的 Android/KMP 配置说明
- 提供基础构建验证，证明 target 配置、依赖接线和插件适配同时成立

## 非目标

- 不把 `recompose-pulse-compiler` 重写为多平台模块
- 不新增完整的 Android、iOS、JS 或 Wasm 示例应用
- 不重构整个发布体系或引入新的仓库发布流程
- 不顺手做与多平台支持无关的 runtime API 或 compiler 行为调整

## 总体方案

本次只调整真正承载多平台能力的 3 个层面：KMP 运行时库、KMP 注解库、以及 JVM Gradle 插件的 KMP 接线逻辑。

### 1. `recompose-pulse-runtime`

`recompose-pulse-runtime` 继续作为 Kotlin Multiplatform 库存在，但从当前的单一 `jvm()` 扩展为以下 targets：

- `jvm()`
- `androidTarget()`
- `iosArm64()`
- `iosSimulatorArm64()`
- `js(IR)`
- `wasmJs()`

源码集优先维持现有的 `commonMain` / `commonTest` 结构，不主动引入新的平台专属实现。只有在某个平台因为 Gradle 或 Compose 平台要求必须补充目录时，才做最小的平台特定文件补充，例如 Android manifest。

### 2. `recompose-pulse-annotations`

`recompose-pulse-annotations` 同步扩展为和 `runtime` 一致的 target 集合。由于该模块主要承载注解定义，预期不需要引入平台专属源码，重点是保证构建矩阵、metadata 和发布结果与 `runtime` 对齐。

### 3. `recompose-pulse-gradle`

`recompose-pulse-gradle` 保持 JVM Gradle 插件，不改为多平台模块。Kotlin compiler plugin artifact 本身仍以现有 JVM 模块 `recompose-pulse-compiler` 交付。

这次的关键变化不在 compiler artifact 形态，而在 Gradle 子插件如何判断适用 compilation。当前 `isApplicable()` 逻辑带有明显的 JVM/debug 偏向，会导致 iOS、JS、Wasm 等 target 即使声明成功，也可能根本没有应用 compiler plugin。

因此这次把 `debugOnly` 行为收敛为用户已确认的语义：

- 只要 `enabled = true`，所有 target、所有 compilation 都应用 compiler plugin

这样 `android`、`iosArm64`、`iosSimulatorArm64`、`js`、`wasmJs`、`jvm` 会采用完全一致的接线策略，不再依赖 compilation 名称推断开发态。

## 文件级设计

### `recompose-pulse-runtime/build.gradle.kts`

需要完成以下修改：

- 新增 `androidTarget()`
- 新增 `iosArm64()` 与 `iosSimulatorArm64()`
- 新增 `js(IR)`
- 新增 `wasmJs()`
- 保留 `jvm()` 与 `jvmToolchain(17)`
- 增加 Android library 插件与 `android {}` 配置
- 根据实际需要补充 Android manifest 路径

测试依赖的策略是保守收敛，而不是强行铺满矩阵。当前仓库已有明确 JVM 测试配置，因此优先保留 JVM 侧测试依赖；其余平台先以“可配置、可编译、可发布”为主要验收标准。

### `recompose-pulse-annotations/build.gradle.kts`

需要与 `runtime` 保持一致：

- 新增相同的 KMP targets
- 对 Android 增加必要的 library 配置
- 保留现有 JVM 测试能力
- 不新增无必要的平台专属实现

### `gradle/libs.versions.toml`

需要补充 Android Gradle Plugin 的版本与插件 alias，使两个库模块能够通过版本目录统一引用 `com.android.library`。

### 根 `build.gradle.kts`

需要在根工程 `plugins` 中声明 Android library 插件的 `apply false`。现有 `publishPulseToMavenLocal` 与依赖替换逻辑保持不变，避免这次设计扩散到发布体系重构。

### `PulseGradlePlugin.kt`

`apply()` 中对 `org.jetbrains.kotlin.multiplatform` 的依赖接线方式继续保留在 `commonMainImplementation`，因为 runtime 与 annotations 仍然通过公共源码集进入消费工程。

`isApplicable()` 的职责需要从“按 JVM/debug 推断是否应该启用”改为“直接反映扩展配置是否启用”。也就是说：

- `enabled = false` 时，不对任何 compilation 生效
- `enabled = true` 时，对所有 compilation 生效

`debugOnly` 不再驱动 target 或 compilation 过滤。为了避免误导，实施时应评估是否将其保留为兼容字段但忽略，或直接在 DSL/README 中说明该字段在多平台模式下不再影响行为。优先选择更小的改动，只要最终行为与文档一致即可。

### `README.md`

README 需要从当前“以 Kotlin JVM + Compose Desktop 为例”的表述，调整为“声明支持矩阵 + 给出 KMP 接入方式 + 说明 Android/KMP 仓库配置要求”的结构。

README 至少应明确：

- 当前支持的 target 列表
- 插件会自动补入 `runtime` 与 `annotations`
- 插件会将 compiler plugin 应用到所有 targets 的 compilations
- Android/KMP 工程需要可解析 `google()`、`mavenCentral()`、`mavenLocal()`
- 当前没有随仓库提供新的跨平台 sample

## 行为定义

### 支持矩阵

本次支持的 target 范围固定为：

- `android`
- `iosArm64`
- `iosSimulatorArm64`
- `js`
- `wasmJs`
- `jvm`

这里的“支持”指：

- 库模块已声明对应 KMP target
- Gradle 能解析并配置这些变体
- 插件接线覆盖这些 target 的 compilations
- README 对外说明这些 target 的使用方式和边界

这里的“支持”暂时不包含：

- 仓库内存在每个平台的完整 sample
- 每个平台都具备端到端 UI 演示
- 每个平台都已经有独立的自动化 UI 测试

### `debugOnly` 语义

用户已确认采用最直接的全量启用语义：

- 只要 `enabled = true`，所有 target、所有 compilation 一律启用

这一定义优先级高于现有实现中的 debug/JVM 特判。实施时不应再保留按 target 名称或 compilation 名称分支判断的旧逻辑。

## 验证策略

### 1. 构建级验证

重点验证两个 KMP 库模块已经真正形成新的 target 矩阵：

- `recompose-pulse-runtime`
- `recompose-pulse-annotations`

验证标准是：Gradle 在 target 配置阶段不报错，相关 compile / metadata / publish 任务可以被解析并执行到合理阶段。优先验证库模块，而不是先引入新的 sample 工程。

### 2. 插件接线验证

需要证明 `recompose-pulse-gradle` 在 KMP consumer 上成立以下事实：

- `commonMain` 自动获得 runtime 与 annotations 依赖
- compiler plugin 会对 `android`、`iosArm64`、`iosSimulatorArm64`、`js`、`wasmJs`、`jvm` 的 compilations 生效

验证方式可以沿用现有 Gradle 功能测试或最小 test fixture，不要求本次额外搭建完整跨平台 demo。

### 3. 文档级验证

README 更新后，应能让外部使用者仅通过文档理解：

- 哪些平台被支持
- 插件会自动做什么
- 需要哪些仓库与插件前置条件
- 当前哪些事情仍不在仓库内演示

## 风险与控制措施

### 1. Android 是唯一需要额外 Gradle 生态配置的平台

Android 不只是增加一个 KMP target，还需要 Android library 插件、namespace、SDK 版本与可能的 manifest。因此 Android 相关改动应严格限制在两个 KMP 库模块、版本目录与根插件声明中，不把整个仓库扩展成 Android app 工程。

### 2. Compose Multiplatform API 可用性风险

`recompose-pulse-runtime` 依赖 `compose.runtime` 与 `compose.ui`。理论上这些 API 应可覆盖本次目标平台，但若实施时发现特定平台受当前 Compose/Kotlin 版本限制，应优先做最小的平台定向修补，而不是扩大为大规模源码拆分。

### 3. DSL 兼容性风险

现有 `debugOnly` 字段的旧语义与新设计冲突。实现时需要避免“字段还在，但行为已完全不同”造成的认知偏差。最小策略是保留字段但在实现与 README 中说明其不再用于多平台过滤；如果仓库内影响面很小，也可以在实现中直接简化掉相关分支。

## 实施边界

为了让这次设计保持聚焦，实施阶段只做以下类别的修改：

- KMP 库模块 target 与 Android 配置
- Gradle 插件的多平台 compiler plugin 适配逻辑
- 版本目录与根插件声明
- README 与基础验证

以下工作明确排除在本次实施之外：

- 新的跨平台 sample
- 发布仓库、签名、坐标策略调整
- 与多平台支持无关的 compiler/runtime 行为扩展

## 成功标准

当以下条件同时成立时，本次设计视为达成：

1. `recompose-pulse-runtime` 与 `recompose-pulse-annotations` 均声明并可配置 `android`、`iosArm64`、`iosSimulatorArm64`、`js`、`wasmJs`、`jvm`
2. `com.adamglin.recompose-pulse` 插件对这些 targets 的 compilations 全量应用 compiler plugin
3. README 明确说明支持矩阵、接入方式与当前边界
4. 仓库内有基础构建或功能验证，能够证明新增平台支持不是仅停留在静态配置层面
