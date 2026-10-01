package tech.testsys.web.components.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.find

class IconTests : MockVaadinTests() {
    @Test
    fun `should render Lucide paths with the standard size`() {
        val icon = buildTestContent { icon(IconName.ChevronDown) }.find("ts-icon")

        val svg = icon.element.getProperty("innerHTML")
        assertTrue(svg.contains("""<path d="m6 9 6 6 6-6"/>"""))
        assertTrue(svg.contains("""width="16""""))
        assertTrue(svg.contains("""stroke="currentColor""""))
    }

    @Test
    fun `should span icon over its size in a block row`() {
        val icon = buildTestRow { icon(IconName.Trophy, size = 1) }.child(0)

        assertTrue("ts-icon" in icon.classes())
        assertEquals("span 1", icon.element.style.get("grid-column"))
    }

    @Test
    fun `should cover the icon subset of the design system`() {
        assertEquals(37, IconName.entries.size)
    }
}
