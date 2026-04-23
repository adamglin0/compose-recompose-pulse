pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "compose-recompose-pulse"

includeBuild("gradle") {
    name = "recompose-pulse-plugin-build"

    dependencySubstitution {
        substitute(module("com.adamglin.recompose.pulse:gradle"))
            .using(project(":"))
    }
}

include(
    ":annotations",
    ":runtime",
    ":compiler",
    ":gradle",
    ":sample-desktop",
)
