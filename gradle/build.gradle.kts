import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
    `java-gradle-plugin`
}

group = providers.gradleProperty("GROUP").orElse("com.adamglin.recompose.pulse").get()
version = providers.gradleProperty("releaseVersion")
    .orElse(providers.gradleProperty("VERSION_NAME"))
    .orElse("0.1.1")
    .get()

if (rootProject.name == "recompose-pulse-plugin-build") {
    layout.buildDirectory.set(layout.projectDirectory.dir("build-included"))
}

val generatePulseMetadataDir = layout.buildDirectory.dir(
    "generated/sources/pulseMetadata/kotlin",
)

val generatePulseMetadata by tasks.registering {
    val outputDir = generatePulseMetadataDir

    inputs.property("group", project.group.toString())
    inputs.property("version", project.version.toString())
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
                const val compilerArtifact = "${project.group}:compiler:${project.version}"
                const val runtimeArtifact = "${project.group}:runtime:${project.version}"
                const val annotationsArtifact = "${project.group}:annotations:${project.version}"
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

tasks.configureEach {
    if (name == "sourcesJar" || name == "kotlinSourcesJar") {
        dependsOn(generatePulseMetadata)
    }
}

gradlePlugin {
    plugins {
        create("recomposePulse") {
            id = "com.adamglin.recompose.pulse"
            implementationClass = "com.adamglin.recompose.pulse.gradle.PulseGradlePlugin"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
