package com.adamglin.recompose.pulse.compiler

import org.jetbrains.kotlin.config.CompilerConfigurationKey

object PulseConfigurationKeys {
    val enabled = CompilerConfigurationKey<Boolean>("Whether recompose pulse is enabled")
    val includePackages = CompilerConfigurationKey<List<String>>("Included packages")
    val excludePackages = CompilerConfigurationKey<List<String>>("Excluded packages")
}
