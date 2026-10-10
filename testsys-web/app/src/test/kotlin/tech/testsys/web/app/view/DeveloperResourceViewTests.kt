package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.IntegerField
import com.vaadin.flow.router.BeforeEnterEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.components.texts.buildUiTexts
import javax.sql.DataSource

@SpringBootTest
class DeveloperResourceViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var developerService: DeveloperService

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var headers: CabinetHeaders

    @Autowired
    private lateinit var fileContentReader: FileContentReader

    private val developers by lazy { DeveloperFixtures(fixtures, developerService, tasks, dataSource) }

    @Test
    fun `should distinguish the latest version from both attached versions in details and history`() {
        developers.signInDeveloper()
        val task = developers.task()
        val committed = developers.statement(task.id)
        val working = developerService.updateStatement(
            task.id,
            committed.id,
            file = FileData(uploadedFilename = "working.pdf", content = "working".toByteArray()),
        )
        val latest = developerService.updateStatement(
            task.id,
            working.id,
            file = FileData(uploadedFilename = "latest.pdf", content = "latest".toByteArray()),
        )
        val history = developerService.viewResource(task.id, latest.versionBucket)
        val revision = checkNotNull(task.wip)
        val loaded = task.copy(
            wip = revision.copy(statement = working.id),
            lastCommitted = revision.copy(statement = committed.id),
        )
        val service = mockk<DeveloperService>()
        every { service.viewResource(task.id, latest.versionBucket) } returns history
        every { service.viewTask(task.id) } returns (loaded to emptyList())
        val view = DeveloperResourceView(buildUiTexts(), headers, service, fileContentReader)
        val event = mockk<BeforeEnterEvent>()
        every { event.routeParameters } returns resourceParameters(task.id, latest.versionBucket)
        UI.getCurrent().removeAll()
        UI.getCurrent().add(view)

        view.beforeEnter(event)

        assertEquals(latest.versionBucket.value.toString(), textField("ID").value)
        assertEquals(latest.id.value.toString(), textField("ID последней версии").value)
        assertEquals(working.id.value.toString(), textField("ID в рабочей версии").value)
        assertEquals(committed.id.value.toString(), textField("ID в зафиксированной версии").value)
        val rows = UI.getCurrent()._get<Table>()._get<TableBody>()._find<TableRow>()
        assertEquals(
            listOf(latest.id.value.toString(), working.id.value.toString(), committed.id.value.toString()),
            rows.map { row -> row.element.getChild(0).textRecursively },
        )
        assertEquals(listOf("latest.pdf", "working.pdf", "statement.pdf"), rows.map { row -> row.element.getChild(3).textRecursively })
        assertEquals(
            listOf("", "Прикреплено в рабочей", "Прикреплено в зафиксированной"),
            rows.map { row -> row.element.getChild(4).textRecursively },
        )
    }

    @Test
    fun `should distinguish an unattached resource from a missing committed revision in details`() {
        developers.signInDeveloper()
        val task = developers.task()
        val statement = developers.statement(task.id)

        open(task.id, statement.versionBucket)
        clickButton("Изменить")

        assertEquals("Не прикреплён", textField("ID в рабочей версии").value)
        assertEquals("Не зафиксирована", textField("ID в зафиксированной версии").value)
        assertTrue(textField("ID последней версии").isReadOnly)
        assertTrue(textField("ID в рабочей версии").isReadOnly)
        assertTrue(textField("ID в зафиксированной версии").isReadOnly)
    }

    @Test
    fun `should show a missing working revision with the actual committed resource identifier`() {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        val task = developerService.viewTask(taskId).first
        val statementId = checkNotNull(task.lastCommitted?.statement)
        val statement = developerService.viewResources().single { resource -> resource.id == statementId }

        open(taskId, statement.versionBucket)

        assertEquals("Нет рабочей версии", textField("ID в рабочей версии").value)
        assertEquals(statementId.value.toString(), textField("ID в зафиксированной версии").value)
    }

    @Test
    fun `should show an unattached committed resource separately from a missing working revision`() {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        val statement = developers.statement(taskId)

        open(taskId, statement.versionBucket)

        assertEquals("Нет рабочей версии", textField("ID в рабочей версии").value)
        assertEquals("Не прикреплён", textField("ID в зафиксированной версии").value)
    }

    @Test
    fun `should keep the resource file when replacements are uploaded and then removed`() {
        developers.signInDeveloper()
        val task = developers.task()
        val statement = developers.statement(task.id)
        open(task.id, statement.versionBucket)
        clickButton("Изменить")
        StudyPages.removeUpload(StudyPages.upload("first.pdf", "first".toByteArray()))
        StudyPages.removeUpload(StudyPages.upload("second.pdf", "second".toByteArray()))
        textField("Название")._value = "Обновлённое условие"

        clickButton("Сохранить")

        val history = developerService.viewResource(task.id, statement.versionBucket)
        assertEquals(1, history.size)
        assertEquals("statement.pdf", textField("Файл").value)
        assertEquals("Обновлённое условие", history.single().first.name)
    }

    @Test
    fun `should show the type and the name of the resource in the head`() {
        developers.signInDeveloper()
        val task = developers.task()
        val statement = developers.statement(task.id, name = "Условие задачи")

        open(task.id, statement.versionBucket)

        assertEquals("Условие «Условие задачи»", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the forbidden screen for a resource of a task of another developer`() {
        developers.signInDeveloper()
        val task = developers.task()
        val statement = developers.statement(task.id)
        developers.signInDeveloper()

        open(task.id, statement.versionBucket)

        assertEquals(OperationErrorView::class.java, currentView)
    }

    @Test
    fun `should rename an attached resource without asking`() {
        developers.signInDeveloper()
        val task = developers.task()
        val statement = developers.statement(task.id)
        developerService.attachStatement(task.id, statement.id)
        open(task.id, statement.versionBucket)

        clickButton("Изменить")
        textField("Название")._value = "Новое условие"
        clickButton("Сохранить")

        assertTrue(UI.getCurrent()._find<Dialog>().none { dialog -> dialog.isOpened })
        val history = developerService.viewResource(task.id, statement.versionBucket)
        assertEquals(listOf("Новое условие"), history.map { (version, _) -> version.name })
        assertEquals("Ресурс изменён", lastToastTitle())
    }

    @Test
    fun `should add a version of an attached developer solution after confirming the uncommitted changes`() {
        developers.signInDeveloper()
        val task = developers.task()
        val solution = developerService.addDeveloperSolution(
            taskId = task.id,
            resourceName = "Эталон",
            file = FileData(uploadedFilename = "solution.py", content = "print(1)".toByteArray()),
            language = TrikSupportedLanguage.Python,
            expectedScore = Score(10),
        )
        developerService.attachDeveloperSolution(task.id, solution.id)
        open(task.id, solution.versionBucket)

        clickButton("Изменить")
        UI.getCurrent()._find<IntegerField>().single { field -> field.ariaLabel.orElse(null) == "Ожидаемый балл" }._value = 20
        clickButton("Сохранить")
        val warning = openDialog().element.textRecursively
        clickButton("Изменить")

        assertTrue("незафиксированные изменения" in warning, warning)
        assertEquals(2, developerService.viewResource(task.id, solution.versionBucket).size)
        assertTrue("solution.py" in tableText("ID версии"), tableText("ID версии"))
        assertEquals("Ресурс изменён", lastToastTitle())
    }

    private fun open(taskId: TaskId, versionBucket: VersionBucket) {
        UI.getCurrent().navigate(DeveloperResourceView::class.java, resourceParameters(taskId, versionBucket))
    }
}
