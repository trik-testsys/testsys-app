package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._clickItemWithCaption
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._fireDomEvent
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableHeaderCell
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.select.Select
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.router.BeforeEnterEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.ResourceVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.app.service.developer.TaskValidationSnapshotVo
import tech.testsys.web.app.service.developer.TestVo
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.app.service.toVo
import tech.testsys.web.components.texts.buildUiTexts
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import javax.sql.DataSource

@SpringBootTest
internal class DeveloperTaskViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var developerService: DeveloperService

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var headers: CabinetHeaders

    @Autowired
    private lateinit var multipleRoleUserService: MultipleRoleUserService

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
        assertTrue("worldПолигон" in resourceTable().element.textRecursively)
        assertEquals("Ресурсы загружены", lastToastTitle())
    }

    @Test
    fun `should keep two uploaded resource forms independent and announce one batch success`() {
        developers.signInDeveloper()
        val task = developers.task()
        open(task.id)
        page().describeUploads(
            task.id,
            listOf(
                FileData(uploadedFilename = "first.xml", content = "<world/>".toByteArray()),
                FileData(uploadedFilename = "second.pdf", content = "statement".toByteArray()),
            ),
        )
        val groups = openDialog()._find<Div> { classes = "ts-dialog__section" }
        chooseGroupType(groups[0], ResourceKind.Test)
        chooseGroupType(groups[1], ResourceKind.Statement)
        groups[0]._get<TextField>()._value = "Полигон"
        groups[1]._get<TextField>()._value = "Условие"

        clickButton("Загрузить")

        assertEquals(setOf("Полигон", "Условие"), developerService.viewResources().map { resource -> resource.name }.toSet())
        assertEquals(listOf("Ресурсы загружены"), UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.map { title -> title.text })
        assertEquals("Всего: 2.", lastToastDescription())
    }

    @Test
    fun `should not announce batch success when a later resource upload fails`() {
        developers.signInDeveloper()
        val task = developers.task()
        val first = FileData(uploadedFilename = "first.xml", content = "<world/>".toByteArray())
        val second = FileData(uploadedFilename = "second.xml", content = "<world/>".toByteArray())
        val resource = developerService.addTest(task.id, "first", first)
        val service = testingPage(task, emptyList())
        every { service.addTest(task.id, "first", first) } returns resource
        every { service.addTest(task.id, "second", second) } throws IllegalStateException("Upload failed")
        page().describeUploads(task.id, listOf(first, second))
        val groups = openDialog()._find<Div> { classes = "ts-dialog__section" }
        chooseGroupType(groups[0], ResourceKind.Test)
        chooseGroupType(groups[1], ResourceKind.Test)

        assertThrows(IllegalStateException::class.java) { clickButton("Загрузить") }

        assertEquals(0, UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.size)
    }

    @Test
    fun `should announce committing only after the commit succeeds`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = testingRequest(task).copy(
            execution = TaskValidationExecutionVo.Completed(emptyList(), emptyList(), emptyList(), Instant.EPOCH),
        )
        val service = testingPage(task, listOf(request))
        every { service.commitTask(task.id, false) } returns task
        clickButton("Зафиксировать")

        clickButton("Зафиксировать")

        assertEquals("Задача зафиксирована", lastToastTitle())
    }

    @Test
    fun `should report a refused commit without announcing success`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = testingRequest(task).copy(
            execution = TaskValidationExecutionVo.Completed(emptyList(), emptyList(), emptyList(), Instant.EPOCH),
        )
        val service = testingPage(task, listOf(request))
        every { service.commitTask(task.id, false) } answers { developerService.commitTask(task.id, false) }
        clickButton("Зафиксировать")

        clickButton("Зафиксировать")

        assertEquals("Не удалось зафиксировать задачу", lastToastTitle())
        assertEquals(1, UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.size)
    }

    @Test
    fun `should announce reverting after confirmation`() {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        val statement = developerService.viewTask(taskId).first.lastCommitted?.statement
        developerService.detachStatement(taskId, checkNotNull(statement))
        open(taskId)
        clickButton("Откатить")

        clickButton("Откатить")

        assertEquals("Изменения задачи отменены", lastToastTitle())
        assertNull(developerService.viewTask(taskId).first.wip)
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
        assertEquals(0, UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.size)
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
        assertEquals("Прикреплено в рабочей", resourceRows().single().element.getChild(2).textRecursively)
        assertEquals("Ресурсы прикреплены", lastToastTitle())
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
        UI.getCurrent()._find<TextField>().first { field -> field.ariaLabel.orElse(null) == "Название" }._value = "Сортировка кубиков"
        clickButton("Сохранить")

        assertEquals("Задача «Сортировка кубиков»", UI.getCurrent()._get<H1>().text)
        assertNull(developerService.viewTask(taskId).first.wip)
        assertEquals("Задача изменена", lastToastTitle())
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
        assertEquals("Доступ к задаче предоставлен", lastToastTitle())
    }

    @Test
    fun `should announce a newly started testing request`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = testingRequest(task)
        val service = testingPage(task, emptyList())
        every { service.testTask(task.id) } returns request

        pageButton("Протестировать")._click()

        assertEquals("Тестирование запущено", lastToastTitle())
        assertEquals("Результат появится в истории тестирований.", lastToastDescription())
    }

    @Test
    fun `should explain when testing returns the current active request`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = testingRequest(task)
        val service = testingPage(task, listOf(request))
        every { service.testTask(task.id) } returns request

        pageButton("Протестировать")._click()

        assertEquals("Тестирование уже идёт", lastToastTitle())
        assertEquals("Дождитесь результата текущего запроса.", lastToastDescription())
    }

    @Test
    fun `should open testing details as a separate page`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = developers.testing(task)
        open(task.id)
        val row = UI.getCurrent()._find<Table>().single { table -> "Запущено" in table.element.textRecursively }
            ._get<TableBody>()._get<TableRow>()

        row._fireDomEvent("click", acceptedClick(row))

        assertEquals(DeveloperTestingView::class.java, currentView)
        assertEquals("developer/tasks/${task.id.value}/testing/${request.id.value}", currentPath())
    }

    @Test
    fun `should not describe a resource removed from the upload list`() {
        developers.signInDeveloper()
        val task = developers.task()
        open(task.id)
        clickButton("Загрузить ресурсы")
        val identity = StudyPages.upload("removed.py", "print(1)".toByteArray())
        UI.getCurrent()._get<Upload>()._fireDomEvent(
            "testsys-transfer-remove",
            ObjectMapper().createObjectNode().put("event.detail.identity", identity),
        )

        clickButton("Далее")

        assertFalse("removed" in openDialog().element.textRecursively)
        assertEquals(emptyList<ResourceVo>(), developerService.viewResources())
    }

    @Test
    fun `should show no attachment badges for unattached resources without a committed revision`() {
        developers.signInDeveloper()
        val task = developers.task()
        developers.statement(task.id)

        open(task.id)

        assertEquals(
            listOf(
                "Условие",
                "Условие",
                "",
                "",
            ),
            resourceRows().single().element.children.map { cell -> cell.textRecursively }.toList(),
        )
        assertFalse(differencesField().isEnabled)
    }

    @Test
    fun `should show a missing working revision without treating committed resources as removed`() {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())

        open(taskId)

        resourceRows().forEach { row ->
            assertEquals("Прикреплено в зафиксированной", row.element.getChild(2).textRecursively)
        }
        assertFalse(differencesField().isEnabled)
        assertTrue("Для сравнения нужны рабочая и зафиксированная версии задачи." in filterPanel().element.textRecursively)
    }

    @Test
    fun `should show compact resource columns with badges for actual attached versions`() {
        developers.signInDeveloper()
        val task = developers.task()
        val old = developerService.addDeveloperSolution(
            taskId = task.id,
            resourceName = "Решение",
            file = FileData(uploadedFilename = "solution.py", content = "print(1)".toByteArray()),
            language = TrikSupportedLanguage.Python,
            expectedScore = Score(50),
        )
        val latest = developerService.updateDeveloperSolution(task.id, old.id, expectedScore = Score(100))
        val newest = developerService.updateDeveloperSolution(task.id, latest.id, expectedScore = Score(150))
        val revision = checkNotNull(task.wip)
        val loaded = developerService.viewTask(task.id).first.copy(
            wip = revision.copy(developerSolutions = listOf(latest.id)),
            lastCommitted = revision.copy(developerSolutions = listOf(old.id)),
        )

        testingPage(loaded, emptyList(), listOf(newest))

        val row = resourceRows().single()
        assertEquals(
            listOf(
                "Название",
                "Тип",
                "Прикреплено",
                "",
            ),
            resourceTable()._find<TableHeaderCell>().map { cell -> cell.element.textRecursively },
        )
        assertEquals(
            listOf(
                "Решение",
                "Авторское решение",
                "Прикреплено в зафиксированнойПрикреплено в рабочей",
                "",
            ),
            row.element.children.map { cell -> cell.textRecursively }.toList(),
        )
    }

    @ParameterizedTest
    @EnumSource(ResourceUsage::class)
    fun `should apply every usage filter to actual revisions`(usage: ResourceUsage) {
        val taskId = taskWithResourceDifferences()
        open(taskId)
        val expected = mapOf(
            ResourceUsage.All to setOf("Условие", "Упражнение", "Новый полигон", "Черновик"),
            ResourceUsage.Working to setOf("Упражнение", "Новый полигон"),
            ResourceUsage.Committed to setOf("Условие", "Упражнение"),
            ResourceUsage.AnyRevision to setOf("Условие", "Упражнение", "Новый полигон"),
            ResourceUsage.Unattached to setOf("Черновик"),
        )

        chooseFilter("Использование", usage)

        clickButton("Применить")

        assertEquals(expected.getValue(usage), resourceNames())
    }

    @Test
    fun `should show added removed and replaced resources when comparing revisions`() {
        val taskId = taskWithResourceDifferences()
        open(taskId)
        differencesField().value = true

        clickButton("Применить")

        assertEquals(setOf("Условие", "Упражнение", "Новый полигон"), resourceNames())
    }

    @Test
    fun `should exclude unchanged resources from revision differences`() {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        val task = developerService.viewTask(taskId).first
        testingPage(task.copy(wip = task.lastCommitted), emptyList(), developerService.viewResources())
        differencesField().value = true

        clickButton("Применить")

        assertEquals(emptySet<String>(), resourceNames())
        assertTrue("Ресурсы не найдены" in resourceTable().element.textRecursively)
    }

    @ParameterizedTest
    @EnumSource(value = ResourceUsage::class, names = ["Working", "Unattached"])
    fun `should not include committed resources in the working or unattached filter without a working revision`(usage: ResourceUsage) {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        open(taskId)

        chooseFilter("Использование", usage)

        clickButton("Применить")

        assertEquals(emptySet<String>(), resourceNames())
    }

    @ParameterizedTest
    @EnumSource(ResourceKind::class)
    fun `should filter resources by each type`(kind: ResourceKind) {
        val taskId = taskWithResourceDifferences()
        developerService.addDeveloperSolution(
            taskId = taskId,
            resourceName = "Решение",
            file = FileData(uploadedFilename = "solution.py", content = "print(1)".toByteArray()),
            language = TrikSupportedLanguage.Python,
            expectedScore = Score(100),
        )
        open(taskId)
        val expected = mapOf(
            ResourceKind.Statement to setOf("Условие"),
            ResourceKind.Exercise to setOf("Упражнение"),
            ResourceKind.Test to setOf("Новый полигон", "Черновик"),
            ResourceKind.DeveloperSolution to setOf("Решение"),
        )

        chooseFilter("Тип", kind)

        clickButton("Применить")

        assertEquals(expected.getValue(kind), resourceNames())
    }

    @Test
    fun `should combine filters only after apply and restore all rows on reset`() {
        val taskId = taskWithResourceDifferences()
        open(taskId)
        filterPanel()._get<TextField>()._value = "ПОЛИГОН"
        chooseFilter("Тип", ResourceKind.Test)
        chooseFilter("Использование", ResourceUsage.Working)
        differencesField().value = true
        assertEquals(4, resourceRows().size)
        clickButton("Применить")
        assertEquals(setOf("Новый полигон"), resourceNames())

        clickButton("Сбросить")

        assertEquals(4, resourceRows().size)
        assertEquals("", filterPanel()._get<TextField>().value)
        assertFalse(differencesField().value)
        assertNull(filterPanel()._find<Select<*>>().single { field -> field.ariaLabel.orElse(null) == "Тип" }.value)
    }

    @Test
    fun `should distinguish an empty filter result from a task without uploaded resources`() {
        val taskId = taskWithResourceDifferences()
        open(taskId)
        filterPanel()._get<TextField>()._value = "Несуществующий ресурс"

        clickButton("Применить")

        assertTrue("Ресурсы не найденыИзмените или сбросьте фильтры." in resourceTable().element.textRecursively)
        assertFalse("Ресурсов пока нет" in resourceTable().element.textRecursively)
    }

    @Test
    fun `should show the upload hint when there are no resources`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(task.id)

        assertTrue("Ресурсов пока нетЗагрузите" in resourceTable().element.textRecursively)
    }

    @Test
    fun `should reset filters after detaching and allow opening a resource row`() {
        val taskId = taskWithResourceDifferences()
        open(taskId)
        chooseFilter("Использование", ResourceUsage.Working)
        clickButton("Применить")

        rowMenu("Действия с ресурсом «Новый полигон»")._clickItemWithCaption("Открепить")

        assertEquals(setOf("Условие", "Упражнение", "Новый полигон", "Черновик"), resourceNames())
        assertEquals(
            ResourceUsage.All,
            filterPanel()._find<Select<*>>().single { field -> field.ariaLabel.orElse(null) == "Использование" }.value,
        )
        val row = resourceRows().single { resource -> resource.element.getChild(0).textRecursively == "Упражнение" }
        row._fireDomEvent("click", acceptedClick(row))
        assertEquals(DeveloperResourceView::class.java, currentView)
    }

    @Test
    fun `should filter before pagination including resources beyond the first page`() {
        developers.signInDeveloper()
        val task = developers.task()
        repeat(21) { index -> developers.statement(task.id, "Условие $index") }
        open(task.id)
        filterPanel()._get<TextField>()._value = "Условие 9"

        clickButton("Применить")

        assertEquals(setOf("Условие 9"), resourceNames())
    }

    private fun taskWithResourceDifferences(): TaskId {
        developers.signInDeveloper()
        val taskId = developers.committedTask(developers.trikStudioVersion())
        val committed = checkNotNull(developerService.viewTask(taskId).first.lastCommitted)
        developerService.detachStatement(taskId, checkNotNull(committed.statement))
        developerService.updateExercise(
            taskId = taskId,
            exerciseId = committed.exercises.single(),
            file = FileData(uploadedFilename = "new.qrs", content = "changed".toByteArray()),
        )
        val added = developerService.addTest(taskId, "Новый полигон", FileData(uploadedFilename = "new.xml", content = byteArrayOf()))
        developerService.attachTest(taskId, added.id)
        developerService.addTest(taskId, "Черновик", FileData(uploadedFilename = "draft.xml", content = byteArrayOf()))
        return taskId
    }

    private fun resourceTable(): Table = UI.getCurrent()._find<Table>()
        .single { table -> "Прикреплено" in table.element.textRecursively }

    private fun resourceRows(): List<TableRow> = resourceTable()._find<TableRow> { classes = "ts-row-clickable" }

    private fun resourceNames(): Set<String> = resourceRows().map { row -> row.element.getChild(0).textRecursively }.toSet()

    private fun filterPanel(): Div = UI.getCurrent()._get<Div> { classes = "ts-table-filters" }

    private fun differencesField(): Checkbox = filterPanel()._get<Checkbox>()

    @Suppress("UNCHECKED_CAST")
    private fun <T> chooseFilter(label: String, value: T) {
        val field = filterPanel()._find<Select<*>>().single { candidate -> candidate.ariaLabel.orElse(null) == label }
        checkNotNull(field as? Select<T>).value = value
    }

    private fun testingRequest(task: TaskVo): TaskValidationRequestVo = TaskValidationRequestVo(
        id = TaskValidationRequestId(1),
        createdAt = Instant.EPOCH,
        task = task.id,
        requestedBy = task.owner,
        snapshot = TaskValidationSnapshotVo(
            tests = emptyList(),
            developerSolutions = emptyList(),
            supportedTrikStudioVersions = emptyList(),
        ),
        execution = TaskValidationExecutionVo.PendingDiagnostics,
        isActive = true,
    )

    private fun testingPage(
        task: TaskVo,
        requests: List<TaskValidationRequestVo>,
        resources: List<ResourceVo> = emptyList(),
    ): DeveloperService {
        val service = mockk<DeveloperService>()
        every { service.viewTask(task.id) } returns (task to emptyList())
        every { service.viewResources() } returns resources
        resources.forEach { resource ->
            val versions = developerService.viewResource(task.id, resource.versionBucket)
            every { service.viewResource(task.id, resource.versionBucket) } returns versions
        }
        every { service.viewTrikStudioVersions() } returns developerService.viewTrikStudioVersions()
        every { service.viewTaskValidationRequests(task.id) } returns requests
        val view = DeveloperTaskView(buildUiTexts(), headers, service, multipleRoleUserService)
        val event = mockk<BeforeEnterEvent>()
        every { event.routeParameters } returns taskParameters(task.id)
        UI.getCurrent().removeAll()
        UI.getCurrent().add(view)
        view.beforeEnter(event)
        return service
    }

    @Suppress("UNCHECKED_CAST")
    private fun chooseGroupType(group: Div, kind: ResourceKind) {
        val select = group._find<Select<*>>().single { field -> field.ariaLabel.orElse(null) == "Тип" }
        checkNotNull(select as? Select<ResourceKind?>).value = kind
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
