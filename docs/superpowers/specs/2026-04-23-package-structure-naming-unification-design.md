# Compose Recompose Pulse 包结构与发布命名统一设计

## 背景

当前仓库的对外命名存在两套规则并存的情况：

- Kotlin 包名与 Android namespace 主要使用 `com.adamglin.recompose.pulse...`
- Maven group、artifactId、Gradle plugin id 仍混用 `com.adamglin`、`recompose-pulse-*`、`com.adamglin.recompose-pulse`

这会导致使用者在源码、文档、发布坐标和插件声明中看到不一致的命名体系，增加理解和接入成本。

## 目标

对整个项目执行一次全量、破坏性的命名统一，不保留任何旧命名兼容层。

统一后的规则如下：

- 公共基础包名：`com.adamglin.recompose.pulse`
- Android namespace：`com.adamglin.recompose.pulse.*`
- Maven group：`com.adamglin.recompose.pulse`
- Maven artifactId：模块短名，例如 `annotations`、`runtime`、`compiler`、`gradle`
- Gradle plugin id：`com.adamglin.recompose.pulse`
- Compiler plugin id：同步改为点号风格，与整体命名保持一致

## 非目标

- 不保留旧的 Maven 坐标兼容发布
- 不保留旧的 Gradle plugin id
- 不添加重定向模块、兼容别名或过渡期桥接代码

## 方案选择

本次采用“全量统一，对外完全切换”的方案。

### 采用方案

- 统一源码包名、namespace、发布 group、artifactId、plugin id、README、sample、测试断言
- 仓库中的模块目录名与 Gradle include 名也一并调整，避免仓库内部与外部命名继续不一致

### 不采用的方案

- 仅修改发布坐标、不修改源码包结构：不能真正完成命名统一
- 保留旧命名兼容层：与本次“完全切换”的要求冲突

## 目标结构

### 模块与发布坐标

| 模块 | 新模块名 | Maven 坐标 |
| --- | --- | --- |
| annotations | `:annotations` | `com.adamglin.recompose.pulse:annotations` |
| runtime | `:runtime` | `com.adamglin.recompose.pulse:runtime` |
| compiler | `:compiler` | `com.adamglin.recompose.pulse:compiler` |
| gradle | `:gradle` | `com.adamglin.recompose.pulse:gradle` |
| sample-desktop | `:sample-desktop` | 不发布 |

### 包名与 namespace

- 运行时公共 API：`com.adamglin.recompose.pulse`
- runtime namespace：`com.adamglin.recompose.pulse.runtime`
- annotations namespace：`com.adamglin.recompose.pulse.annotations`
- compiler 实现包：`com.adamglin.recompose.pulse.compiler`
- gradle 插件实现包：`com.adamglin.recompose.pulse.gradle`
- sample 包：`com.adamglin.recompose.pulse.sample`

### 插件标识

- Gradle plugin id：`com.adamglin.recompose.pulse`
- Compiler plugin id：`com.adamglin.recompose.pulse.compiler`

## 设计细节

### 1. 仓库结构调整

将现有模块目录和 `settings.gradle.kts` 中的模块路径统一改为短名：

- `recompose-pulse-annotations` -> `annotations`
- `recompose-pulse-runtime` -> `runtime`
- `recompose-pulse-compiler` -> `compiler`
- `recompose-pulse-gradle` -> `gradle`

对应修改：

- 根项目 `include(...)`
- `includeBuild("recompose-pulse-gradle")` 路径
- 所有 project path 引用
- 所有 dependency substitution
- 所有测试工程内嵌构建脚本与断言
- 所有 README、示例和 CI 中引用到旧模块路径的内容

### 2. 发布配置调整

根项目发布 group 改为 `com.adamglin.recompose.pulse`。

每个已发布模块的 `artifactId` 改为短名：

- `annotations`
- `runtime`
- `compiler`
- `gradle`

根项目的 `PublishedModule` 映射、`publishPulseToMavenLocal` 聚合任务、依赖替换规则和本地消费测试都需要同步到新的 group 与 artifactId。

### 3. Gradle 插件与编译器插件调整

