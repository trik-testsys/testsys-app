package tech.testsys.web.ui.forms

import com.github.mvysny.kaributesting.v10._setValue
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.select.Select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestRow
import tech.testsys.web.ui.control

class ChoiceTests : MockVaadinTests() {
    private enum class Language { Kotlin, Python }

    @Test
    fun `should offer all select items`() {
        buildTestRow { select("Язык", labelSize = 4, size = 20, Language.entries, itemLabel = { language -> language.name }) }

        assertEquals(2, control<Select<*>>("Язык").listDataView.itemCount)
    }

    @Test
    fun `should report selected item through the handle`() {
        lateinit var input: ValueInput<Language?>
        buildTestRow { input = select("Язык", labelSize = 4, size = 20, Language.entries, itemLabel = { language -> language.name }) }

        input.value = Language.Kotlin

        assertEquals(Language.Kotlin, input.value)
    }

    @Test
    fun `should report checkbox state through the handle`() {
        lateinit var input: ValueInput<Boolean>
        buildTestRow { input = checkbox("Открытый тур", labelSize = 4, size = 20) }

        control<Checkbox>("Открытый тур")._setValue(true)

        assertTrue(input.value)
    }
}
