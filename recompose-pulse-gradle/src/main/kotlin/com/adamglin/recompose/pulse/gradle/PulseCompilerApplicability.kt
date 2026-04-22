package com.adamglin.recompose.pulse.gradle

internal object PulseCompilerApplicability {
    fun shouldApply(
        enabled: Boolean,
        debugOnly: Boolean,
        targetName: String,
        compilationName: String,
    ): Boolean {
        return enabled
    }
}
