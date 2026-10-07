package tech.testsys.web.components.quiz

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

class QuestionNavTests : MockVaadinTests() {
    @Test
    fun `should show current answered flagged and accessible question states`() {
        val root = buildTestContent { questionNav(QuestionNavData(3, answered = setOf(2), flagged = setOf(3))) }

        val cells = root.findAll("ts-qnav__cell")
        assertEquals("step", cells[0].element.getAttribute("aria-current"))
        assertTrue("ts-qnav__cell--answered" in cells[1].element.classList)
        assertEquals(testTexts.components.questionStatus(3, false, true), cells[2].element.getAttribute("aria-label"))
        assertEquals("true", root.findAll("ts-qnav__flag").single().element.getAttribute("aria-hidden"))
    }

    @Test
    fun `should show new data set through the handle`() {
        lateinit var handle: SelectionHandle<QuestionNavData>
        val root = buildTestContent {
            handle = questionNav(QuestionNavData(3, answered = setOf(2), flagged = setOf(3)))
        }

        handle.data = QuestionNavData(2, current = 2, answered = setOf(2))

        val cells = root.findAll("ts-qnav__cell")
        assertEquals(2, cells.size)
        assertEquals("step", cells[1].element.getAttribute("aria-current"))
        assertFalse("ts-qnav__cell--answered" in cells[1].element.classList)
        assertTrue(root.findAll("ts-qnav__flag").isEmpty())
    }

    @Test
    fun `should group digits of question numbers by the locale`() {
        val root = buildTestContent { questionNav(QuestionNavData(total = 1000, current = 1000)) }

        assertEquals("1 000", root.findAll("ts-qnav__cell").last().element.text)
    }

    @ParameterizedTest
    @CsvSource("0,1", "3,0", "3,4")
    fun `should reject an empty navigator or a current number outside it`(total: Int, current: Int) {
        assertThrows(IllegalArgumentException::class.java) { QuestionNavData(total, current = current) }
    }

    @Test
    fun `should reject a status number outside the questions`() {
        val error = assertThrows(IllegalArgumentException::class.java) { QuestionNavData(3, answered = setOf(4)) }

        assertTrue("[4]" in error.message.orEmpty())
    }
}
