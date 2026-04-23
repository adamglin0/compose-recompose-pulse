package com.adamglin.recompose.pulse.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class PulseGradlePlugin : KotlinCompilerPluginSupportPlugin, Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("recomposePulse", PulseGradleExtension::class.java)

        target.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            target.dependencies.add("implementation", PulseGradleMetadata.runtimeArtifact)
            target.dependencies.add("implementation", PulseGradleMetadata.annotationsArtifact)
        }

        target.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
            target.dependencies.add("commonMainImplementation", PulseGradleMetadata.runtimeArtifact)
            target.dependencies.add("commonMainImplementation", PulseGradleMetadata.annotationsArtifact)
        }
    }

    override fun getCompilerPluginId(): String {
        return "com.adamglin.recompose.pulse.compiler"
    }

    override fun getPluginArtifact(): SubpluginArtifact {
        return PulseGradleMetadata.compilerArtifact.toSubpluginArtifact()
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PulseGradleExtension::class.java)
        return PulseCompilerApplicability.shouldApply(
            enabled = extension.enabled.get(),
            debugOnly = extension.debugOnly.get(),
            targetName = kotlinCompilation.target.targetName,
            compilationName = kotlinCompilation.compilationName,
        )
    }

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(PulseGradleExtension::class.java)
        return project.provider {
            buildList {
                add(SubpluginOption("enabled", extension.enabled.get().toString()))
                extension.includePackages.get().forEach { add(SubpluginOption("includePackage", it)) }
                extension.excludePackages.get().forEach { add(SubpluginOption("excludePackage", it)) }
            }
        }
    }

    private fun String.toSubpluginArtifact(): SubpluginArtifact {
        val coordinates = split(":")
        require(coordinates.size == 3) { "Invalid artifact coordinates: $this" }
        return SubpluginArtifact(
            groupId = coordinates[0],
            artifactId = coordinates[1],
            version = coordinates[2],
        )
    }
}
