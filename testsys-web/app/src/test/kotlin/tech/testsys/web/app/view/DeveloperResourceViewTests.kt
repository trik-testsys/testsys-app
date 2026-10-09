package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.dialog.Dialog
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.textfield.IntegerField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.developer.DeveloperService
import javax.sql.DataSource

@SpringBootTest
class DeveloperResourceViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var developerService: DeveloperService

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var dataSource: DataSource

    private val developers by lazy { DeveloperFixtures(fixtures, developerService, tasks, dataSource) }

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
        assertTrue("solution.py" in tableText("Скачать"), tableText("Скачать"))
    }

    private fun open(taskId: TaskId, versionBucket: VersionBucket) {
        UI.getCurrent().navigate(DeveloperResourceView::class.java, resourceParameters(taskId, versionBucket))
    }
}
