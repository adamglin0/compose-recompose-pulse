package com.adamglin.recompose.pulse

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun recomposePulseModifier(tag: String? = null): Modifier {
    return Modifier.composed {
        val config = LocalRecomposePulseConfig.current
        recomposePulseModifier(enabled = config.enabled, style = config.style, tag = tag)
    }
}

private fun recomposePulseModifier(
    enabled: Boolean,
    style: RecomposePulseStyle,
    tag: String? = null,
): Modifier {
    return if (enabled) Modifier.recomposePulse(style = style, tag = tag) else Modifier
}

fun Modifier.recomposePulse(
    style: RecomposePulseStyle = RecomposePulseStyle(),
    tag: String? = null,
): Modifier {
    return this.then(RecomposePulseElement(style = style, tag = tag))
}

private class RecomposePulseElement(
    private val style: RecomposePulseStyle,
    private val tag: String?,
) : ModifierNodeElement<RecomposePulseNode>() {
    override fun create(): RecomposePulseNode = RecomposePulseNode(style = style, tag = tag)

    override fun update(node: RecomposePulseNode) {
        node.onModifierUpdated(style = style, tag = tag)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "recomposePulse"
        properties["style"] = style
        properties["tag"] = tag
    }

    override fun equals(other: Any?): Boolean = false

    override fun hashCode(): Int {
        var result = style.hashCode()
        result = 31 * result + (tag?.hashCode() ?: 0)
        return result
    }
}

private class RecomposePulseNode(
    style: RecomposePulseStyle,
    tag: String?,
) : Modifier.Node(), DrawModifierNode {
    private var _style = style
    private var _tag = tag
    private var _initialized = false
    private var _currentAlpha = 0f
    private var _pulseJob: Job? = null

    override fun onDetach() {
        _pulseJob?.cancel()
        _pulseJob = null
        _currentAlpha = 0f
    }

    fun onModifierUpdated(style: RecomposePulseStyle, tag: String?) {
        _style = style
        _tag = tag
        if (!_initialized) {
            _initialized = true
            return
        }
        startPulse()
    }

    override fun ContentDrawScope.draw() {
        drawContent()

        val alpha = _currentAlpha
        if (alpha <= 0f) return

        when (_style.mode) {
            RecomposePulseStyle.Mode.Fill -> {
                drawRect(color = _style.color.copy(alpha = alpha))
            }

            RecomposePulseStyle.Mode.Border -> {
                drawRect(
                    color = _style.color.copy(alpha = alpha),
                    topLeft = Offset.Zero,
                    size = size,
                    style = Stroke(width = _style.borderWidth.toPx()),
                )
            }
        }
    }

    private fun startPulse() {
        _pulseJob?.cancel()

        if (!isAttached || _style.maxAlpha <= 0f || _style.durationMillis <= 0) {
            _currentAlpha = 0f
            invalidateDraw()
            return
        }

        _pulseJob = coroutineScope.launch {
            val durationNanos = _style.durationMillis * 1_000_000L
            val startedAt = withFrameNanos { it }

            while (true) {
                val frameTimeNanos = withFrameNanos { it }
                val progress = ((frameTimeNanos - startedAt).toDouble() / durationNanos.toDouble())
                    .coerceIn(0.0, 1.0)
                    .toFloat()

                _currentAlpha = _style.maxAlpha * (1f - progress)
                invalidateDraw()

                if (progress >= 1f) {
                    _currentAlpha = 0f
                    invalidateDraw()
                    break
                }
            }
        }
    }
}
