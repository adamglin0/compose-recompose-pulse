package com.adamglin.recompose.pulse

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RecomposePulseStyleTest {
    @Test
    fun `default config starts disabled`() {
        val config = RecomposePulseConfig()

        assertFalse(config.enabled)
        assertEquals(Color(0xFFFFD54F), config.style.color)
        assertEquals(RecomposePulseStyle.Mode.Fill, config.style.mode)
        assertEquals(140, config.style.durationMillis)
        assertEquals(0.10f, config.style.maxAlpha)
        assertEquals(2.dp, config.style.borderWidth)
    }
}
