package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.RouterLink
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.testDiagnosticResult
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TaskValidationTechnicalFailure
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.NotFoundView
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.DeveloperSolutionInputVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.app.service.developer.TaskValidationSnapshotVo
import tech.testsys.web.components.texts.buildUiTexts
import java.time.Instant
import javax.sql.DataSource

@SpringBootTest
class DeveloperTestingViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var developerService: DeveloperService

    @Autowired
    private lateinit var tasks: TaskRepository

    @Autowired
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var headers: CabinetHeaders

    private val developers by lazy { DeveloperFixtures(fixtures, developerService, tasks, dataSource) }

    @Test
    fun `should return from testing results to its task through breadcrumb`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = developers.testing(task)
        UI.getCurrent().navigate(DeveloperTestingView::class.java, testingParameters(task.id, request.id))

        UI.getCurrent().navigate(UI.getCurrent()._get<RouterLink> { text = "Задача «${task.name}»" }.href)

        assertEquals("developer/tasks/${task.id.value}", currentPath())
    }

    @Test
    fun `should show not found for a request of another task`() {
        developers.signInDeveloper()
        val first = developers.task()
        val request = developers.testing(first)
        val second = developers.task()

        UI.getCurrent().navigate(DeveloperTestingView::class.java, testingParameters(second.id, request.id))

        assertEquals(NotFoundView::class.java, currentView)
    }

    @Test
    fun `should refuse another developer before showing request results`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = developers.testing(task)
        developers.signInDeveloper()

        UI.getCurrent().navigate(DeveloperTestingView::class.java, testingParameters(task.id, request.id))

        assertEquals(OperationErrorView::class.java, currentView)
    }

    @Test
    fun `should load current request state when opening the page again`() {
        developers.signInDeveloper()
        val task = developers.task()
        val request = testingRequest(task)
        val service = service(task, listOf(request))
        open(task.id, request.id, service)
        every { service.viewTaskValidationRequests(task.id) } returns listOf(
            request.copy(execution = TaskValidationExecutionVo.Completed(emptyList(), emptyList(), emptyList(), Instant.EPOCH)),
        )

        open(task.id, request.id, service)

        assertEquals("Все посылки набрали ожидаемый балл", textField("Итог").value)
        assertTrue(UI.getCurrent()._find<Button> { text = "К задаче" }.isEmpty())
        assertTrue(UI.getCurrent()._find<Button> { text = "Обновить" }.isEmpty())
    }

    @Test
    fun `should use historical resource names from the selected request snapshot`() {
        developers.signInDeveloper()
        val draft = developers.task()
        val original = developerService.addTest(draft.id, "Прежний полигон", FileData("world.xml", "<world/>".toByteArray()))
        developerService.updateTest(draft.id, original.id, "Новый полигон", FileData("world2.xml", "<world/>".toByteArray()))
        val task = developerService.viewTask(draft.id).first
        val request = testingRequest(task).copy(
            snapshot = TaskValidationSnapshotVo(listOf(original.id), emptyList(), emptyList()),
            execution = TaskValidationExecutionVo.StoppedByDiagnostics(
                listOf(testDiagnosticResult { testId = original.id }),
                Instant.EPOCH,
            ),
        )
        val service = service(task, listOf(request))
        every { service.viewResources() } returns developerService.viewResources()
        val history = developerService.viewResource(task.id, original.versionBucket)
        every { service.viewResource(task.id, original.versionBucket) } returns history

        open(task.id, request.id, service)

        val diagnostics = tableText("Полигон")
        assertTrue("Прежний полигон" in diagnostics)
        assertFalse("Новый полигон" in diagnostics)
        assertTrue("Без замечаний" in diagnostics)
    }

    @Test
    fun `should replace previous request data when navigating on the same view`() {
        developers.signInDeveloper()
        val task = developers.task()
        val first = developers.testing(task)
        val second = developers.testing(task)
        UI.getCurrent().navigate(DeveloperTestingView::class.java, testingParameters(task.id, first.id))
        val view = UI.getCurrent()._get<DeveloperTestingView>()

        UI.getCurrent().navigate(DeveloperTestingView::class.java, testingParameters(task.id, second.id))

        assertEquals(view, UI.getCurrent()._get<DeveloperTestingView>())
        assertEquals("developer/tasks/${task.id.value}/testing/${second.id.value}", currentPath())
        assertEquals(2, UI.getCurrent()._find<Table>().size)
    }

    @ParameterizedTest
    @MethodSource("submissionStates")
    fun `should show submission status and actual versus expected score`(
        execution: TaskValidationExecutionVo,
        status: String,
        score: String,
    ) {
        developers.signInDeveloper()
        val task = developers.task()
        val request = testingRequest(task).copy(
            snapshot = TaskValidationSnapshotVo(
                tests = emptyList(),
                developerSolutions = listOf(
                    DeveloperSolutionInputVo(
                        developerSolution = DeveloperSolutionId(1),
                        solution = SolutionId(1),
                        expectedScore = Score(10),
                    ),
                ),
                supportedTrikStudioVersions = listOf(TrikStudioVersion("2026.1")),
            ),
            execution = execution,
        )
        val service = service(task, listOf(request))

        open(task.id, request.id, service)

        val submissions = UI.getCurrent()._find<Table>().single { table -> "Авторское решение" in table.element.textRecursively }
        assertTrue(status in submissions.element.textRecursively)
        assertTrue(score in submissions.element.textRecursively)
        assertTrue("2026.1" in submissions.element.textRecursively)
    }

    private fun testingRequest(task: TaskVo): TaskValidationRequestVo = TaskValidationRequestVo(
        id = TaskValidationRequestId(1),
        createdAt = Instant.EPOCH,
        task = task.id,
        requestedBy = task.owner,
        snapshot = TaskValidationSnapshotVo(emptyList(), emptyList(), emptyList()),
        execution = TaskValidationExecutionVo.PendingDiagnostics,
        isActive = true,
    )

    private fun service(task: TaskVo, requests: List<TaskValidationRequestVo>): DeveloperService {
        val service = mockk<DeveloperService>()
        every { service.viewTask(task.id) } returns (task to emptyList())
        every { service.viewResources() } returns emptyList()
        every { service.viewTaskValidationRequests(task.id) } returns requests
        return service
    }

    private fun open(taskId: TaskId, requestId: TaskValidationRequestId, service: DeveloperService) {
        val view = DeveloperTestingView(buildUiTexts(), headers, service)
        val event = mockk<BeforeEnterEvent>()
        every { event.routeParameters } returns testingParameters(taskId, requestId)
        UI.getCurrent().removeAll()
        UI.getCurrent().add(view)
        view.beforeEnter(event)
    }

    companion object {
        @JvmStatic
        fun submissionStates(): List<Arguments> {
            val submissions = listOf(SubmissionId(1))
            return listOf(
                Arguments.of(TaskValidationExecutionVo.SubmissionsCreated(emptyList(), submissions), "Проверяется", "— / 10"),
                Arguments.of(
                    TaskValidationExecutionVo.CreatedSubmissions(
                        emptyList(),
                        submissions,
                        TaskValidationTechnicalFailure(description = "Stopped", occurredAt = Instant.EPOCH),
                    ),
                    "Без результата",
                    "— / 10",
                ),
                Arguments.of(
                    TaskValidationExecutionVo.Completed(emptyList(), submissions, emptyList(), Instant.EPOCH),
                    "Ожидаемый балл",
                    "10 / 10",
                ),
                Arguments.of(
                    TaskValidationExecutionVo.Completed(
                        emptyList(),
                        submissions,
                        listOf(AuthorSubmissionFailure.ScoreMismatch(SubmissionId(1), actualScore = 5)),
                        Instant.EPOCH,
                    ),
                    "Другой балл",
                    "5 / 10",
                ),
                Arguments.of(
                    TaskValidationExecutionVo.Completed(
                        emptyList(),
                        submissions,
                        listOf(AuthorSubmissionFailure.GradingFailed(SubmissionId(1))),
                        Instant.EPOCH,
                    ),
                    "Ошибка проверки",
                    "— / 10",
                ),
            )
        }
    }
}
