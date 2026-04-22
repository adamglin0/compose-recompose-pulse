package com.adamglin.recompose.pulse.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Recompose Pulse Desktop Sample",
    ) {
        App()
    }
}
