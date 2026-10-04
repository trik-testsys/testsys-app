package tech.testsys.web.components.quiz

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.findAll
import tech.testsys.web.components.testTexts

class QuestionNavTests : MockVaadinTests() {
    @Test
    fun `should update current answered flagged and accessible question states`() {
        lateinit var handle: SelectionHandle<QuestionNavData>
        val root = buildTestContent { handle = questionNav(QuestionNavData(3, answered = setOf(2), flagged = setOf(3))) }
        var cells = root.findAll("ts-qnav__cell")
        assertEquals("step", cells[0].element.getAttribute("aria-current"))
        assertTrue("ts-qnav__cell--answered" in cells[1].element.classList)
        assertEquals(testTexts.components.questionStatus(3, false, true), cells[2].element.getAttribute("aria-label"))
        assertEquals("true", root.findAll("ts-qnav__flag").single().element.getAttribute("aria-hidden"))
        handle.data = QuestionNavData(2, current = 2, answered = setOf(2))
        cells = root.findAll("ts-qnav__cell")
        assertEquals(2, cells.size)
        assertEquals("step", cells[1].element.getAttribute("aria-current"))
        assertFalse("ts-qnav__cell--answered" in cells[1].element.classList)
        assertTrue(root.findAll("ts-qnav__flag").isEmpty())
    }
}
