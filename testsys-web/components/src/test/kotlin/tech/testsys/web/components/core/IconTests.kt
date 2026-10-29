package tech.testsys.web.components.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.buildTestRow
import tech.testsys.web.components.child
import tech.testsys.web.components.classes
import tech.testsys.web.components.find

class IconTests : MockVaadinTests() {
    private val sprite = requireNotNull(javaClass.getResource("/META-INF/resources/testsys-ui/icons.svg")).readText()

    @Test
    fun `should render a sprite reference with the standard size and color`() {
        val icon = buildTestContent { icon(IconName.ChevronDown) }.find("ts-icon")

        val svg = icon.element.getProperty("innerHTML")
        assertTrue(svg.contains("""href="testsys-ui/icons.svg#chevron-down""""))
        assertTrue(svg.contains("""width="16""""))
        assertTrue(svg.contains("""stroke="currentColor""""))
    }

    @Test
    fun `should keep the Lucide path of an icon in the sprite`() {
        assertTrue(sprite.contains("""<symbol id="chevron-down" viewBox="0 0 24 24"><path d="m6 9 6 6 6-6"/></symbol>"""))
    }

    @Test
    fun `should span icon over its size in a block row`() {
        val icon = buildTestRow { icon(IconName.Trophy, size = 1) }.child(0)

        assertTrue("ts-icon" in icon.classes())
        assertEquals("span 1", icon.element.style.get("grid-column"))
    }

    @Test
    fun `should keep the icon subset of the design system in the sprite`() {
        assertEquals(37, IconName.entries.size)
        assertEquals(37, Regex("<symbol ").findAll(sprite).count())
    }

    @ParameterizedTest
    @EnumSource(IconName::class)
    internal fun `should define a sprite symbol for every icon name`(name: IconName) {
        assertTrue(sprite.contains("<symbol id=\"${name.key}\""))
    }
}
