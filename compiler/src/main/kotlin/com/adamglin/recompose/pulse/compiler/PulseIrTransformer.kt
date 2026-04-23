package com.adamglin.recompose.pulse.compiler

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrContainerExpression
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.fileEntry
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.parentClassOrNull

internal class PulseIrTransformer(
    private val pluginContext: IrPluginContext,
    private val options: PulseCompilerOptions,
    private val symbols: PulseSymbols,
) : IrElementTransformerVoidWithContext() {
    override fun visitCall(expression: IrCall): IrExpression {
        val call = super.visitCall(expression) as IrCall
        val currentFile = currentFile ?: return call
        val currentFunction = currentFunction?.irElement as? IrFunction ?: return call
        val callee = call.symbol.owner

        if (!callee.hasAnnotation(PulseSymbols.composableFqName)) return call
        if (!options.shouldTransform(currentFile.packageFqName.asString())) return call
        if (callee.hasAnnotation(symbols.noRecomposePulse)) return call
        if (callee.parentClassOrNull?.hasAnnotation(symbols.noRecomposePulse) == true) return call

        val modifierParameter = callee.parameters.firstOrNull { parameter ->
            parameter.kind == IrParameterKind.Regular &&
            parameter.name.asString() == "modifier" && parameter.type.classOrNull == symbols.modifierClass
        } ?: return call

        val tag = buildTag(currentFile, call, currentFunction.name.asString())
        val pulseModifier = buildPulseModifierCall(call, tag)
        val existingModifier = call.arguments[modifierParameter]

        val injectedModifier = if (existingModifier != null && !existingModifier.isNullPlaceholder()) {
            buildModifierThenCall(call, existingModifier, pulseModifier)
        } else {
            pulseModifier
        }

        call.arguments[modifierParameter] = injectedModifier
        return call
    }

    private fun buildPulseModifierCall(call: IrCall, tag: String): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol, call.startOffset, call.endOffset)
        return builder.irCall(symbols.recomposePulseModifier).apply {
            arguments[symbols.recomposePulseModifier.owner.parameters.single { it.kind == IrParameterKind.Regular }] = builder.irString(tag)
        }
    }

    private fun buildModifierThenCall(
        call: IrCall,
        existingModifier: IrExpression,
        pulseModifier: IrExpression,
    ): IrExpression {
        val builder = DeclarationIrBuilder(pluginContext, currentScope!!.scope.scopeOwnerSymbol, call.startOffset, call.endOffset)
        return builder.irCall(symbols.modifierThen).apply {
            dispatchReceiver = existingModifier
            arguments[symbols.modifierThen.owner.parameters.single { it.kind == IrParameterKind.Regular }] = pulseModifier
        }
    }

    private fun buildTag(currentFile: IrFile, call: IrCall, functionName: String): String {
        val lineNumber = currentFile.fileEntry.getLineNumber(call.startOffset) + 1
        val fileName = currentFile.fileEntry.name.substringAfterLast('/')
        return "$fileName:$lineNumber:$functionName"
    }

    private fun IrExpression.isNullPlaceholder(): Boolean {
        return when (this) {
            is IrConst -> value == null
            is IrTypeOperatorCall -> argument.isNullPlaceholder()
            is IrContainerExpression -> (statements.lastOrNull() as? IrExpression)?.isNullPlaceholder() == true
            else -> false
        }
    }
}
