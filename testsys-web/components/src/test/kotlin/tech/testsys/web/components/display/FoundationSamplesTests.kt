package tech.testsys.web.components.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestPage
import tech.testsys.web.components.find

class FoundationSamplesTests : MockVaadinTests() {
    @ParameterizedTest
    @EnumSource(FoundationCategory::class)
    fun `should illustrate the canonical catalog with core markup`(category: FoundationCategory) {
        val root = buildTestPage { block { foundationSamples(category, sampleText = "Example") } }.find("ts-foundation-samples")

        assertTrue(root.children.count() > 0)
        val first = root.children.findFirst().orElseThrow().children.toList()
        val token = first[0].element.text
        val example = first[1].element
        assertTrue(example.getAttribute("style").contains("var($token)"))

    }
    @Test
    fun `should preserve provided typography sample content`() {
        val root = buildTestPage { block { foundationSamples(FoundationCategory.Typography, sampleText = "Example") } }.find("ts-foundation-samples")

        assertEquals("Example", root.children.findFirst().orElseThrow().children.toList()[1].element.text)
    }
    @Test
    fun `should configure visibility of the foundation representation through its handle`() {
        lateinit var handle: ElementHandle
        val root = buildTestPage { block { handle = foundationSamples(FoundationCategory.Palette) { isVisible = false } } }

        assertFalse(root.find("ts-foundation-samples").isVisible)
        handle.isVisible = true
        assertTrue(root.find("ts-foundation-samples").isVisible)
    }
}
