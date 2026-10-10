package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.datetimepicker.DateTimePicker
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.select.Select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.developer.DeveloperService
import java.time.LocalDateTime
import javax.sql.DataSource

@SpringBootTest
class DeveloperViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var developerService: DeveloperService

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var dataSource: DataSource

    private val developers by lazy { DeveloperFixtures(fixtures, developerService, tasks, dataSource) }

    @Test
    fun `should open the tasks tab when no section is given`() {
        developers.signInDeveloper()

        UI.getCurrent().navigate(DeveloperView::class.java)

        assertEquals(DeveloperView::class.java, currentView)
        assertEquals("developer/tasks", currentPath())
    }

    @Test
    fun `should show the forbidden screen to a user without the developer role`() {
        signIn(fixtures.multipleRoleUser())

        open(TASKS_SECTION)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should create a task from the dialog and refresh its list`() {
        developers.signInDeveloper()
        open(TASKS_SECTION)

        clickButton("Создать задачу")
        textField("Название")._value = "Движение по линии"
        clickButton("Создать")

        assertEquals(DeveloperView::class.java, currentView)
        assertTrue("Движение по линии" in tableText("Состояние"))
        assertEquals("Задача создана", lastToastTitle())
    }

    @Test
    fun `should preserve applied filters after task creation`() {
        developers.signInDeveloper()
        developers.task("Линия")
        open(TASKS_SECTION)
        textField("Название")._value = "Линия"
        clickButton("Применить")
        clickButton("Создать задачу")
        textField("Название")._value = "Кубики"

        clickButton("Создать")

        assertEquals(DeveloperView::class.java, currentView)
        assertEquals("Линия", textField("Название").value)
        assertTrue("Кубики" !in tableText("Состояние"))
        assertEquals("Задача создана", lastToastTitle())
    }

    @Test
    fun `should make only own task rows navigable`() {
        val community = fixtures.community()
        developers.signInDeveloper(community)
        val shared = developers.committedTask(developers.trikStudioVersion(), name = "Чужая задача")
        developerService.shareTask(shared, setOf(community.id))
        developers.signInDeveloper(community)
        developers.task("Своя задача")

        open(TASKS_SECTION)

        val clickable = UI.getCurrent()._get<Table>()._find<TableRow> { classes = "ts-row-clickable" }
        assertEquals(1, clickable.size)
        assertTrue("Своя задача" in clickable.single().element.textRecursively)
        assertTrue("Чужая задача" in tableText("Состояние"), tableText("Состояние"))
    }

    @Test
    fun `should open the page of an own task from its row`() {
        developers.signInDeveloper()
        val task = developers.task("Своя задача")
        open(TASKS_SECTION)

        val row = UI.getCurrent()._get<Table>()._get<TableBody>()._get<TableRow>()
        row._fireDomEvent("click", acceptedClick(row))

        assertEquals("developer/tasks/${task.id.value}", currentPath())
    }

    @Test
    fun `should show only the tasks whose name contains the filter text after it is applied`() {
        developers.signInDeveloper()
        developers.task("Движение по линии")
        developers.task("Сортировка кубиков")
        open(TASKS_SECTION)

        textField("Название")._value = "линии"
        clickButton("Применить")

        val text = tableText("Состояние")
        assertTrue("Движение по линии" in text, text)
        assertTrue("Сортировка кубиков" !in text, text)
    }

    @Test
    fun `should create a contest from the dialog and show it in the table`() {
        developers.signInDeveloper()
        val version = developers.trikStudioVersion()
        open(CONTESTS_SECTION)

        clickButton("Создать тур")
        textField("Название")._value = "Весенний тур"
        chooseVersion(version)
        clickButton("Создать")

        val contests = developerService.viewContests(Pagination(page = 0, size = 10)).content
        assertEquals(listOf("Весенний тур"), contests.map { (contest, _) -> contest.name })
        assertTrue("Весенний тур" in tableText("Задачи"), tableText("Задачи"))
        assertEquals("Тур создан", lastToastTitle())
    }

    @Test
    fun `should create no contest if its end is set without its start`() {
        developers.signInDeveloper()
        val version = developers.trikStudioVersion()
        open(CONTESTS_SECTION)

        clickButton("Создать тур")
        textField("Название")._value = "Весенний тур"
        chooseVersion(version)
        openDialog()._find<DateTimePicker>().last().value = LocalDateTime.of(2026, 5, 1, 10, 0)
        clickButton("Создать")

        assertEquals(0L, developerService.viewContests(Pagination(page = 0, size = 10)).totalElements)
        assertTrue(openDialog().isOpened)
    }

    @Test
    fun `should open the page of a contest from its row`() {
        developers.signInDeveloper()
        val contest = developers.contest(developers.trikStudioVersion())
        open(CONTESTS_SECTION)
        val row = UI.getCurrent()._get<Table>()._get<TableBody>()._get<TableRow>()

        row._fireDomEvent("click", acceptedClick(row))

        assertEquals("developer/contests/${contest.id.value}", currentPath())
    }

    private fun open(section: String) {
        UI.getCurrent().navigate(DeveloperView::class.java, developerSectionParameters(section))
    }

    @Suppress("UNCHECKED_CAST")
    private fun chooseVersion(version: TrikStudioVersion) {
        val select = openDialog()._find<Select<*>>().single { candidate -> candidate.ariaLabel.orElse(null) == "Версия TRIK Studio" }
        checkNotNull(select as? Select<TrikStudioVersion?>).value = version
    }
}
