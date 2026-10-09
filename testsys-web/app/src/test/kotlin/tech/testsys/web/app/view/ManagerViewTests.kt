package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.datepicker.DatePicker
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.internal.nodefeature.ElementListenerMap
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@SpringBootTest
class ManagerViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var classes: ClassRepository

    @Autowired
    private lateinit var competitions: CompetitionRepository

    @Test
    fun `should open the classes tab when no section is given`() {
        signInManager()

        UI.getCurrent().navigate(ManagerView::class.java)

        assertEquals(ManagerView::class.java, currentView)
        assertEquals("manager/classes", UI.getCurrent().internals.activeViewLocation.path)
    }

    @Test
    fun `should show the forbidden screen to a user without the manager role`() {
        signIn(fixtures.multipleRoleUser())

        UI.getCurrent().navigate(ManagerView::class.java, managerSection(CLASSES_SECTION))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the classes of the manager with the number of their students`() {
        val manager = signInManager()
        fixtures.studyClass(owner = manager, name = "Кружок", students = listOf(fixtures.multipleRoleUser(), fixtures.multipleRoleUser()))
        fixtures.studyClass(owner = fixtures.multipleRoleUser(), name = "Чужой класс")

        UI.getCurrent().navigate(ManagerView::class.java, managerSection(CLASSES_SECTION))

        val text = tableText()
        assertTrue("Кружок" in text, text)
        assertTrue("Чужой класс" !in text, text)
        assertTrue(text.endsWith("2"), text)
    }

    @Test
    fun `should show the competitions of the manager on the competitions tab`() {
        val manager = signInManager()
        fixtures.competition(owner = manager, name = "Весенний кубок")

        UI.getCurrent().navigate(ManagerView::class.java, managerSection(COMPETITIONS_SECTION))

        val text = tableText()
        assertTrue("Весенний кубок" in text, text)
        assertTrue(text.endsWith("0"), text)
    }

    @Test
    fun `should create a class from the dialog and show it in the table`() {
        val manager = signInManager()
        UI.getCurrent().navigate(ManagerView::class.java, managerSection(CLASSES_SECTION))

        UI.getCurrent()._get<Button> { text = "Создать класс" }._click()
        dialogField()._value = "Новый класс"
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        val created = classes.findAvailableToManager(ownerId = manager.id, pagination = Pagination(page = 0, size = 10)).content
        assertEquals(listOf("Новый класс"), created.map { studyClass -> studyClass.data.name })
        assertTrue("Новый класс" in tableText())
    }

    @Test
    fun `should create nothing if the name of the class is blank`() {
        val manager = signInManager()
        UI.getCurrent().navigate(ManagerView::class.java, managerSection(CLASSES_SECTION))

        UI.getCurrent()._get<Button> { text = "Создать класс" }._click()
        dialogField()._value = "  "
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        assertEquals(0, classes.findAvailableToManager(ownerId = manager.id, pagination = Pagination(page = 0, size = 10)).totalElements)
    }

    @Test
    fun `should create a competition from the dialog`() {
        val manager = signInManager()
        UI.getCurrent().navigate(ManagerView::class.java, managerSection(COMPETITIONS_SECTION))

        UI.getCurrent()._get<Button> { text = "Создать соревнование" }._click()
        dialogField()._value = "Олимпиада"
        UI.getCurrent()._get<Button> { text = "Создать" }._click()

        val created = competitions.findAvailableToManager(ownerId = manager.id, pagination = Pagination(page = 0, size = 10)).content
        assertEquals(listOf("Олимпиада"), created.map { competition -> competition.data.name })
        assertTrue("Олимпиада" in tableText())
    }

    @Test
    fun `should show only the classes whose name contains the filter text after it is applied`() {
        val manager = signInManager()
        fixtures.studyClass(owner = manager, name = "Робототехника 7А")
        fixtures.studyClass(owner = manager, name = "Программирование")
        UI.getCurrent().navigate(ManagerView::class.java, managerSection(CLASSES_SECTION))

        field("Название")._value = "робот"
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val text = tableText()
        assertTrue("Робототехника 7А" in text, text)
        assertTrue("Программирование" !in text, text)
    }

    @Test
    fun `should show no competitions created before the lower bound of the date filter`() {
        val manager = signInManager()
        fixtures.competition(owner = manager, name = "Весенний кубок")
        UI.getCurrent().navigate(ManagerView::class.java, managerSection(COMPETITIONS_SECTION))

        UI.getCurrent()._find<DatePicker>().first()._value = LocalDate.now().plusDays(1)
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val text = tableText()
        assertTrue("Весенний кубок" !in text, text)
    }

    @Test
    fun `should show the competitions created on the day of the date filter`() {
        val manager = signInManager()
        fixtures.competition(owner = manager, name = "Весенний кубок")
        UI.getCurrent().navigate(ManagerView::class.java, managerSection(COMPETITIONS_SECTION))
        val pickers = UI.getCurrent()._find<DatePicker>()

        pickers.first()._value = LocalDate.now()
        pickers.last()._value = LocalDate.now()
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val text = tableText()
        assertTrue("Весенний кубок" in text, text)
    }

    @Test
    fun `should open the page of a class from its row`() {
        val studyClass = fixtures.studyClass(owner = signInManager(), name = "Кружок")
        UI.getCurrent().navigate(ManagerView::class.java, managerSection(CLASSES_SECTION))

        clickRow(UI.getCurrent()._find<Table>().single()._get<TableBody>()._get<TableRow>())

        assertEquals(ManagerClassView::class.java, currentView)
        assertEquals("manager/classes/${studyClass.id.value}", UI.getCurrent().internals.activeViewLocation.path)
    }

    private fun signInManager(): MultipleRoleUser =
        fixtures.grantRole(fixtures.multipleRoleUser(), CommunityRole.Manager).also { manager -> signIn(manager) }

    private fun field(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun dialogField(): TextField = UI.getCurrent()._get<Dialog>()._get<TextField>()

    private fun tableText(): String = UI.getCurrent()._find<Table>().single().element.textRecursively
}

/** Clicks [row] as the client does when the click passes every filter of the row. */
internal fun clickRow(row: TableRow) {
    val data = ObjectMapper().createObjectNode().apply {
        row.element.node.getFeature(ElementListenerMap::class.java).getExpressions("click").forEach { expression -> put(expression, true) }
    }
    row._fireDomEvent("click", data)
}
