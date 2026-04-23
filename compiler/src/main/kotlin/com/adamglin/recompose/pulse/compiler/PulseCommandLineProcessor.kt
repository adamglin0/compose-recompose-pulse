package com.adamglin.recompose.pulse.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class PulseCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID

    override val pluginOptions: Collection<AbstractCliOption> = listOf(
        CliOption(
            optionName = OPTION_ENABLED,
            valueDescription = "true|false",
            description = "Enable recompose pulse compiler plugin",
            required = false,
            allowMultipleOccurrences = false,
        ),
        CliOption(
            optionName = OPTION_INCLUDE_PACKAGE,
            valueDescription = "package.fq.name",
            description = "Include package for instrumentation",
            required = false,
            allowMultipleOccurrences = true,
        ),
        CliOption(
            optionName = OPTION_EXCLUDE_PACKAGE,
            valueDescription = "package.fq.name",
            description = "Exclude package from instrumentation",
            required = false,
            allowMultipleOccurrences = true,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            OPTION_ENABLED -> configuration.put(
                PulseConfigurationKeys.enabled,
                value.toBooleanStrictOrNull() ?: error("Invalid value for $OPTION_ENABLED: $value"),
            )

            OPTION_INCLUDE_PACKAGE -> configuration.appendList(PulseConfigurationKeys.includePackages, value)
            OPTION_EXCLUDE_PACKAGE -> configuration.appendList(PulseConfigurationKeys.excludePackages, value)
            else -> error("Unexpected plugin option: ${option.optionName}")
        }
    }

    companion object {
        const val PLUGIN_ID = "com.adamglin.recompose.pulse.compiler"
        const val OPTION_ENABLED = "enabled"
        const val OPTION_INCLUDE_PACKAGE = "includePackage"
        const val OPTION_EXCLUDE_PACKAGE = "excludePackage"
    }
}
