package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
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
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.DeveloperSolutionInputVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TaskValidationRequestVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.texts.currentLocalization

/**
 * Results of one testing request owned by the Developer (testsys.web.page.developer.testing).
 *
 * @since 1.0.0-beta.1
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
        // Checks access before the blocks load, so that a refusal or a request of another task opens its error screen.
        val (task, request) = developerService.checkTaskValidationRequestAccess(taskId, requestId)
        val loaded = SharedFetch {
            val current = developerService.checkTaskValidationRequestAccess(taskId, requestId).second
            current to resourceNames(developerService, task, current)
        }
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.testingTitle(startedAt = request.createdAt)) {
                crumb(texts.title(), DeveloperView::class.java)
                crumb(texts.taskTitle(name = task.name), DeveloperTaskView::class.java, taskParameters(taskId))
            }
            guide(texts.testingGuide())
            row {
                block(title = texts.details()) {
                    guide(texts.testingDetailsGuide())
                    val details = { developerService.checkTaskValidationRequestAccess(taskId, requestId).second }
                    load(details) { current -> detailsRows(current) }
                }
            }
            row {
                block(title = texts.testingDiagnosticsTitle(), size = 12) {
                    guide(texts.testingDiagnosticsGuide())
                    load(loaded.forLoad()) { (current, known) -> diagnosticsTable(current, known) }
                }
                block(title = localization.pageCabinet.sectionSubmissions(), size = 12) {
                    guide(texts.testingSubmissionsGuide())
                    load(loaded.forLoad()) { (current, known) -> submissionRows(current, known) }
                }
            }
        }
    }

    private fun BlockScope.detailsRows(request: TaskValidationRequestVo) {
        val texts = currentLocalization().pageDeveloper
        row {
            codeInput(texts.startedAt(), labelSize = 2, size = 4) {
                value = texts.testingStartedAtValue(startedAt = request.createdAt)
                isEditable = false
            }
            val (label, tone) = validationStatusOf(request.execution)
            field(texts.state(), labelSize = 2, size = 4) { badge(label, tone) }
        }
        row {
            textInput(texts.summary(), labelSize = 2, size = 22) {
                value = summaryOf(request.execution)
                isEditable = false
            }
        }
    }

    private fun BlockScope.diagnosticsTable(request: TaskValidationRequestVo, names: Map<DomainId, String>) {
        val texts = currentLocalization().pageDeveloper
        val diagnostics = diagnosticsOf(request.execution)
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

    /** Adds a row for each submission of a developer solution in a TRIK Studio version, in the order they were created. */
    private fun BlockScope.submissionRows(request: TaskValidationRequestVo, names: Map<DomainId, String>) {
        val texts = currentLocalization().pageDeveloper
        val execution = request.execution
        val rows = submissionRunsOf(request)
        table(
            key = { row: Pair<Pair<DeveloperSolutionInputVo, TrikStudioVersion>, SubmissionId> -> row.second },
            fetch = { query -> pageOf(rows, query) },
        ) {
            textColumn(texts.testingSubmissionsDeveloperSolution(), size = 7) { (run, _) ->
                names[run.first.developerSolution] ?: texts.testingUnknownResource(id = run.first.developerSolution.value)
            }
            codeColumn(texts.trikStudioVersion(), size = 5) { (run, _) -> run.second.version }
            column(texts.status(), size = 7) { (_, submission) ->
                val (status, tone) = submissionStatusOf(execution, submission)
                badge(texts.testingSubmissionsStatus(status = status), tone)
            }
            codeColumn(texts.score()) { (run, submission) ->
                val expected = run.first.expectedScore.value
                when (val failure = failureOf(execution, submission)) {
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
            onRowClick(isNavigation = true) { (_, submission) -> openDeveloperSubmission(request.task, request.id, submission) }
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
