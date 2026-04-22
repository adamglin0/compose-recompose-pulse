package com.adamglin.recompose.pulse.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.jupiter.api.Test

class ModifierInjectionCompilerTest {
    @Test
    fun `adds pulse modifier when modifier argument exists`() {
        val result = CompilerPluginCompilation(
            source = SourceFile.kotlin(
                "ExplicitModifier.kt",
                """
                package sample

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier

                @Composable
                fun Target(modifier: Modifier) {}

                @Composable
                fun Wrapper(modifier: Modifier) {
                    Target(modifier = modifier)
                }
                """.trimIndent(),
            ),
        ).compile()

        result.assertSuccess()
        assertThat(result.methodContainsPulseModifierCall("sample.ExplicitModifierKt", "Wrapper")).isTrue()
        assertThat(result.methodContainsModifierThenCall("sample.ExplicitModifierKt", "Wrapper")).isTrue()
    }

    @Test
    fun `replaces lowered null modifier placeholder with pulse modifier`() {
        val result = CompilerPluginCompilation(
            source = SourceFile.kotlin(
                "DefaultModifier.kt",
                """
                package sample

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier

                @Composable
                fun Target(modifier: Modifier? = Modifier) {}

                @Composable
                fun Wrapper() {
                    Target(modifier = null)
                }
                """.trimIndent(),
            ),
        ).compile()

        result.assertSuccess()
        assertThat(result.methodContainsPulseModifierCall("sample.DefaultModifierKt", "Wrapper")).isTrue()
        assertThat(result.methodContainsNullThenPattern("sample.DefaultModifierKt", "Wrapper")).isFalse()
    }

    @Test
    fun `skips annotated composables`() {
        val result = CompilerPluginCompilation(
            source = SourceFile.kotlin(
                "AnnotatedComposable.kt",
                """
                package sample

                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import com.adamglin.recompose.pulse.NoRecomposePulse

                @NoRecomposePulse
                @Composable
                fun Target(modifier: Modifier = Modifier) {}

                @Composable
                fun Wrapper() {
                    Target()
                }
                """.trimIndent(),
            ),
        ).compile()

        result.assertSuccess()
        assertThat(result.methodContainsPulseModifierCall("sample.AnnotatedComposableKt", "Wrapper")).isFalse()
    }
}