Gradle 插件对外 id 改为 `com.adamglin.recompose.pulse`。这会影响：

- 插件声明配置
- README 中的安装示例
- sample-desktop 中的 `apply(plugin = ...)`
- Gradle plugin 功能测试中的 fixture build scripts
- plugin marker 坐标说明

编译器插件 id 改为 `com.adamglin.recompose.pulse.compiler`。这会影响：

- `PulseCommandLineProcessor` 常量
- Gradle 插件内部连接 compiler plugin 的实现
- 编译相关测试

### 4. 源码包结构调整

公共 API 继续放在 `com.adamglin.recompose.pulse` 下，不引入额外层级。

实现层包名保持与职责一致：

- compiler: `com.adamglin.recompose.pulse.compiler`
- gradle: `com.adamglin.recompose.pulse.gradle`

若目录路径仍使用旧目录结构，则同步物理移动到与包名一致的目录，避免“声明包名”和“物理路径”长期偏离。

### 5. 文档与样例调整

README、sample-desktop、发布说明、消费者示例全部切换到新命名：

- 新 plugin id
- 新 group/artifact 坐标
- 新 plugin marker 坐标
- 新模块名与目录路径

`docs/superpowers` 下的历史设计/计划文档属于历史记录，不作为兼容承诺来源；本次可以不批量重写历史文档内容，但新增 spec 和后续 plan 采用新命名。

## 数据流与影响面

这次重命名主要影响四条链路：

1. 使用者接入链路
   - `plugins { id("com.adamglin.recompose.pulse") }`
   - `implementation("com.adamglin.recompose.pulse:runtime:<version>")`

2. Gradle 插件接线链路
   - Gradle 插件向消费者注入新的 runtime 与 annotations 坐标
   - Gradle 插件向 Kotlin 编译流程声明新的 compiler plugin id

3. 发布链路
   - 本地发布与 Maven Central 发布产物全部使用新 group/artifact 组合

4. 测试与样例链路
   - 内嵌测试项目与 sample-desktop 需要用新命名验证端到端接线

## 错误处理与迁移策略

由于这是明确的破坏性调整，不提供自动迁移代码。错误处理策略是“失败即暴露”：

- 旧 plugin id 应直接无法解析
- 旧 Maven 坐标应直接无法解析
- 仓库内部若仍有旧命名引用，应通过编译失败、测试失败或依赖解析失败暴露

这样可以确保没有遗漏的旧命名继续潜伏在发布物中。

## 测试与验证

至少执行以下验证：

1. 单模块与源码级验证
   - `:annotations` 测试
   - `:runtime` 测试
   - `:compiler` 测试
   - `:gradle` 测试

2. 构建接线验证
   - sample-desktop 能以新 plugin id 成功配置
   - compiler plugin 仍能被 Gradle 插件正确接入

3. 发布验证
   - `publishPulseToMavenLocal` 成功
   - 本地 Maven 仓库产物路径变为 `com/adamglin/recompose/pulse/...`
   - 消费者样例能用 `com.adamglin.recompose.pulse:runtime` 等新坐标成功解析

## 实施顺序

1. 调整模块目录名与 `settings.gradle.kts`
2. 调整根构建脚本中的 project path、发布映射、依赖替换
3. 调整各模块 `build.gradle.kts` 的 namespace、artifact 配置与相关引用
4. 调整 Gradle plugin id 与 compiler plugin id
5. 调整源码包路径、import、测试 fixture 与 sample
6. 调整 README 与发布说明
7. 执行测试、发布到本地仓库并验证消费者解析

## 风险

- 模块目录与 project path 一起调整后，容易遗漏测试中的硬编码路径
- plugin id 与 compiler plugin id 同时变更，Gradle 功能测试容易出现级联失败
- 发布坐标变更后，本地消费者和依赖替换规则必须同步，否则会出现“工程内能编译，发布后不能消费”的分叉

## 结果定义

当以下条件全部满足时，本次调整完成：

- 仓库内不再引用旧命名规则
- 所有模块与测试通过
- 本地发布产物使用 `com.adamglin.recompose.pulse` group 和短 artifactId
- sample 与消费者验证使用新 plugin id、新坐标可以正常工作
