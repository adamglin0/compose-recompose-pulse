package com.adamglin.recompose.pulse

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class NoRecomposePulseAnnotationTest {
    @Test
    fun `annotation targets function and class`() {
        val target = NoRecomposePulse::class.java.getAnnotation(Target::class.java)
        assertNotNull(target)
        assertEquals(
            setOf(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS),
            target.allowedTargets.toSet(),
        )
    }
}
