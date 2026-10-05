package tech.testsys.web.devapp.demo.ui

import com.github.mvysny.kaributesting.v10.MockVaadin
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Table
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.texts.buildUiTexts
import tech.testsys.web.devapp.demo.DemoSession
import tech.testsys.web.devapp.demo.model.actor

class DemoResultMatrixTests {
    @BeforeEach
    fun setUpVaadin() = MockVaadin.setup()

    @AfterEach
    fun tearDownVaadin() = MockVaadin.tearDown()

    // http://localhost:8081/dev/demo/organizer.results — user-reported result matrix layout.
    @Test
    @Tag("regression")
    fun `should fill the available grid with two task columns`() {
        val table = buildMatrix(taskCount = 2)

        assertEquals("100.0%", table.element.style.get("--ts-table-width"))
        assertEquals("24", table.element.style.get("--ts-table-used"))
        assertEquals(listOf("25.0%", "37.5%", "37.5%"), columnWidths(table))
    }

    @Test
    fun `should retain numeric scores and attempt labels in the shared matrix`() {
        val table = buildMatrix(taskCount = 2)

        val body = table.element.children.toList().single { element -> element.tag == "tbody" }
        assertTrue(body.textRecursively.contains("37балловРешений: 2"))
        assertTrue(body.textRecursively.contains("Нет результатаРешений: 0"))
    }

    // http://localhost:8081/dev/demo/organizer.results — user-reported result matrix layout.
    @Test
    @Tag("regression")
    fun `should distribute indivisible remaining fractions over the first task columns`() {
        val table = buildMatrix(taskCount = 5)

        assertEquals("100.0%", table.element.style.get("--ts-table-width"))
        assertEquals(
            listOf("25.0%", "16.666666666666664%", "16.666666666666664%", "16.666666666666664%", "12.5%", "12.5%"),
            columnWidths(table),
        )
    }

    // http://localhost:8081/dev/demo/organizer.results — user-reported result matrix layout.
    @ParameterizedTest
    @Tag("regression")
    @ValueSource(ints = [0, 1])
    fun `should fill the grid with zero or one task`(taskCount: Int) {
        val table = buildMatrix(taskCount)

        assertEquals("100.0%", table.element.style.get("--ts-table-width"))
        assertEquals("24", table.element.style.get("--ts-table-used"))
        assertEquals(taskCount + 1, columnWidths(table).size)
    }

    @Test
    fun `should retain all thirty task columns in the expanded scrollable matrix`() {
        val table = buildMatrix(taskCount = 30)

        assertEquals("66", table.element.style.get("--ts-table-used"))
        assertEquals("100.0%", table.element.style.get("--ts-table-width"))
        assertEquals(31, columnWidths(table).size)
        assertTrue(table.element.textRecursively.contains("Задача 30"))
        assertTrue(table.parent.orElseThrow().element.classList.contains("ts-table-scroll"))
    }

    private fun columnWidths(table: Table): List<String> = table.element.children.toList().single { element -> element.tag == "colgroup" }
        .children.toList().map { column -> column.style.get("width") }

    private fun buildMatrix(taskCount: Int): Table {
        val session = DemoSession()
        val actor = session.state.actor("Организатор")
        val participant = session.state.users.first { user -> user.role == "Участник" }
        val tasks =
            List(taskCount) { index -> session.state.tasks.first().copy(id = "matrix-task-$index", name = "Задача ${index + 1}") }
        val tour = session.state.tours.first().copy(taskIds = tasks.map { task -> task.id })
        val solutions = tasks.take(1).flatMap { task ->
            listOf(
                session.state.solutions.first().copy(
                    id = "matrix-best",
                    userId = participant.id,
                    taskId = task.id,
                    status = "Checked",
                    score = 37,
                ),
                session.state.solutions.first().copy(
                    id = "matrix-other",
                    userId = participant.id,
                    taskId = task.id,
                    status = "Checked",
                    score = 12,
                ),
            )
        }
        session.state = session.state.copy(tasks = tasks, solutions = solutions)
        val context = DemoContext(session, "organizer.results", {}, {})
        val view = object : TestSysView(buildUiTexts()) {
            init {
                page(CabinetHeader()) {
                    block("Результаты") { demoResultMatrix(context, actor, tour, listOf(participant)) }
                }
            }
        }
        UI.getCurrent().add(view)
        return view._get<Table>()
    }
}
