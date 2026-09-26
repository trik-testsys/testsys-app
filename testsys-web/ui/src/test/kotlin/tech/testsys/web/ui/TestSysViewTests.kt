package tech.testsys.web.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.testsys.web.ui.display.text
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.navigation.CabinetHeader

class TestSysViewTests : MockVaadinTests() {
    private class SampleView(body: PageScope.() -> Unit) : TestSysView(testTexts) {
        init {
            page(CabinetHeader(), body)
        }
    }

    private class NestedPageView : TestSysView(testTexts) {
        init {
            page(CabinetHeader()) { page(CabinetHeader()) {} }
        }
    }

    private class RepeatedPageView : TestSysView(testTexts) {
        init {
            page(CabinetHeader()) {}
            page(CabinetHeader()) {}
        }
    }

    @Test
    fun `should build app root with header and page body`() {
        val root = SampleView { block { row { text("x") } } }.child(0)

        assertTrue("ts-app" in root.classes())
        assertTrue("ts-header" in root.child(0).classes())
        assertEquals("main", root.child(1).element.tag)
        assertTrue("ts-page" in root.child(1).classes())
    }

    @Test
    fun `should reject a page call inside the page body`() {
        val error = assertThrows<IllegalStateException> { NestedPageView() }

        assertTrue("NestedPageView" in error.message.orEmpty())
    }

    @Test
    fun `should reject a repeated page call`() {
        val error = assertThrows<IllegalStateException> { RepeatedPageView() }

        assertTrue("RepeatedPageView" in error.message.orEmpty())
    }
}
