package com.adamglin.recompose.pulse

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

val LocalRecomposePulseConfig = compositionLocalOf { RecomposePulseConfig() }

@Composable
fun ProvideRecomposePulse(
    enabled: Boolean,
    style: RecomposePulseStyle = RecomposePulseStyle(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRecomposePulseConfig provides RecomposePulseConfig(enabled = enabled, style = style),
        content = content,
    )
}

@Composable
fun DisableRecomposePulse(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRecomposePulseConfig provides LocalRecomposePulseConfig.current.copy(enabled = false),
        content = content,
    )
}
