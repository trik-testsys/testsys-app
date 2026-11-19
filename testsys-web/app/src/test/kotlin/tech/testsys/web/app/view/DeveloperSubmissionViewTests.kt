package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.html.TableBody
import com.vaadin.flow.component.html.TableDataCell
import com.vaadin.flow.component.html.TableRow
import com.vaadin.flow.component.textfield.TextArea
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.RouterLink
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.OperationErrorView
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.DeveloperSolutionInputVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.app.service.developer.TaskValidationSnapshotVo
import tech.testsys.web.app.service.developer.TaskValidationSubmissionVo
import tech.testsys.web.app.service.judge.TestVerdictVo
import tech.testsys.web.app.service.judge.VerdictVo
import tech.testsys.web.app.service.study.GradingResultVo
import tech.testsys.web.app.service.study.SubmissionKindVo
import tech.testsys.web.app.service.study.SubmissionStatusVo
import tech.testsys.web.app.service.study.SubmissionVo
import java.time.Instant
import javax.sql.DataSource

@SpringBootTest
class DeveloperSubmissionViewTests : MockSpringVaadinTests() {
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

    private val submissionId = SubmissionId(1)

    private val version = TrikStudioVersion(tag = "trikset/trik-studio:image", version = "2026.1")

    @Test
    fun `should title the browser tab and head the page with the submission id`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(submissionOf(task, success(), verdict(task)))

