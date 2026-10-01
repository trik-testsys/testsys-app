package tech.testsys.web.components

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TextsHolderTests : MockVaadinTests() {
    @Test
    fun `should give the texts of the page built on the current UI`() {
        buildTestPage {}

        assertSame(testTexts, currentTexts())
    }

    @Test
    fun `should reject reading texts before any page is built`() {
        assertThrows<IllegalStateException> { currentTexts() }
    }
}
