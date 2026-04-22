package com.adamglin.recompose.pulse.compiler

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PulseCompilerOptionsTest {
    @Test
    fun `empty include list allows every package`() {
        val options = PulseCompilerOptions(
            enabled = true,
            includePackages = emptySet(),
            excludePackages = emptySet(),
        )

        assertThat(options.shouldTransform("com.adamglin.feature")).isTrue()
    }

    @Test
    fun `exclude packages win over include packages`() {
        val options = PulseCompilerOptions(
            enabled = true,
            includePackages = setOf("com.adamglin"),
            excludePackages = setOf("com.adamglin.excluded"),
        )

        assertThat(options.shouldTransform("com.adamglin.screen")).isTrue()
        assertThat(options.shouldTransform("com.adamglin.excluded")).isFalse()
        assertThat(options.shouldTransform("com.adamglin.excluded.feature")).isFalse()
    }
}
