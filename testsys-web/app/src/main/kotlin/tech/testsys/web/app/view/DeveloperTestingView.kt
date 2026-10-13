package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.DiagnosticSeverity
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TestDiagnosticResult
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.localization.PageDeveloper.TestingSubmissionsStatusStatus
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.DeveloperSolutionInputVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.text
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.texts.currentLocalization

/**
 * Results of one testing request owned by the Developer (testsys.web.page.developer.testing).
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/tasks/:taskId([0-9]+)/testing/:testingId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperTestingView(private val headers: CabinetHeaders, private val developerService: DeveloperService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageDeveloper.testingTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val taskId = TaskId(event.routeParameters.getLong(TASK_ID_PARAMETER).orElseThrow())
        val requestId = TaskValidationRequestId(event.routeParameters.getLong(TESTING_ID_PARAMETER).orElseThrow())
        show(taskId, requestId)
    }

    private fun show(taskId: TaskId, requestId: TaskValidationRequestId) {
        val (task, _) = developerService.viewTask(taskId)
        val request = developerService.viewTaskValidationRequests(taskId).firstOrNull { request ->
            request.id == requestId && request.task == taskId
        } ?: throw NotFoundException("Testing request ${requestId.value} does not belong to task ${taskId.value}")
        val names = resourceNames(task, request)
        val execution = request.execution
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.testingTitle(startedAt = request.createdAt)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), DeveloperView::class.java)
                crumb(texts.taskTitle(name = task.name), DeveloperTaskView::class.java, taskParameters(taskId))
            }
            guide(texts.testingGuide())
            row {
                block(title = texts.details()) {
                    guide(texts.testingDetailsGuide())
                    row {
                        codeInput(texts.startedAt(), labelSize = 6, size = 18) {
                            value = texts.testingStartedAtValue(startedAt = request.createdAt)
                            isEditable = false
                        }
                    }
                    row {
                        val (label, tone) = validationStatusOf(execution)
                        field(texts.state(), labelSize = 6, size = 18) { badge(label, tone) }
                    }
                    row {
                        textInput(texts.summary(), labelSize = 6, size = 18) {
                            value = summaryOf(execution)
                            isEditable = false
                        }
                    }
                }
            }
            row {
                block(title = texts.testingDiagnosticsTitle()) {
                    guide(texts.testingDiagnosticsGuide())
                    val diagnostics = diagnosticsOf(execution)
                    table(key = { result: TestDiagnosticResult -> result.testId }, fetch = { query -> pageOf(diagnostics, query) }) {
                        textColumn(texts.testingDiagnosticsPolygon(), size = 8) { result ->
                            names[result.testId] ?: texts.testingUnknownResource(id = result.testId.value)
                        }
                        column(texts.testingDiagnosticsRemarks()) { result ->
                            // Info neither blocks author solutions nor needs action, so the page shows only Warning and Error.
                            val reports = result.reports.filter { report -> report.severity != DiagnosticSeverity.Info }
                            vertical {
                                if (reports.isEmpty()) text(texts.testingDiagnosticsNoRemarks())
                                reports.forEach { report -> text(diagnosticText(report)) }
                            }
                        }
                        empty(texts.testingDiagnosticsEmpty(), texts.testingDiagnosticsEmptyHint())
                    }
                }
            }
            row {
                block(title = localization.pageCabinet.sectionSubmissions()) {
                    guide(texts.testingSubmissionsGuide())
                    submissionRows(request, names)
                }
            }
        }
    }

    /** Resolves the versions recorded in the request, including versions absent from the current task revisions. */
    private fun resourceNames(task: TaskVo, request: TaskValidationRequestVo): Map<DomainId, String> {
        val ids = request.snapshot.tests + request.snapshot.developerSolutions.map { input -> input.developerSolution }
        val latest = developerService.viewResources().filter { resource -> resource.versionBucket in task.uploadedResources }
        val names = latest.associate { resource -> resource.id to resource.name }.toMutableMap()
        if (ids.any { id -> id !in names }) {
            latest.forEach { resource ->
                developerService.viewResource(task.id, resource.versionBucket).forEach { (version, _) ->
                    names[version.id] = version.name
                }
            }
        }

        return names
    }

    /** Adds a row for each submission of a developer solution in a TRIK Studio version, in the order they were created. */
    private fun BlockScope.submissionRows(request: TaskValidationRequestVo, names: Map<DomainId, String>) {
        val texts = currentLocalization().pageDeveloper
        val execution = request.execution
        val (submissions, failures) = when (execution) {
            is TaskValidationExecutionVo.SubmissionsCreated -> execution.submissions to null
            is TaskValidationExecutionVo.Completed -> execution.submissions to execution.failures
            is TaskValidationExecutionVo.CreatedSubmissions -> execution.submissions to null
            TaskValidationExecutionVo.PendingDiagnostics,
            is TaskValidationExecutionVo.AwaitingSubmissions,
            is TaskValidationExecutionVo.StoppedByDiagnostics,
            is TaskValidationExecutionVo.IncompleteDiagnostics,
            is TaskValidationExecutionVo.CompletedDiagnostics,
            -> emptyList<SubmissionId>() to null
        }
        // The same order as TaskValidationSnapshot.authorRuns: by developer solution, then by version.
        val versions = request.snapshot.supportedTrikStudioVersions.sortedBy { version -> version.version }
        val runs = request.snapshot.developerSolutions.sortedBy { input -> input.developerSolution.value }
            .flatMap { input -> versions.map { version -> input to version } }
        val rows = runs.zip(submissions)
        table(
            key = { row: Pair<Pair<DeveloperSolutionInputVo, TrikStudioVersion>, SubmissionId> -> row.second },
            fetch = { query -> pageOf(rows, query) },
        ) {
            textColumn(texts.testingSubmissionsDeveloperSolution(), size = 7) { (run, _) ->
                names[run.first.developerSolution] ?: texts.testingUnknownResource(id = run.first.developerSolution.value)
            }
            codeColumn(texts.trikStudioVersion(), size = 5) { (run, _) -> run.second.version }
            column(texts.status(), size = 7) { (_, submission) ->
                val failure = failures?.firstOrNull { failed -> failed.submission == submission }
                val (status, tone) = when {
                    execution is TaskValidationExecutionVo.CreatedSubmissions -> TestingSubmissionsStatusStatus.NO_RESULT to Tone.Neutral
                    execution is TaskValidationExecutionVo.SubmissionsCreated -> TestingSubmissionsStatusStatus.CHECKING to Tone.Info
                    failure is AuthorSubmissionFailure.ScoreMismatch -> TestingSubmissionsStatusStatus.SCORE_MISMATCH to Tone.Danger
                    failure is AuthorSubmissionFailure.GradingFailed -> TestingSubmissionsStatusStatus.GRADING_FAILED to Tone.Danger
                    else -> TestingSubmissionsStatusStatus.EXPECTED_SCORE to Tone.Success
                }
                badge(texts.testingSubmissionsStatus(status = status), tone)
            }
            codeColumn(texts.score()) { (run, submission) ->
                val expected = run.first.expectedScore.value
                when (val failure = failures?.firstOrNull { failed -> failed.submission == submission }) {
                    is AuthorSubmissionFailure.ScoreMismatch ->
                        texts.testingSubmissionsScore(actual = failure.actualScore, expected = expected)
                    is AuthorSubmissionFailure.GradingFailed -> texts.testingSubmissionsScorePending(expected = expected)
                    null -> if (execution is TaskValidationExecutionVo.Completed) {
                        texts.testingSubmissionsScore(actual = expected, expected = expected)
                    } else {
                        texts.testingSubmissionsScorePending(expected = expected)
                    }
                }
            }
        }
    }
}

/** Returns the diagnostic results of [execution], none before diagnostics finish. */
private fun diagnosticsOf(execution: TaskValidationExecutionVo): List<TestDiagnosticResult> = when (execution) {
    TaskValidationExecutionVo.PendingDiagnostics, is TaskValidationExecutionVo.IncompleteDiagnostics -> emptyList()
    is TaskValidationExecutionVo.AwaitingSubmissions -> execution.diagnostics
    is TaskValidationExecutionVo.StoppedByDiagnostics -> execution.diagnostics
    is TaskValidationExecutionVo.SubmissionsCreated -> execution.diagnostics
    is TaskValidationExecutionVo.Completed -> execution.diagnostics
    is TaskValidationExecutionVo.CompletedDiagnostics -> execution.diagnostics
    is TaskValidationExecutionVo.CreatedSubmissions -> execution.diagnostics
}
