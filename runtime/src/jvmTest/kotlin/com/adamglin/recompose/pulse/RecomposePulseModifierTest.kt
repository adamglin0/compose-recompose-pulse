package com.adamglin.recompose.pulse

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import java.lang.reflect.Modifier as JavaModifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.Description
import org.junit.runners.model.Statement

class RecomposePulseModifierTest {
    @Test
    fun `exports plain jvm signature for injected runtime call`() {
        val method = Class
            .forName("com.adamglin.recompose.pulse.RecomposePulseModifierKt")
            .declaredMethods
            .singleOrNull { candidate ->
                candidate.name == "recomposePulseModifier" &&
                    candidate.parameterTypes.contentEquals(arrayOf(String::class.java)) &&
                    JavaModifier.isStatic(candidate.modifiers)
            }

        assertNotNull(method)
    }

    @Test
    fun `manual modifier adds one element`() {
        val modifier = Modifier.recomposePulse()

        assertEquals(1, modifierElementCount(modifier))
    }

    @Test
    fun `provider returns only composed wrapper when disabled`() {
        var modifier: Modifier = Modifier

        runWithComposeRule { composeRule ->
            composeRule.setContent {
                ProvideRecomposePulse(enabled = false) {
                    modifier = recomposePulseModifier()
                }
            }
        }

        assertEquals(1, modifierElementCount(modifier))
    }

    @Test
    fun `nested disable suppresses pulse element injection`() {
        var modifier: Modifier = Modifier

        runWithComposeRule { composeRule ->
            composeRule.setContent {
                ProvideRecomposePulse(enabled = true) {
                    DisableRecomposePulse {
                        modifier = recomposePulseModifier()
                    }
                }
            }
        }

        assertEquals(1, modifierElementCount(modifier))
    }

    private fun modifierElementCount(modifier: Modifier): Int {
        var count = 0
        modifier.foldIn(Unit) { _, _ ->
            count += 1
        }
        return count
    }

    private fun runWithComposeRule(block: (ComposeContentTestRule) -> Unit) {
        val composeRule = createComposeRule()
        val statement = object : Statement() {
            override fun evaluate() {
                block(composeRule)
            }
        }

        composeRule.apply(
            statement,
            Description.createTestDescription(
                RecomposePulseModifierTest::class.java,
                "composeRule",
            ),
        ).evaluate()
    }
}
