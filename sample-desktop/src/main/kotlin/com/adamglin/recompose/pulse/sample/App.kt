package com.adamglin.recompose.pulse.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.adamglin.recompose.pulse.DisableRecomposePulse
import com.adamglin.recompose.pulse.ProvideRecomposePulse
import com.adamglin.recompose.pulse.RecomposePulseStyle

@Composable
fun App() {
    var count by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(true) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ProvideRecomposePulse(
                enabled = enabled,
                style = RecomposePulseStyle(maxAlpha = 0.16f, durationMillis = 220),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { count += 1 }) {
                            Text("Increment")
                        }
                        Button(onClick = { enabled = !enabled }) {
                            Text(if (enabled) "Disable Pulse" else "Enable Pulse")
                        }
                    }

                    PulseCard(
                        title = "Pulse-enabled card",
                        description = "This card should pulse lightly whenever count changes.",
                        count = count,
                    )

                    DisableRecomposePulse {
                        PulseCard(
                            title = "Disabled subtree card",
                            description = "This card recomposes too, but pulse is disabled for this subtree.",
                            count = count,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseCard(
    title: String,
    description: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = description, style = MaterialTheme.typography.body2)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Count: $count")
        }
    }
}
