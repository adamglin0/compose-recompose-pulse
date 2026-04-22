package com.adamglin.recompose.pulse

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class RecomposePulseStyle(
    val color: Color = Color(0xFFFFD54F),
    val maxAlpha: Float = 0.10f,
    val durationMillis: Int = 140,
    val borderWidth: Dp = 2.dp,
    val mode: Mode = Mode.Fill,
) {
    enum class Mode {
        Fill,
        Border,
    }
}

@Immutable
data class RecomposePulseConfig(
    val enabled: Boolean = false,
    val style: RecomposePulseStyle = RecomposePulseStyle(),
)
