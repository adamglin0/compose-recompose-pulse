import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.mavenPublish)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":annotations"))
    implementation(libs.kotlin.compiler.embeddable)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.truth)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
