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

includeBuild("recompose-pulse-gradle") {
    name = "recompose-pulse-plugin-build"

    dependencySubstitution {
        substitute(module("com.adamglin:recompose-pulse-gradle"))
            .using(project(":"))
    }
}

include(
    ":recompose-pulse-annotations",
    ":recompose-pulse-runtime",
    ":recompose-pulse-compiler",
    ":recompose-pulse-gradle",
    ":sample-desktop",
)
