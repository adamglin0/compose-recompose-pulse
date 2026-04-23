package com.adamglin.recompose.pulse.gradle

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PulseCompilerApplicabilityTest {
    @Test
    fun enabledAppliesCompilerPluginToIosMainEvenWhenDebugOnlyIsTrue() {
        assertTrue(
            PulseCompilerApplicability.shouldApply(
                enabled = true,
                debugOnly = true,
                targetName = "iosArm64",
                compilationName = "main",
            ),
        )
    }

    @Test
    fun enabledAppliesCompilerPluginToJsMainEvenWhenDebugOnlyIsTrue() {
        assertTrue(
            PulseCompilerApplicability.shouldApply(
                enabled = true,
                debugOnly = true,
                targetName = "js",
                compilationName = "main",
            ),
        )
    }

    @Test
    fun disabledSkipsCompilerPluginForEveryCompilation() {
        assertFalse(
            PulseCompilerApplicability.shouldApply(
                enabled = false,
                debugOnly = false,
                targetName = "jvm",
                compilationName = "main",
            ),
        )
    }
}
