package tech.testsys.web.components.feedback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent

internal class SkeletonTests : MockVaadinTests() {
    @Test
    fun `should expose semantic skeleton shapes in intrinsic horizontal flows`() {
        val placeholders = mutableListOf<ElementHandle>()
        buildTestContent {
            horizontal { SkeletonShape.entries.forEach { shape -> placeholders += skeleton(shape) } }
        }

        assertEquals(SkeletonShape.entries.size, placeholders.size)
        placeholders.forEach { handle ->
            assertTrue(handle.component.element.classList.contains("ts-skeleton"))
        }
    }
}
