package tech.testsys.web.components

import org.junit.jupiter.api.Assertions.assertEquals
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

    private fun resource(path: String) = javaClass.getResource("/META-INF/resources/$path")
}
