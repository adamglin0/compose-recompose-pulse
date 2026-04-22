plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinCompose) apply false
}

tasks.register("publishPulseToMavenLocal") {
    dependsOn(
        ":recompose-pulse-annotations:publishToMavenLocal",
        ":recompose-pulse-runtime:publishToMavenLocal",
        ":recompose-pulse-compiler:publishToMavenLocal",
        ":recompose-pulse-gradle:publishToMavenLocal",
    )
}

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION_NAME").get()

    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("${project.group}:recompose-pulse-annotations"))
                .using(project(":recompose-pulse-annotations"))
            substitute(module("${project.group}:recompose-pulse-compiler"))
                .using(project(":recompose-pulse-compiler"))
            substitute(module("${project.group}:recompose-pulse-runtime"))
                .using(project(":recompose-pulse-runtime"))
        }
    }
}
