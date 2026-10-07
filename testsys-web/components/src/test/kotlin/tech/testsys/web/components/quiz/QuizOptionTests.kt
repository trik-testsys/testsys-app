package tech.testsys.web.components.quiz

import com.github.mvysny.kaributesting.v10._click
import com.vaadin.flow.component.Component
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
import tech.testsys.web.components.findAll

class QuizOptionTests : MockVaadinTests() {
    @Test
    fun `should select a multiple choice option on click and report it`() {
        var choices = 0
        val root = buildTestContent {
            quizOption(QuizOptionData("Answer", isMultiple = true)) { onChange { choices++ } }
        }
        val button = option(root)

        button._click()

        assertEquals("true", button.element.getAttribute("aria-pressed"))
        assertTrue("ts-qopt--multi" in button.element.classList)
        assertEquals(1, choices)
    }

    @Test
    fun `should show new data set through the handle without a callback`() {
        lateinit var handle: SelectionHandle<QuizOptionData>
        var choices = 0
        val root = buildTestContent {
            handle = quizOption(QuizOptionData("Answer", isMultiple = true, isSelected = true)) { onChange { choices++ } }
        }

        handle.data = QuizOptionData("Changed")

        val button = option(root)
        assertEquals("false", button.element.getAttribute("aria-pressed"))
        assertEquals("Changed", button.find("ts-qopt__label").element.text)
        assertEquals(0, choices)
    }

    @ParameterizedTest
    @EnumSource(value = QuizResult::class, names = ["Correct", "Wrong"])
    fun `should lock checked results without accepting choices`(result: QuizResult) {
        lateinit var handle: SelectionHandle<QuizOptionData>
        var choices = 0
        val root = buildTestContent {
            handle = quizOption(QuizOptionData("Answer", isSelected = true, result = result)) { onChange { choices++ } }
        }
        val button = option(root)

        button._click()

        assertEquals("true", button.element.getAttribute("aria-disabled"))
        assertTrue("ts-qopt--locked" in button.element.classList)
        assertTrue(handle.data.isSelected)
        assertEquals(0, choices)
    }

    private fun option(root: Component): NativeButton = root.findAll("ts-qopt").filterIsInstance<NativeButton>().single()
}
