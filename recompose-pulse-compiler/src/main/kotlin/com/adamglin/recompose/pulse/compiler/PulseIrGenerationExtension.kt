package com.adamglin.recompose.pulse.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

internal class PulseIrGenerationExtension(
    private val options: PulseCompilerOptions,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val symbols = PulseSymbols(pluginContext)
        moduleFragment.transform(PulseIrTransformer(pluginContext, options, symbols), null)
    }
}
