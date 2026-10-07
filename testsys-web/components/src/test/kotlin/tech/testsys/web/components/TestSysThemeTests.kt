package tech.testsys.web.components

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestSysThemeTests {
    @Test
    fun `should package design system stylesheet with its tokens`() {
        assertEquals("testsys-ui/styles/styles.css", TestSysTheme.DESIGN_SYSTEM)
        assertNotNull(resource(TestSysTheme.DESIGN_SYSTEM))
        assertTrue(resource("testsys-ui/tokens/components.css")!!.readText().contains(".ts-slot__row"))
    }

    @Test
    fun `should package Vaadin overrides stylesheet`() {
        assertNotNull(resource(TestSysTheme.VAADIN_OVERRIDES))
    }

    @Test
    fun `should use canonical typography tokens for headings`() {
        val components = text("testsys-ui/tokens/components.css")

        assertTrue(rule(components, ".ts-h1").getValue("font").contains("var(--fs-h1)"))
        assertEquals("var(--tracking-tight)", rule(components, ".ts-h1")["letter-spacing"])
        assertTrue(rule(components, ".ts-block__title").getValue("font").contains("var(--fw-bold)"))
        assertTrue(rule(components, ".ts-dialog__title").getValue("font").contains("var(--fw-bold)"))
    }

    @Test
    fun `should use the canonical focus ring`() {
        val components = text("testsys-ui/tokens/components.css")

        assertEquals("0 0 0 3px var(--accent-ring)", rule(components, ".ts-header__search:focus-within")["box-shadow"])
        assertEquals("0 0 0 3px var(--accent-ring)", rule(components, ".ts-sort-handle:focus-visible")["box-shadow"])
    }

    @Test
    fun `should use only defined font size tokens`() {
        val typography = text("testsys-ui/tokens/typography.css")
        val components = text("testsys-ui/tokens/components.css")

        val defined = Regex("(--fs-[a-z0-9-]+)\\s*:").findAll(typography).map { match -> match.groupValues[1] }.toSet()
        val used = Regex("var\\((--fs-[a-z0-9-]+)\\)").findAll(components).map { match -> match.groupValues[1] }.toSet()

        assertTrue(defined.containsAll(used), "Undefined size tokens: ${used - defined}")
    }

    @Test
    fun `should use the canonical motion curve`() {
        val components = text("testsys-ui/tokens/components.css")

        assertFalse(components.contains("var(--ease)"))
        assertEquals(
            "box-shadow var(--dur-fast) var(--ease-out), border-color var(--dur-fast) var(--ease-out)",
            rule(components, ".ts-ccard")["transition"],
        )
    }

    @Test
    fun `should animate the drawer on opening only when motion is allowed`() {
        val overrides = text(TestSysTheme.VAADIN_OVERRIDES)
        val drawer = "vaadin-dialog[theme~=\"ts-drawer\"]::part(overlay)"

        assertTrue(rule(overrides, drawer).getValue("animation").contains("ts-drawer-in"))
        assertEquals("none", rule(overrides, "vaadin-dialog[theme~=\"ts-drawer\"] .ts-dialog")["animation"])
        assertTrue(overrides.substringAfter("@media (prefers-reduced-motion: reduce)", "").contains(drawer))
    }

    @Test
    fun `should dim the page behind dialogs and drawers with the overlay tokens`() {
        val overrides = text(TestSysTheme.VAADIN_OVERRIDES)

        assertEquals("var(--overlay)", rule(overrides, "vaadin-dialog[theme~=\"ts-dialog\"]::part(backdrop)")["background"])
        assertEquals("var(--overlay-drawer)", rule(overrides, "vaadin-dialog[theme~=\"ts-drawer\"]::part(backdrop)")["background"])
    }

    @Test
    fun `should share editor geometry and retain opaque disabled controls`() {
        val components = resource("testsys-ui/tokens/components.css")!!.readText()
        val overrides = resource(TestSysTheme.VAADIN_OVERRIDES)!!.readText()
        val box = rule(components, ".ts-code")
        assertTrue(box.getValue("--ts-code-height").contains("--ts-code-min-lines"))
        assertCodeGeometry(components, ".ts-code__lines")
        assertCodeGeometry(components, ".ts-code__area")
        assertOpaqueDisabled(overrides, "vaadin-custom-field[disabled] .ts-code")
        assertOpaqueDisabled(overrides, "vaadin-radio-group.ts-seg[disabled]::part(group-field)")
        assertOpaqueDisabled(overrides, "vaadin-switch[disabled]::part(switch)")
    }

    @Test
    fun `should retain fixed readable fractions and local table scrolling`() {
        val css = resource("testsys-ui/tokens/components.css")!!.readText()
        val layout = rule(css, ".ts-table.ts-table-grid, .ts-leaderboard table.ts-table-grid")

        assertEquals("fixed", layout["table-layout"])
        assertEquals("var(--ts-table-width)", layout["width"])
        assertEquals("calc(var(--ts-table-used) * 52px)", layout["min-width"])
        assertEquals("auto", rule(css, ".ts-table-scroll")["overflow-x"])
        assertEquals("anywhere", rule(css, ".ts-table-grid th, .ts-table-grid td")["overflow-wrap"])
    }

    @Test
    fun `should keep the natural width of a brand image`() {
        val css = text("testsys-ui/tokens/components.css")

        assertEquals("auto", rule(css, ".ts-brand-image")["width"])
    }

    private fun assertCodeGeometry(css: String, selector: String) {
        val declarations = rule(css, selector)
        assertTrue(declarations.getValue("font").contains("--ts-code-line-height"))
        assertTrue(declarations.getValue("padding").contains("--ts-code-padding-y"))
    }

    private fun assertOpaqueDisabled(css: String, selector: String) {
        val declarations = rule(css, selector)
        assertTrue(declarations["opacity"] == null || declarations["opacity"] == "1")
        assertTrue(declarations.containsKey("background"))
    }

    private fun rule(css: String, selector: String): Map<String, String> =
        requireNotNull(Regex(Regex.escape(selector) + "\\s*\\{([^}]+)}").find(css)).groupValues[1].split(';')
            .mapNotNull { declaration ->
                declaration.split(':', limit = 2).takeIf { parts -> parts.size == 2 }?.let { parts -> parts[0].trim() to parts[1].trim() }
            }.toMap()

    private fun resource(path: String) = javaClass.getResource("/META-INF/resources/$path")

    private fun text(path: String): String = requireNotNull(resource(path)) { "Resource $path is missing" }.readText()
}
