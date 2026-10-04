package tech.testsys.web.components.quiz

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.html.NativeButton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.SelectionHandle
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.find

class QuizOptionTests : MockVaadinTests() {
    @Test
    fun `should reflect multiple selected state and update the caption`() {
        lateinit var handle: SelectionHandle<QuizOptionData>
        var choices = 0
        val root = buildTestContent { handle = quizOption(QuizOptionData("Answer", isMultiple = true)) { onChange { choices++ } } }
        val button = root.find("ts-qopt") as NativeButton

        button._click()

        assertEquals("true", button.element.getAttribute("aria-pressed"))
        assertTrue("ts-qopt--multi" in button.element.classList)
        assertEquals(1, choices)
        handle.data = QuizOptionData("Changed")
        assertEquals("false", button.element.getAttribute("aria-pressed"))
        assertEquals("Changed", button.find("ts-qopt__label").element.text)
    }

    @ParameterizedTest
    @EnumSource(value = QuizResult::class, names = ["Correct", "Wrong"])
    fun `should lock checked results without accepting choices`(result: QuizResult) {
        lateinit var handle: SelectionHandle<QuizOptionData>
        var choices = 0
        val root = buildTestContent {
            handle = quizOption(QuizOptionData("Answer", isSelected = true, result = result)) { onChange { choices++ } }
        }
        val button = root.find("ts-qopt") as NativeButton

        button._click()

        assertEquals("true", button.element.getAttribute("aria-disabled"))
        assertTrue("ts-qopt--locked" in button.element.classList)
        assertTrue(handle.data.isSelected)
        assertEquals(0, choices)
    }
}
