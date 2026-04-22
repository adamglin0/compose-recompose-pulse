package com.adamglin.recompose.pulse.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal class PulseSymbols(pluginContext: IrPluginContext) {
    val noRecomposePulse: IrClassSymbol = pluginContext.referenceClass(ClassId.topLevel(NO_RECOMPOSE_PULSE_FQ_NAME))
        ?: error("Missing ${NO_RECOMPOSE_PULSE_FQ_NAME.asString()} symbol")

    val modifierClass: IrClassSymbol = pluginContext.referenceClass(ClassId.topLevel(MODIFIER_FQ_NAME))
        ?: error("Missing ${MODIFIER_FQ_NAME.asString()} symbol")

    val modifierThen: IrSimpleFunctionSymbol = modifierClass.owner.declarations
        .filterIsInstance<IrSimpleFunction>()
        .singleOrNull { function ->
            function.name.asString() == "then" &&
                function.parameters.count { it.kind == IrParameterKind.Regular } == 1
        }
        ?.symbol
        ?: error("Missing androidx.compose.ui.Modifier.then symbol")

    val recomposePulseModifier: IrSimpleFunctionSymbol = pluginContext
        .referenceFunctions(CallableId(RECOMPOSE_PULSE_PACKAGE_FQ_NAME, Name.identifier("recomposePulseModifier")))
        .singleOrNull { function ->
            function.owner.parameters.count { it.kind == IrParameterKind.Regular } == 1
        }
        ?: error("Missing ${RECOMPOSE_PULSE_MODIFIER_FQ_NAME.asString()} symbol")

    companion object {
        val composableFqName = FqName("androidx.compose.runtime.Composable")

        private val MODIFIER_FQ_NAME = FqName("androidx.compose.ui.Modifier")
        private val NO_RECOMPOSE_PULSE_FQ_NAME = FqName("com.adamglin.recompose.pulse.NoRecomposePulse")
        private val RECOMPOSE_PULSE_PACKAGE_FQ_NAME = FqName("com.adamglin.recompose.pulse")
        private val RECOMPOSE_PULSE_MODIFIER_FQ_NAME = FqName("com.adamglin.recompose.pulse.recomposePulseModifier")
    }
}
