# Compose Recompose Pulse

Current version: `0.1.1`

## Introduce

Compose Recompose Pulse is a local-installable Compose instrumentation toolkit that highlights recomposition with a lightweight visual pulse.

The current setup supports these Kotlin Multiplatform targets:

- `android`
- `iosArm64`
- `iosSimulatorArm64`
- `js`
- `wasmJs`
- `jvm`

The public package base is `com.adamglin.recompose.pulse`.
The Gradle plugin id is `com.adamglin.recompose-pulse`.

## Install

First, make sure your consumer project can resolve artifacts from `mavenLocal()`.

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

Then apply the plugin:

```kotlin
plugins {
    id("com.adamglin.recompose-pulse") version "0.1.1"
}
```

If you only want the runtime APIs, you can depend on the libraries directly:

```kotlin
dependencies {
    implementation("com.adamglin:recompose-pulse-runtime:0.1.1")
    implementation("com.adamglin:recompose-pulse-annotations:0.1.1")
}
```

## Usage

Enable the plugin and scope instrumentation to the packages you want:

```kotlin
recomposePulse {
    enabled.set(true)
    includePackages.add("com.example.app")
}
```

If `includePackages` is omitted, instrumentation is not restricted to a specific package list.

Wrap your UI with `ProvideRecomposePulse`:

```kotlin
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.adamglin.recompose.pulse.ProvideRecomposePulse
import com.adamglin.recompose.pulse.RecomposePulseStyle

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

Skip instrumentation for a specific composable or class:

```kotlin
import com.adamglin.recompose.pulse.NoRecomposePulse

@NoRecomposePulse
@Composable
fun ExpensiveComposable() {
}
```

Disable the pulse for a subtree:

```kotlin
import androidx.compose.runtime.Composable
import com.adamglin.recompose.pulse.DisableRecomposePulse

@Composable
fun Screen() {
    DisableRecomposePulse {
    }
}
```

The desktop sample in `sample-desktop` shows a simple counter screen that demonstrates both enabled and disabled subtrees.

## Build Local

Publish all artifacts to your local Maven repository:

```bash
./gradlew --no-daemon publishPulseToMavenLocal
```

This publishes the main coordinates:

- `com.adamglin:recompose-pulse-annotations:0.1.1`
- `com.adamglin:recompose-pulse-runtime:0.1.1`
- `com.adamglin:recompose-pulse-compiler:0.1.1`
- `com.adamglin:recompose-pulse-gradle:0.1.1`
- plugin marker: `com.adamglin.recompose-pulse:com.adamglin.recompose-pulse.gradle.plugin:0.1.1`

If you republish the same version, refresh dependencies in the consumer project:

```bash
./gradlew --refresh-dependencies compileKotlin
```

## Release

Push a tag prefixed with `v` to trigger the GitHub release workflow:

```bash
git tag v1.2.3
git push origin v1.2.3
```

The workflow strips the leading `v` and publishes the remaining value as the Maven Central version. For example, `v1.2.3` publishes `1.2.3`, and `v1.2.3-beta01` publishes `1.2.3-beta01`.

Configure these GitHub Actions repository secrets before releasing:

- `GPG_KEY_CONTENTS`
- `MAVEN_CENTRAL_PASSWORD`
- `MAVEN_CENTRAL_USERNAME`
- `SIGNING_KEY_ID`
- `SIGNING_PASSWORD`

## Design Approach

The project is split into four focused pieces:

- `annotations`: opt-out markers such as `@NoRecomposePulse`
- `runtime`: Compose APIs and modifier implementation that render the pulse effect
- `compiler`: injects the pulse modifier into eligible composable calls
- `gradle`: connects the runtime and compiler plugin to consumer projects

The design goal is to keep the consumer API small while moving the heavy lifting into compile-time instrumentation.
In practice, the Gradle plugin adds the runtime and annotations dependencies, the compiler plugin rewrites matching composable call sites, and the runtime renders a short-lived overlay pulse at recomposition time.

This keeps application code simple, makes opt-in usage explicit, and allows selective opt-out for performance-sensitive or noisy UI regions.

This project is licensed under Apache-2.0. See `LICENSE` and `NOTICE` for the attribution requirement.
