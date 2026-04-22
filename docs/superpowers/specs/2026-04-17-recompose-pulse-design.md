# Recompose Pulse Design

## 背景

目标是在 Compose Multiplatform 生态里提供一套可复用的“重组脉冲高亮”能力，满足以下约束：

- 运行时视觉效果尽量非侵入
- 项目侧接入改动尽量少
- 第一版先在 JVM/Desktop 上完成端到端验证
- 运行时逻辑可复用为 Kotlin Multiplatform library
- 编译器注入与运行时开关可以组合使用

现状是仓库为空目录，因此本设计同时定义第一版的最小工程结构。

## 目标

- 提供一个 KMP runtime 库，在 `commonMain` 中实现重组脉冲的运行时能力
- 提供一个 Kotlin compiler plugin，自动向可安全改写的 `@Composable` 调用注入脉冲 `Modifier`
- 提供一个很薄的 Gradle plugin，负责依赖接入与编译参数透传
- 提供一个 Compose Desktop sample，用于验证自动注入、全局开关和局部禁用

## 非目标

- 第一版不承诺 Android、iOS、Web 全平台验证完成
- 第一版不尝试覆盖所有 `@Composable` 调用
- 第一版不通过自动包裹 `Box` 或其他布局节点来扩大覆盖面
- 第一版不提供 IDE 插件或 Layout Inspector 集成
- 第一版不做复杂策略扩展，例如自定义 tag 模板或按任意注解字符串过滤

## 总体架构

第一版工程拆分为 5 个模块：

1. `recompose-pulse-runtime`
   Kotlin Multiplatform 库。职责是提供运行时配置、`CompositionLocal`、脉冲绘制节点、手动 `Modifier` API。

2. `recompose-pulse-annotations`
   极薄注解模块。职责是承载 `@NoRecomposePulse` 等编译期过滤标记，避免 `runtime` 与 `compiler` 直接耦合。

3. `recompose-pulse-compiler`
   Kotlin compiler plugin。职责是识别可安全改写的 `@Composable` 调用，并向 `modifier` 参数注入 `recomposePulseModifier()`。

4. `recompose-pulse-gradle`
   很薄的 Gradle plugin。职责是自动加 `runtime` 与 `annotations` 依赖、注册 compiler plugin artifact、提供少量 DSL 配置。

5. `sample-desktop`
   Compose Desktop 演示工程。职责是验证自动注入、运行时开关、局部禁用和视觉表现。

## 模块边界与依赖关系

- `recompose-pulse-runtime` 不依赖 `recompose-pulse-compiler`
- `recompose-pulse-compiler` 依赖 `recompose-pulse-annotations`，并在 IR 改写时引用 `recompose-pulse-runtime` 暴露的 API 符号
- `recompose-pulse-gradle` 负责把 `runtime`、`annotations` 和 compiler plugin 一起接到消费工程
- `sample-desktop` 只通过 Gradle plugin 与公开 runtime API 消费能力

这样拆分的原因是：

- runtime 必须保持 KMP 纯净，不携带编译器实现细节
- 注解独立后，业务代码和插件都能稳定引用，依赖方向清晰
- Gradle plugin 保持薄层，只负责“接线”，不承载业务逻辑
- sample 单独存在，便于做端到端验证和后续回归

## 编译器注入规则

第一版 compiler plugin 只实现一条强约束规则：

`只改写带 modifier: Modifier 参数的 @Composable 调用`

### 改写行为

1. 调用点显式传入了 `modifier`

原始调用：

```kotlin
Card(
    modifier = Modifier.padding(8.dp),
) { ... }
```

改写后：

```kotlin
Card(
    modifier = Modifier
        .padding(8.dp)
        .then(recomposePulseModifier("HomeScreen.kt:42:Card")),
) { ... }
```

2. 调用点未传 `modifier`，但目标函数签名存在 `modifier: Modifier = Modifier`

原始调用：

```kotlin
Text(text = "Hello")
```

改写后：

```kotlin
Text(
    text = "Hello",
    modifier = recomposePulseModifier("HomeScreen.kt:58:Text"),
)
```

