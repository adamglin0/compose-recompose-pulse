import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    `maven-publish`
}

group = providers.gradleProperty("GROUP").orElse("com.adamglin").get()
version = providers.gradleProperty("VERSION_NAME").orElse("0.1.1").get()

if (rootProject.name == "recompose-pulse-plugin-build") {
    layout.buildDirectory.set(layout.projectDirectory.dir("build-included"))
}

val generatePulseMetadataDir = layout.buildDirectory.dir(
    "generated/sources/pulseMetadata/kotlin",
)

val generatePulseMetadata by tasks.registering {
    val outputDir = generatePulseMetadataDir

    outputs.dir(outputDir)

    doLast {
        val file = outputDir.get().file("com/adamglin/recompose/pulse/gradle/PulseGradleMetadata.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package com.adamglin.recompose.pulse.gradle

            internal object PulseGradleMetadata {
                const val group = "${project.group}"
                const val version = "${project.version}"
                const val compilerArtifact = "${project.group}:recompose-pulse-compiler:${project.version}"
                const val runtimeArtifact = "${project.group}:recompose-pulse-runtime:${project.version}"
                const val annotationsArtifact = "${project.group}:recompose-pulse-annotations:${project.version}"
            }
            """.trimIndent(),
        )
    }
}

kotlin {
    jvmToolchain(17)
}

sourceSets.main {
    kotlin.srcDir(generatePulseMetadataDir)
}

dependencies {
    implementation(gradleApi())
    implementation(libs.kotlin.gradle.plugin.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.truth)
    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testImplementation(gradleTestKit())
}

tasks.named("compileKotlin") {
    dependsOn(generatePulseMetadata)
}

gradlePlugin {
    plugins {
        create("recomposePulse") {
            id = "com.adamglin.recompose-pulse"
            implementationClass = "com.adamglin.recompose.pulse.gradle.PulseGradlePlugin"
        }
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
