import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

data class PublishedModule(
    val artifactId: String,
    val pomName: String,
    val pomDescription: String,
)

val publishedModules = mapOf(
    ":recompose-pulse-annotations" to PublishedModule(
        artifactId = "recompose-pulse-annotations",
        pomName = "Compose Recompose Pulse Annotations",
        pomDescription = "Annotations for opting out of Compose Recompose Pulse instrumentation.",
    ),
    ":recompose-pulse-runtime" to PublishedModule(
        artifactId = "recompose-pulse-runtime",
        pomName = "Compose Recompose Pulse Runtime",
        pomDescription = "Runtime APIs and pulse overlay rendering for Compose Recompose Pulse.",
    ),
    ":recompose-pulse-compiler" to PublishedModule(
        artifactId = "recompose-pulse-compiler",
        pomName = "Compose Recompose Pulse Compiler",
        pomDescription = "Compiler plugin that injects Compose Recompose Pulse instrumentation.",
    ),
    ":recompose-pulse-gradle" to PublishedModule(
        artifactId = "recompose-pulse-gradle",
        pomName = "Compose Recompose Pulse Gradle Plugin",
        pomDescription = "Gradle plugin that wires Compose Recompose Pulse dependencies and compiler integration.",
    ),
)

plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.kotlinCompose) apply false
    alias(libs.plugins.mavenPublish) apply false
}

tasks.register("publishPulseToMavenLocal") {
    dependsOn(publishedModules.keys.map { "$it:publishToMavenLocal" })
}

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = rootProject.providers.gradleProperty("releaseVersion")
        .orElse(rootProject.providers.gradleProperty("VERSION_NAME"))
        .get()

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

subprojects {
    val publishedModule = publishedModules[path] ?: return@subprojects
    val isMavenLocalPublish = gradle.startParameter.taskNames.any { taskName ->
        "MavenLocal" in taskName
    }

    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure(MavenPublishBaseExtension::class.java) {
            publishToMavenCentral(automaticRelease = true)
            if (!isMavenLocalPublish) {
                signAllPublications()
            }
            pom {
                name.set(publishedModule.pomName)
                description.set(publishedModule.pomDescription)
                inceptionYear.set(rootProject.providers.gradleProperty("POM_INCEPTION_YEAR"))
                url.set(rootProject.providers.gradleProperty("POM_URL"))
                licenses {
                    license {
                        name.set(rootProject.providers.gradleProperty("POM_LICENSE_NAME"))
                        url.set(rootProject.providers.gradleProperty("POM_LICENSE_URL"))
                        distribution.set(rootProject.providers.gradleProperty("POM_LICENSE_DIST"))
                    }
                }
                developers {
                    developer {
                        id.set(rootProject.providers.gradleProperty("POM_DEVELOPER_ID"))
                        name.set(rootProject.providers.gradleProperty("POM_DEVELOPER_NAME"))
                        url.set(rootProject.providers.gradleProperty("POM_DEVELOPER_URL"))
                    }
                }
                scm {
                    url.set(rootProject.providers.gradleProperty("POM_SCM_URL"))
                    connection.set(rootProject.providers.gradleProperty("POM_SCM_CONNECTION"))
                    developerConnection.set(rootProject.providers.gradleProperty("POM_SCM_DEV_CONNECTION"))
                }
            }
        }

        extensions.configure(PublishingExtension::class.java) {
            publications.withType(MavenPublication::class.java).configureEach {
                version = project.version.toString()
            }
        }
    }
}
