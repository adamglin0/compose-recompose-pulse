import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

buildscript {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }

    dependencies {
        classpath("com.adamglin:recompose-pulse-gradle:0.1.1")
    }
}

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinCompose)
}

apply(plugin = "com.adamglin.recompose-pulse")

kotlin {
    jvmToolchain(17)
}

val recomposePulseExtension = extensions.getByName("recomposePulse")

(recomposePulseExtension.javaClass.getMethod("getEnabled").invoke(recomposePulseExtension) as Property<Boolean>).set(true)
(recomposePulseExtension.javaClass.getMethod("getIncludePackages").invoke(recomposePulseExtension) as ListProperty<String>)
    .add("com.adamglin.recompose.pulse.sample")

dependencies {
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.adamglin.recompose.pulse.sample.MainKt"
    }
}

tasks.register("compileKotlinJvm") {
    dependsOn(tasks.named("compileKotlin"))
}
