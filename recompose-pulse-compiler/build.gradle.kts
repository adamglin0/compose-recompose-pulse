import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.kotlinJvm)
    `maven-publish`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":recompose-pulse-annotations"))
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }

    repositories {
        mavenLocal()
    }
}
