package com.adamglin.recompose.pulse.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import java.io.File
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode

@OptIn(ExperimentalCompilerApi::class)
internal class CompilerPluginCompilation(
    private val source: SourceFile,
) {
    fun compile(): CompilationResult {
        val compilation = KotlinCompilation().apply {
            inheritClassPath = true
            messageOutputStream = System.out
            sources = listOf(
                composableStub,
                modifierStub,
                pulseRuntimeStub,
                noRecomposePulseStub,
                source,
            )
            commandLineProcessors = listOf(PulseCommandLineProcessor())
            compilerPluginRegistrars = listOf(PulseCompilerPluginRegistrar())
            pluginOptions = listOf(
                PluginOption(PulseCommandLineProcessor.PLUGIN_ID, "enabled", "true"),
                PluginOption(PulseCommandLineProcessor.PLUGIN_ID, "includePackage", "sample"),
            )
        }

        val result = compilation.compile()
        return CompilationResult(result.exitCode, result.messages, compilation.classesDir)
    }

    internal data class CompilationResult(
        val exitCode: KotlinCompilation.ExitCode,
        val messages: String,
        private val classesDir: File,
    ) {
        fun assertSuccess() {
            assertThat(exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
        }

        fun methodContainsPulseModifierCall(className: String, methodName: String): Boolean {
            return methodContainsMethodCall(
                className = className,
                methodName = methodName,
                invokedMethodName = "recomposePulseModifier",
            )
        }

        fun methodContainsModifierThenCall(className: String, methodName: String): Boolean {
            return methodContainsMethodCall(
                className = className,
                methodName = methodName,
                invokedMethodName = "then",
            )
        }

        fun methodContainsNullThenPattern(className: String, methodName: String): Boolean {
            val method = readMethodNode(className, methodName) ?: return false
            return method.instructions
                .asSequence()
                .filterIsInstance<MethodInsnNode>()
                .any { instruction ->
                    instruction.name == "then" &&
                        instruction.previousMeaningfulInstructions(limit = 6)
                            .any { it.opcode == Opcodes.ACONST_NULL }
                }
        }

        private fun methodContainsMethodCall(
            className: String,
            methodName: String,
            invokedMethodName: String,
        ): Boolean {
            return readMethodNode(className, methodName)
                ?.instructions
                ?.asSequence()
                ?.filterIsInstance<MethodInsnNode>()
                ?.any { it.name == invokedMethodName } == true
        }

        private fun readMethodNode(className: String, methodName: String) = readClassNode(className)
            .methods
            .firstOrNull { it.name == methodName }

        private fun readClassNode(className: String): ClassNode {
            val classFile = File(classesDir, className.replace('.', '/') + ".class")
            val classNode = ClassNode()
            ClassReader(classFile.readBytes()).accept(classNode, 0)
            return classNode
        }

        private fun AbstractInsnNode.previousMeaningfulInstruction(): AbstractInsnNode? {
            var current = previous
            while (current != null && current.opcode < 0) {
                current = current.previous
            }
            return current
        }

        private fun AbstractInsnNode.previousMeaningfulInstructions(limit: Int): Sequence<AbstractInsnNode> = sequence {
            var current = previousMeaningfulInstruction()
            var remaining = limit
            while (current != null && remaining > 0) {
                yield(current)
                current = current.previousMeaningfulInstruction()
                remaining--
            }
        }
    }

    private companion object {
        val composableStub = SourceFile.kotlin(
            "Composable.kt",
            """
            package androidx.compose.runtime

            @Target(
                AnnotationTarget.FUNCTION,
                AnnotationTarget.TYPE,
                AnnotationTarget.TYPE_PARAMETER,
            )
            annotation class Composable
            """.trimIndent(),
        )

        val modifierStub = SourceFile.kotlin(
            "Modifier.kt",
            """
            package androidx.compose.ui

            interface Modifier {
                infix fun then(other: Modifier): Modifier = CombinedModifier(this, other)

                companion object : Modifier
            }

            data class CombinedModifier(
                val outer: Modifier,
                val inner: Modifier,
            ) : Modifier
            """.trimIndent(),
        )

        val pulseRuntimeStub = SourceFile.kotlin(
            "RecomposePulseKt.kt",
            """
            package com.adamglin.recompose.pulse

            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier

            @Composable
            fun recomposePulseModifier(tag: String? = null): Modifier = Modifier
            """.trimIndent(),
        )

        val noRecomposePulseStub = SourceFile.kotlin(
            "NoRecomposePulse.kt",
            """
            package com.adamglin.recompose.pulse

            @Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.BINARY)
            annotation class NoRecomposePulse
            """.trimIndent(),
        )
    }
}
