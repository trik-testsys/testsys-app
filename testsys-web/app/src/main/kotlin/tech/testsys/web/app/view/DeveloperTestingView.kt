package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.task.AuthorSubmissionFailure
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TestDiagnosticResult
import tech.testsys.domain.model.task.TrikStudioVersion
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
import tech.testsys.web.components.texts.UiTexts
import java.time.format.DateTimeFormatter

/** Format of the creation moment in the testing page heading. */
private val MOMENT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

/**
 * Results of one testing request owned by the Developer (testsys.web.page.developer.testing).
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/tasks/:taskId([0-9]+)/testing/:testingId([0-9]+)")
@PageTitle("Тестирование задачи")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperTestingView(texts: UiTexts, private val headers: CabinetHeaders, private val developerService: DeveloperService) :
    TestSysView(texts),
    BeforeEnterObserver {
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
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Тестирование от ${request.createdAt.toServerDateTime().format(MOMENT_FORMAT)}") {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Разработчика", DeveloperView::class.java)
                crumb("Задача «${task.name}»", DeveloperTaskView::class.java, taskParameters(taskId))
            }
            row {
                block(title = "Сведения") {
                    row {
                        codeInput("Запущено", labelSize = 6, size = 18) {
                            value = request.createdAt.toServerDateTime().format(MOMENT_FORMAT)
                            isEditable = false
                        }
                    }
                    row {
                        val (label, tone) = validationStatusOf(execution)
                        field("Состояние", labelSize = 6, size = 18) { badge(label, tone) }
                    }
                    row {
                        textInput("Итог", labelSize = 6, size = 18) {
                            value = summaryOf(execution)
                            isEditable = false
                        }
                    }
                }
            }
            row {
                block(title = "Диагностики") {
                    val diagnostics = diagnosticsOf(execution)
                    table(key = { result: TestDiagnosticResult -> result.testId }, fetch = { query -> pageOf(diagnostics, query) }) {
                        textColumn("Полигон", size = 8) { result -> names[result.testId] ?: "#${result.testId.value}" }
                        column("Замечания") { result ->
                            vertical {
                                if (result.reports.isEmpty()) text("Без замечаний")
                                result.reports.forEach { report -> text(diagnosticText(report)) }
                            }
                        }
                        empty("Диагностик пока нет", "Результаты появятся после выполнения диагностик.")
                    }
                }
            }
            row { block(title = "Посылки") { submissionRows(request, names) } }
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
            textColumn("Авторское решение", size = 7) { (run, _) ->
                names[run.first.developerSolution] ?: "#${run.first.developerSolution.value}"
            }
            codeColumn("Версия TRIK Studio", size = 5) { (run, _) -> run.second.version }
            column("Статус", size = 7) { (_, submission) ->
                val failure = failures?.firstOrNull { failed -> failed.submission == submission }
                when {
                    execution is TaskValidationExecutionVo.CreatedSubmissions -> badge("Без результата", Tone.Neutral)
                    execution is TaskValidationExecutionVo.SubmissionsCreated -> badge("Проверяется", Tone.Info)
                    failure is AuthorSubmissionFailure.ScoreMismatch -> badge("Другой балл", Tone.Danger)
                    failure is AuthorSubmissionFailure.GradingFailed -> badge("Ошибка проверки", Tone.Danger)
                    else -> badge("Ожидаемый балл", Tone.Success)
                }
            }
            codeColumn("Балл") { (run, submission) ->
                val expected = run.first.expectedScore.value
                val actual = when (val failure = failures?.firstOrNull { failed -> failed.submission == submission }) {
                    is AuthorSubmissionFailure.ScoreMismatch -> failure.actualScore.toString()
                    is AuthorSubmissionFailure.GradingFailed -> "—"
                    null -> if (execution is TaskValidationExecutionVo.Completed) expected.toString() else "—"
                }
                "$actual / $expected"
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
