package tech.testsys.web.devapp.demo.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.texts.buildUiTexts
import tech.testsys.web.devapp.demo.model.DemoRow
import tech.testsys.web.devapp.demo.model.DemoTableState

class DemoTablesTests {
    private val state = DemoTableState()

    @BeforeEach
    fun setUp() {
        MockVaadin.setup()
        val rows = listOf(DemoRow(id = "p1", title = "Анна"), DemoRow(id = "p2", title = "Иван"))
        UI.getCurrent().add(
            object : TestSysView(buildUiTexts()) {
                init {
                    page(CabinetHeader()) { row { block(title = "Участники") { demoTable(state, rows) } } }
                }
            },
        )
    }

    @AfterEach
    fun tearDown() = MockVaadin.tearDown()

    @Test
    fun `should keep the table unchanged until the draft is applied`() {
        val search = UI.getCurrent()._get<TextField>()

        search.value = "Анна"

        assertEquals(listOf("p1", "p2"), shownIds())
        assertEquals("", state.applied.query)
    }

    @Test
    fun `should filter the table by the applied draft`() {
        UI.getCurrent()._get<TextField>().value = "Анна"

        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        assertEquals(listOf("p1"), shownIds())
    }

    @Test
    fun `should restore empty defaults on reset`() {
        val search = UI.getCurrent()._get<TextField>()
        search.value = "Анна"
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        UI.getCurrent()._get<Button> { text = "Сбросить" }._click()

        assertEquals("", search.value)
        assertEquals(listOf("p1", "p2"), shownIds())
    }

    private fun shownIds(): List<String> {
        val body = UI.getCurrent()._get<Table>().element.children.toList().single { element -> element.tag == "tbody" }
        return body.children.toList().map { row -> row.children.toList().first().textRecursively }
    }
}
