package tech.testsys.web.components.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.web.components.DataHandle
import tech.testsys.web.components.MockVaadinTests
import tech.testsys.web.components.buildTestContent
import tech.testsys.web.components.find
import tech.testsys.web.components.testTexts

class DifficultyTests : MockVaadinTests() {
    @ParameterizedTest
    @CsvSource("Easy,1", "Medium,2", "Hard,3")
    fun `should update accessible difficulty and indicators without visible caption`(level: DifficultyLevel, filled: Int) {
        lateinit var handle: DataHandle<DifficultyLevel>
        val root = buildTestContent { handle = difficulty(DifficultyLevel.Easy, showLabel = false) }
        val indicator = root.find("ts-difficulty")

        handle.data = level

        assertEquals(testTexts.components.difficultyLabels[level.ordinal], indicator.element.getAttribute("aria-label"))
        val marks = indicator.children.toList().single().element
        assertEquals("true", marks.getAttribute("aria-hidden"))
        assertEquals(filled, marks.children.filter { !it.style.get("background").isNullOrBlank() }.count().toInt())
    }
}
