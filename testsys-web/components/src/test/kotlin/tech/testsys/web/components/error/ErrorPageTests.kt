package tech.testsys.web.components.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.find
import tech.testsys.web.components.testTexts

class ErrorPageTests : MockVaadinTests() {
    @Test
    fun `should replace heading, description and page title when shown`() {
        val page = NotFoundPage()

        page.show(title = "Нет доступа", description = "Нет прав", pageTitle = "Нет доступа — TestSys")

        assertEquals("Нет доступа", page.find("ts-h1").element.text)
        assertEquals("Нет прав", page.find("ts-empty__desc").element.text)
        assertEquals("Нет доступа — TestSys", page.pageTitle)
    }

    @Test
    fun `should show the missing page texts on the not found page`() {
        val page = NotFoundPage()

        assertEquals(testTexts.messages.uiFeedback.errorNotFoundTitle(), page.find("ts-h1").element.text)
        assertEquals(testTexts.messages.uiFeedback.errorNotFoundDescription(), page.find("ts-empty__desc").element.text)
        assertEquals(testTexts.messages.uiFeedback.errorNotFoundPageTitle(), page.pageTitle)
    }
}
