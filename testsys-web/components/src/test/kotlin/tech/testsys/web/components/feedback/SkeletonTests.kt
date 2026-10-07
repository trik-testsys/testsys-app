package tech.testsys.web.components.feedback

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

internal class SkeletonTests : MockVaadinTests() {
    @ParameterizedTest
    @CsvSource(
        "Text,ts-skeleton--text,ts-skel--text",
        "Circle,ts-skeleton--circle,ts-skel--circle",
        "Badge,ts-skeleton--badge,ts-skel--badge",
        "Rectangle,ts-skeleton--rectangle,ts-skel--rectangle",
    )
    fun `should mark a skeleton and its decorative part by the chosen shape`(shape: SkeletonShape, rootClass: String, partClass: String) {
        lateinit var handle: ElementHandle

        buildTestContent { handle = skeleton(shape) }

        val root = handle.component.element
        assertEquals(setOf("ts-skeleton", rootClass), root.classList.toSet())
        assertEquals(setOf("ts-skel", partClass), root.getChild(0).classList.toSet())
    }

    @Test
    fun `should announce a skeleton as one loading status with a hidden part`() {
        lateinit var handle: ElementHandle

        buildTestContent { handle = skeleton() }

        val root = handle.component.element
        assertEquals("status", root.getAttribute("role"))
        assertEquals(testTexts.components.loading, root.getAttribute("aria-label"))
        assertEquals("true", root.getChild(0).getAttribute("aria-hidden"))
    }

    @Test
    fun `should show the requested number of loading rows`() {
        val content = buildTestContent { skeletonRows(rows = 3) }

        assertEquals(3, content.findAll("ts-skel-row").size)
    }

    @Test
    fun `should announce loading rows as one status`() {
        lateinit var handle: ElementHandle

        buildTestContent { handle = skeletonRows(rows = 2) }

        assertEquals("status", handle.component.element.getAttribute("role"))
        assertEquals(testTexts.components.loading, handle.component.element.getAttribute("aria-label"))
    }

    @Test
    fun `should hide the shapes of loading rows from assistive technology`() {
        lateinit var handle: ElementHandle

        buildTestContent { handle = skeletonRows(rows = 2) }

        assertEquals("true", handle.component.element.getChild(0).getAttribute("aria-hidden"))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1])
    fun `should reject a nonpositive number of loading rows`(rows: Int) {
        assertThrows(IllegalArgumentException::class.java) { buildTestContent { skeletonRows(rows = rows) } }
    }
}
