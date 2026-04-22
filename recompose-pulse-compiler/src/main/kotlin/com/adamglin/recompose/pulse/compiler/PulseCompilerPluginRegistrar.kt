package com.adamglin.recompose.pulse.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class PulseCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = PulseCommandLineProcessor.PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = PulseCompilerOptions(
            enabled = configuration.get(PulseConfigurationKeys.enabled) ?: false,
            includePackages = configuration.getList(PulseConfigurationKeys.includePackages).toSet(),
            excludePackages = configuration.getList(PulseConfigurationKeys.excludePackages).toSet(),
        )

        if (!options.enabled) {
            return
        }

        IrGenerationExtension.registerExtension(PulseIrGenerationExtension(options))
    }
}
