package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.select.Select
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.ResourceVo
import tech.testsys.web.app.service.developer.TestVo
import tech.testsys.web.app.service.toVo
import javax.sql.DataSource

@SpringBootTest
class DeveloperTaskViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var developerService: DeveloperService

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var dataSource: DataSource

    private val developers by lazy { DeveloperFixtures(fixtures, developerService, tasks, dataSource) }

    @Test
    fun `should show the forbidden screen for a task of another developer`() {
        developers.signInDeveloper()
        val task = developers.task()
        developers.signInDeveloper()

        open(task.id)

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Нет доступа", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should show the name of the task in the head`() {
        developers.signInDeveloper()
        val task = developers.task("Движение по линии")

        open(task.id)

        assertEquals("Задача «Движение по линии»", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should upload the described files as resources of the task`() {
        developers.signInDeveloper()
        val task = developers.task()
        open(task.id)

        page().describeUploads(task.id, listOf(FileData(uploadedFilename = "world.xml", content = "<world/>".toByteArray())))
        chooseType(ResourceKind.Test)
        clickButton("Загрузить")

        val uploaded = developerService.viewResources().single()
        assertTrue(uploaded is TestVo)
        assertEquals("world", uploaded.name)
        assertTrue("worldПолигон" in tableText("Рабочая версия"), tableText("Рабочая версия"))
    }

    @Test
    fun `should upload nothing until the language and expected score of a developer solution are given`() {
        developers.signInDeveloper()
        val task = developers.task()
        open(task.id)

        page().describeUploads(task.id, listOf(FileData(uploadedFilename = "solution.py", content = "print(1)".toByteArray())))
        chooseType(ResourceKind.DeveloperSolution)
        clickButton("Загрузить")

        assertEquals(emptyList<ResourceVo>(), developerService.viewResources())
        assertTrue(openDialog().isOpened)
    }

    @Test
    fun `should attach a chosen resource to a new task without asking`() {
        developers.signInDeveloper()
        val task = developers.task()
        val statement = developers.statement(task.id)
        open(task.id)

        clickButton("Прикрепить")
        resourcesField().value = setOf(statement)
        clickButton("Прикрепить")

        assertEquals(statement.id, developerService.viewTask(task.id).first.wip?.statement)
        assertTrue("УсловиеУсловиеПоследняя" in tableText("Рабочая версия"), tableText("Рабочая версия"))
    }

    @Test
    fun `should ask to confirm the new state before detaching a resource of a committed task`() {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        open(taskId)

        rowMenu("Действия с ресурсом «Условие»")._clickItemWithCaption("Открепить")
        val warning = openDialog().element.textRecursively
        clickButton("Продолжить")

        assertTrue("Задача перейдёт в состояние «Не зафиксирована»" in warning, warning)

        val wip = developerService.viewTask(taskId).first.wip
        assertNotNull(wip)
        assertNull(wip?.statement)
    }

    @Test
    fun `should ask to confirm the new state before changing the versions of a committed task`() {
        developers.signInDeveloper()
        val version = developers.trikStudioVersion()
        val added = developers.trikStudioVersion()
        val taskId = developers.committedTask(version)
        open(taskId)

        clickButton("Изменить")
        versionsField().value = setOf(version, added)
        clickButton("Сохранить")
        val warning = openDialog().element.textRecursively
        clickButton("Продолжить")

        assertTrue("Задача перейдёт в состояние «Не зафиксирована»" in warning, warning)
        assertEquals(setOf(version, added), developerService.viewTask(taskId).first.wip?.supportedTrikStudioVersions?.toSet())
    }

    @Test
    fun `should save the name of a committed task without asking`() {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        open(taskId)

        clickButton("Изменить")
        textField("Название")._value = "Сортировка кубиков"
        clickButton("Сохранить")

        assertEquals("Задача «Сортировка кубиков»", UI.getCurrent()._get<H1>().text)
        assertNull(developerService.viewTask(taskId).first.wip)
    }

    @Test
    fun `should keep committing unavailable without a successful testing`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(task.id)

        assertFalse(pageButton("Зафиксировать").isEnabled)
    }

    @Test
    fun `should show why testing was refused`() {
        developers.signInDeveloper()
        val task = developers.task()
        open(task.id)

        pageButton("Протестировать")._click()

        assertEquals("Не удалось запустить тестирование", lastToastTitle())
        assertEquals("Прикрепите условие.", lastToastDescription())
    }

    @Test
    fun `should keep sharing unavailable for a new task`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(task.id)

        assertFalse(pageButton("Предоставить доступ").isEnabled)
    }

    @Test
    fun `should share a committed task to a community after the confirmation`() {
        val community = fixtures.community()
        developers.signInDeveloper(community)
        val taskId = developers.committedTask(developers.trikStudioVersion())
        open(taskId)

        clickButton("Предоставить доступ")
        communitiesField().value = setOf(community.toVo())
        clickButton("Предоставить")
        clickButton("Предоставить")

        assertEquals(listOf(community.id), developerService.viewTask(taskId).second.map { shared -> shared.id })
    }

    private fun open(taskId: TaskId) {
        UI.getCurrent().navigate(DeveloperTaskView::class.java, taskParameters(taskId))
    }

    private fun page(): DeveloperTaskView = UI.getCurrent()._get<DeveloperTaskView>()

    @Suppress("UNCHECKED_CAST")
    private fun chooseType(kind: ResourceKind) {
        val select = openDialog()._find<Select<*>>().single { candidate -> candidate.ariaLabel.orElse(null) == "Тип" }
        checkNotNull(select as? Select<ResourceKind?>).value = kind
    }

    /** Returns the supported versions field of the page, the multi-selection named «Версии TRIK Studio». */
    @Suppress("UNCHECKED_CAST")
    private fun versionsField(): CustomField<Set<TrikStudioVersion>> {
        val field = UI.getCurrent()._find<CustomField<*>>().single { candidate ->
            candidate.element.getProperty("accessibleName") == "Версии TRIK Studio"
        }
        return checkNotNull(field as? CustomField<Set<TrikStudioVersion>>)
    }

    /** Returns the resources field of the open dialog, the only custom field in it. */
    @Suppress("UNCHECKED_CAST")
    private fun resourcesField(): CustomField<Set<ResourceVo>> =
        checkNotNull(openDialog()._get<CustomField<*>>() as? CustomField<Set<ResourceVo>>)

    /** Returns the communities field of the open dialog, the only custom field in it. */
    @Suppress("UNCHECKED_CAST")
    private fun communitiesField(): CustomField<Set<CommunityVo>> =
        checkNotNull(openDialog()._get<CustomField<*>>() as? CustomField<Set<CommunityVo>>)
}