        val view = UI.getCurrent()._get<DeveloperSubmissionView>()
        assertEquals("Посылка", view.pageTitle)
        assertEquals("Посылка №1", view._get<H1>().text)
        val currentCrumb = view._find<Span>().single { span -> span.element.getAttribute("aria-current") == "page" }
        assertEquals("Посылка №1", currentCrumb.text)
    }

    @Test
    fun `should return from the submission to its testing results through breadcrumb`() {
        developers.signInDeveloper()
        val task = developers.task()
        val (request, submission) = developers.testedSubmission(task)
        UI.getCurrent().navigate(DeveloperSubmissionView::class.java, submissionParameters(task.id, request.id, submission))

        val crumb = UI.getCurrent()._find<RouterLink>().single { link -> link.text.startsWith("Тестирование от") }
        UI.getCurrent().navigate(crumb.href)

        assertEquals("developer/tasks/${task.id.value}/testing/${request.id.value}", currentPath())
    }

    @Test
    fun `should show the developer solution, version, status and scores without a reason after a successful grading`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(submissionOf(task, success(), verdict(task)))

        assertEquals("#1", textField("Авторское Решение").value)
        assertEquals("2026.1", textField("Версия TRIK Studio").value)
        assertEquals("Ожидаемый балл", status())
        assertEquals("75", textField("Балл").value)
        assertEquals("75", textField("Ожидаемый балл").value)
        assertTrue(UI.getCurrent()._find<TextArea>().isEmpty())
    }

    @Test
    fun `should show the description of the grading error as the reason`() {
        developers.signInDeveloper()
        val task = developers.task()
        val failed = SubmissionStatusVo.Graded(GradingResultVo.GradingError("Грейдер упал"))

        open(submissionOf(task, failed, verdict = null, failures = listOf(AuthorSubmissionFailure.GradingFailed(submissionId))))

        assertEquals("Ошибка проверки", status())
        assertEquals("—", textField("Балл").value)
        assertEquals("Грейдер упал", reason())
    }

    @Test
    fun `should show the timeout message as the reason`() {
        developers.signInDeveloper()
        val task = developers.task()
        val timedOut = SubmissionStatusVo.Graded(GradingResultVo.Timeout)

        open(submissionOf(task, timedOut, verdict = null, failures = listOf(AuthorSubmissionFailure.GradingFailed(submissionId))))

        assertEquals("Проверка не завершилась за отведённое время.", reason())
    }

    @Test
    fun `should show the status of a solution that does not start with the node description as the reason`() {
        developers.signInDeveloper()
        val task = developers.task()
        val invalid = SubmissionStatusVo.Graded(GradingResultVo.InvalidSubmission("Нет функции main"))

        open(submissionOf(task, invalid, verdict = null, failures = listOf(AuthorSubmissionFailure.GradingFailed(submissionId))))

        assertEquals("Решение не запускается", status())
        assertEquals("Нет функции main", reason())
    }

    @Test
    fun `should show no reason for a score mismatch`() {
        developers.signInDeveloper()
        val task = developers.task()
        val mismatch = AuthorSubmissionFailure.ScoreMismatch(submissionId, actualScore = 70)

        open(submissionOf(task, success(), verdict(task, score = 70), failures = listOf(mismatch)))

        assertEquals("Другой балл", status())
        assertEquals("70", textField("Балл").value)
        assertTrue(UI.getCurrent()._find<TextArea>().isEmpty())
    }

    @Test
    fun `should show the result of a graded submission while its request is still being checked`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(submissionOf(task, success(), verdict(task), execution = inProgress()))

        assertEquals("Ожидаемый балл", status())
    }

    @Test
    fun `should show the score of each polygon with the logs and only an existing recording`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(submissionOf(task, success(), verdict(task)))

        val first = verdictCells("#10")
        val second = verdictCells("#11")
        assertEquals(listOf("#10", "75"), first.take(2).map { cell -> cell.element.textRecursively })
        assertEquals(listOf(1, 1), first.drop(2).map { cell -> cell._find<NativeButton>().size })
        assertEquals(listOf("#11", "0"), second.take(2).map { cell -> cell.element.textRecursively })
        assertEquals(listOf(1, 0), second.drop(2).map { cell -> cell._find<NativeButton>().size })
    }

    @Test
    fun `should show an empty verdict without a successful verdict`() {
        developers.signInDeveloper()
        val task = developers.task()

        open(submissionOf(task, SubmissionStatusVo.InProgress, verdict = null, execution = inProgress()))

        val emptyTitles = UI.getCurrent()._find<Span> { classes = "ts-empty__title" }.map { title -> title.text }
        assertTrue("Вердикта нет" in emptyTitles, emptyTitles.toString())
        assertEquals("Проверяется", status())
    }

    @Test
    fun `should show not found for a submission of another request`() {
        developers.signInDeveloper()
        val task = developers.task()
        val (request, _) = developers.testedSubmission(task)
        val (_, other) = developers.testedSubmission(task)

        UI.getCurrent().navigate(DeveloperSubmissionView::class.java, submissionParameters(task.id, request.id, other))

        assertEquals(OperationErrorView::class.java, currentView)
        assertEquals("Страница не найдена", UI.getCurrent()._get<H1>().text)
    }

    @Test
    fun `should refuse another developer before showing the submission`() {
        developers.signInDeveloper()
        val task = developers.task()
        val (request, submission) = developers.testedSubmission(task)
        developers.signInDeveloper()

        UI.getCurrent().navigate(DeveloperSubmissionView::class.java, submissionParameters(task.id, request.id, submission))

        assertEquals(OperationErrorView::class.java, currentView)
    }

    private fun success(): SubmissionStatusVo = SubmissionStatusVo.Graded(GradingResultVo.Success(VerdictId(4)))

    private fun inProgress(): TaskValidationExecutionVo = TaskValidationExecutionVo.SubmissionsCreated(emptyList(), listOf(submissionId))

    /** Returns verdict 4 of [task]: polygon 10 with [score] and a recording, polygon 11 with no score and no recording. */
    private fun verdict(task: TaskVo, score: Int = 75): VerdictVo = VerdictVo(
        id = VerdictId(4),
        createdAt = Instant.EPOCH,
        task = task.id,
        submission = submissionId,
        testVerdicts = listOf(
            TestVerdictVo(score = Score(score), test = TestId(10), logs = LogsId(30), recording = RecordingId(40)),
            TestVerdictVo(score = Score(0), test = TestId(11), logs = LogsId(31), recording = null),
        ),
    )

    /** Returns submission 1 of [task] in a request of developer solution 1 expected to score 75 in one version. */
    private fun submissionOf(
        task: TaskVo,
        status: SubmissionStatusVo,
        verdict: VerdictVo?,
        failures: List<AuthorSubmissionFailure> = emptyList(),
        execution: TaskValidationExecutionVo =
            TaskValidationExecutionVo.Completed(emptyList(), listOf(submissionId), failures, Instant.EPOCH),
    ): TaskValidationSubmissionVo = TaskValidationSubmissionVo(
        task = task,
        request = TaskValidationRequestVo(
            id = TaskValidationRequestId(2),
            createdAt = Instant.EPOCH,
            task = task.id,
            requestedBy = task.owner,
            snapshot = TaskValidationSnapshotVo(
                tests = listOf(TestId(10), TestId(11)),
                developerSolutions = listOf(
                    DeveloperSolutionInputVo(
                        developerSolution = DeveloperSolutionId(1),
                        solution = SolutionId(3),
                        expectedScore = Score(75),
                    ),
                ),
                supportedTrikStudioVersions = listOf(version),
            ),
            execution = execution,
            isActive = false,
        ),
        submission = SubmissionVo(
            id = submissionId,
            createdAt = Instant.EPOCH,
            author = task.owner,
            solution = SolutionId(3),
            task = task.id,
            status = status,
            kind = SubmissionKindVo.DeveloperSolutionTest(version),
            judgmentOrders = emptyList(),
        ),
        verdict = verdict,
    )

    private fun open(submission: TaskValidationSubmissionVo) {
        val service = mockk<DeveloperService>()
        val request = submission.request
        every { service.checkTaskValidationSubmissionAccess(request.task, request.id, submissionId) } returns submission
        every { service.viewResources() } returns emptyList()
        val view = DeveloperSubmissionView(headers, service, fileContentReader)
        val event = mockk<BeforeEnterEvent>()
        every { event.routeParameters } returns submissionParameters(request.task, request.id, submissionId)
        UI.getCurrent().removeAll()
        UI.getCurrent().add(view)
        view.beforeEnter(event)
    }

    private fun reason(): String = UI.getCurrent()._find<TextArea>().single { area -> area.ariaLabel.orElse(null) == "Причина" }.value

    private fun status(): String = UI.getCurrent()._find<Span> { classes = "ts-status" }.single().element.textRecursively

    /** Returns the cells of the verdict row of [polygon]: polygon, score, logs and recording. */
    private fun verdictCells(polygon: String): List<TableDataCell> = UI.getCurrent()._find<TableBody>()
        .flatMap { body -> body._find<TableRow>() }
        .map { row -> row._find<TableDataCell>() }
        .single { cells -> cells.firstOrNull()?.element?.textRecursively == polygon }
}
