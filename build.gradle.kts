import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

data class PublishedModule(
    val artifactId: String,
    val pomName: String,
    val pomDescription: String,
)

val publishedModules = mapOf(
    ":annotations" to PublishedModule(
        artifactId = "annotations",
        pomName = "Compose Recompose Pulse Annotations",
        pomDescription = "Annotations for opting out of Compose Recompose Pulse instrumentation.",
    ),
    ":runtime" to PublishedModule(
        artifactId = "runtime",
        pomName = "Compose Recompose Pulse Runtime",
        pomDescription = "Runtime APIs and pulse overlay rendering for Compose Recompose Pulse.",
    ),
    ":compiler" to PublishedModule(
        artifactId = "compiler",
        pomName = "Compose Recompose Pulse Compiler",
        pomDescription = "Compiler plugin that injects Compose Recompose Pulse instrumentation.",
    ),
    ":gradle" to PublishedModule(
        artifactId = "gradle",
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
            substitute(module("${project.group}:annotations"))
                .using(project(":annotations"))
            substitute(module("${project.group}:compiler"))
                .using(project(":compiler"))
            substitute(module("${project.group}:runtime"))
                .using(project(":runtime"))
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