3. 目标函数存在名为 `modifier` 的参数，但类型不是 Compose `Modifier`

- 跳过，不改写

4. 目标函数不存在 `modifier` 参数

- 跳过，不改写

### 过滤规则

以下情况明确跳过：

- `runtime` 自身实现包，避免自我递归注入
- 标注了 `@NoRecomposePulse` 的函数或类
- 不在 `includePackages` 范围内的调用点
- 命中 `excludePackages` 的调用点

### 不做的事情

- 不自动包裹 `Box` 或其他布局容器
- 不尝试基于全局 `Recomposer` 事件做统一拦截
- 不承诺覆盖所有 composable，只覆盖可安全注入 `modifier` 的调用

### Tag 规则

插件为每次注入生成一个轻量 tag，默认格式为：

```text
<FileName>:<LineNumber>:<ComposableName>
```

例如：

```text
HomeScreen.kt:42:Card
```

tag 的职责仅是辅助排查和调试，不作为功能正确性的核心依赖。

## Runtime API 设计

第一版 runtime 提供以下公开 API：

```kotlin
@Composable
fun ProvideRecomposePulse(
    enabled: Boolean,
    style: RecomposePulseStyle = RecomposePulseStyle(),
    content: @Composable () -> Unit,
)

@Composable
fun DisableRecomposePulse(
    content: @Composable () -> Unit,
)

@Composable
fun recomposePulseModifier(
    tag: String? = null,
): Modifier

fun Modifier.recomposePulse(
    style: RecomposePulseStyle = RecomposePulseStyle(),
    tag: String? = null,
): Modifier
```

配套类型：

```kotlin
@Immutable
data class RecomposePulseStyle(
    val color: Color = Color(0xFFFFD54F),
    val maxAlpha: Float = 0.10f,
    val durationMillis: Int = 140,
    val borderWidth: Dp = 2.dp,
    val mode: Mode = Mode.Fill,
) {
    enum class Mode { Fill, Border }
}

@Immutable
data class RecomposePulseConfig(
    val enabled: Boolean = false,
    val style: RecomposePulseStyle = RecomposePulseStyle(),
)
```

## Runtime 实现策略

runtime 基于 `Modifier.Node` 与 `DrawModifierNode` 实现，不引入额外布局层级。

关键策略如下：

- 使用 `ModifierNodeElement` 驱动节点更新
- `equals()` 故意返回 `false`，确保每次 recomposition 都会进入 `update()`
- 首次 composition 不触发闪烁，只在后续 recomposition 时启动脉冲动画
- draw 阶段只绘制一层短时、淡色的 overlay 或 border
- 不参与布局测量、不改变输入处理、不包装额外容器

### 默认视觉行为

默认样式选择：

- `mode = Fill`
- `color = Color(0xFFFFD54F)`
- `maxAlpha = 0.10f`
- `durationMillis = 140`

默认使用淡色 fill overlay，而不是红框或粗边框，原因是：

- 视觉上更克制，更接近“观察工具”而不是“性能警报”
- 不容易被误解为错误态或选中态
- 对中大尺寸组件更容易感知“刚发生过重组”

保留 `Border` 模式，是为了兼容透明背景或 fill 不明显的场景。

## 运行时开关模型

运行时控制分两层：

1. 根级开关

```kotlin
ProvideRecomposePulse(enabled = true) {
    App()
}
```

2. 局部子树禁用

```kotlin
DisableRecomposePulse {
    ExpensiveAnimatedArea()
}
```

`recomposePulseModifier()` 设计为 `@Composable` 函数，这样编译器注入后可以在调用点直接读取 `CompositionLocal`，把“哪里注入”和“当前是否启用”分离开：

- 编译期决定注入位置
- 运行时决定是否启用以及采用何种样式

## 注解设计

第一版注解模块只提供一个公开注解：

```kotlin
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class NoRecomposePulse
```

用途：

- 对敏感 composable 或整类组件树显式排除自动注入
- 为 sample 和测试提供可验证的排除路径

## Gradle Plugin 设计

Gradle plugin 保持薄层，公开 DSL：

```kotlin
recomposePulse {
    enabled.set(true)
    debugOnly.set(true)
    includePackages.add("com.example")
    excludePackages.add("com.example.generated")
}
```

### 职责

- 自动向消费工程加入 `recompose-pulse-runtime`
- 自动向消费工程加入 `recompose-pulse-annotations`
- 向 Kotlin 编译器注册 `recompose-pulse-compiler`
- 把 `enabled`、`debugOnly`、`includePackages`、`excludePackages` 转换为 compiler plugin 参数

### 配置语义

- `enabled`
  控制是否注册 compiler plugin

- `debugOnly`
  默认 `true`。Desktop sample 中映射到开发运行场景；后续扩展到 Android 时再映射到 debug variant

- `includePackages`
  白名单。默认空表示不过滤。sample 中应显式配置业务包，避免误处理第三方代码

- `excludePackages`
  黑名单，用于跳过生成代码、预览代码和敏感区域

第一版不提供更多策略项，优先保证闭环稳定。

## 工程结构

建议第一版目录如下：

```text
compose-recompose-pulse/
  build.gradle.kts
  settings.gradle.kts
  gradle.properties
  gradle/libs.versions.toml
  docs/superpowers/specs/
  recompose-pulse-annotations/
  recompose-pulse-runtime/
  recompose-pulse-compiler/
  recompose-pulse-gradle/
  sample-desktop/
```

## 测试策略

测试分为 4 层。

### 1. Runtime 单元测试

覆盖点：

- 默认配置与默认样式值
- `ProvideRecomposePulse` 下 `enabled` 状态透传
- `DisableRecomposePulse` 对子树禁用是否生效
- 手动 `Modifier.recomposePulse()` 与 `recomposePulseModifier()` 的开关行为

这层以纯逻辑和组合配置为主，不把图形输出像素级比对作为第一版目标。

### 2. Compiler Plugin 盒测

覆盖点：

- 已显式传入 `modifier` 的调用会被追加 `.then(recomposePulseModifier(...))`
- 未传 `modifier` 但目标函数有默认 `modifier` 参数时会补入参数
- 无 `modifier` 参数的 composable 不会被改写
- 标注 `@NoRecomposePulse` 的函数或类不会被改写
- 命中 `includePackages` 和 `excludePackages` 的行为符合预期

### 3. Gradle Plugin 集成测试

覆盖点：

- 插件能被 sample 工程成功应用
- `runtime` 与 `annotations` 依赖会自动加入
- compiler plugin 参数能透传到编译阶段
- sample 能成功编译

### 4. Sample Desktop 手工验收

验收步骤：

- 运行 sample
- 触发状态变更，引发局部 recomposition
- 观察目标组件出现轻微 pulse 效果
- 将 `ProvideRecomposePulse(enabled = false)` 后确认效果消失
- 在 `DisableRecomposePulse` 子树内再次触发重组，确认该子树不再闪烁

## 第一版验收标准

- 能发布 `recompose-pulse-runtime` KMP 库
- 能发布 JVM 可消费的 `recompose-pulse-compiler`
- 能发布很薄的 `recompose-pulse-gradle`
- `sample-desktop` 以最小项目改动看到自动注入的 pulse 效果
- 运行时支持全局开启、全局关闭、局部子树关闭
- 插件明确跳过没有 `modifier` 参数的 composable
- 第一版仅以 JVM/Desktop 端到端验证为完成标准

## 风险与约束

- Kotlin compiler plugin API 存在版本演进风险，需要跟随 Kotlin 版本维护
- 第一版从空目录起步，必须优先固定 Kotlin 与 Compose 版本组合，避免调试成本扩散
- 若未来要扩展 Android、iOS、Web，优先复用 runtime，再逐步扩展消费和验证矩阵

## 推荐推进顺序

1. 建立多模块 Gradle 工程与版本约束
2. 实现 `recompose-pulse-runtime` 与手动 API
3. 实现 `recompose-pulse-annotations`
4. 实现 `recompose-pulse-compiler` 的最小 IR 注入闭环
5. 实现 `recompose-pulse-gradle` 的接线能力
6. 搭建 `sample-desktop` 并完成端到端验证
7. 完成运行时、编译器与 Gradle 集成测试
