package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.RouteParameters
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DiagnosticData
import tech.testsys.domain.model.task.DiagnosticReport
import tech.testsys.domain.model.task.DiagnosticSeverity
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.TaskAlreadyCommittedError
import tech.testsys.operation.error.TaskNotTestedError
import tech.testsys.operation.error.TaskTestingNoDeveloperSolutionsError
import tech.testsys.operation.error.TaskTestingNoExerciseForLanguageError
import tech.testsys.operation.error.TaskTestingNoPolygonsError
import tech.testsys.operation.error.TaskTestingNoStatementError
import tech.testsys.operation.error.TaskTestingNoTrikStudioVersionsError
import tech.testsys.operation.error.TaskTrikStudioVersionNotSupportedError
import tech.testsys.web.app.service.TaskRevisionVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperSolutionVo
import tech.testsys.web.app.service.developer.ExerciseVo
import tech.testsys.web.app.service.developer.ResourceVo
import tech.testsys.web.app.service.developer.StatementVo
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TestVo
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.forms.UploadedFile
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Route parameter of [DeveloperView] with the open tab. */
internal const val DEVELOPER_SECTION_PARAMETER = "section"

/** Route parameter of [DeveloperTaskView] and [DeveloperResourceView] with the task id. */
internal const val TASK_ID_PARAMETER = "taskId"

/** Route parameter of [DeveloperResourceView] with the version chain of the resource. */
internal const val RESOURCE_ID_PARAMETER = "resourceId"

/** Route parameter of [DeveloperContestView] with the contest id. */
internal const val CONTEST_ID_PARAMETER = "contestId"

/** Value of [DEVELOPER_SECTION_PARAMETER] of the tasks tab. */
internal const val TASKS_SECTION = "tasks"

/** Value of [DEVELOPER_SECTION_PARAMETER] of the contests tab. */
internal const val CONTESTS_SECTION = "contests"

/** Largest file of a resource version a page receives: 10 MiB. */
internal const val MAX_RESOURCE_BYTES = 10 * 1024 * 1024

/** Languages of exercises and developer solutions with their labels. */
internal val LANGUAGE_LABELS: Map<TrikSupportedLanguage, String> = mapOf(
    TrikSupportedLanguage.Python to "Python",
    TrikSupportedLanguage.JavaScript to "JavaScript",
    TrikSupportedLanguage.VisualLanguage to "Визуальный язык",
)

/**
 * State of a task as a page shows it.
 *
 * @property label the label of the state.
 * @property tone the tone of its badge.
 */
internal enum class TaskState(val label: String, val tone: Tone) {
    /** Never committed. */
    New("Новая", Tone.Info),

    /** Committed with changes in the working revision. */
    Uncommitted("Не зафиксирована", Tone.Warning),

    /** Committed without changes. */
    Committed("Зафиксирована", Tone.Success),
}

/** Copies this temporary upload into a file the page keeps after the upload handler returns. */
internal fun UploadedFile.toFileData(): FileData =
    FileData(uploadedFilename = filename, content = openStream().use { stream -> stream.readBytes() })

/** Returns this date and time in the time zone of the server as a moment. */
internal fun LocalDateTime.toServerInstant(): Instant = atZone(ZoneId.systemDefault()).toInstant()

/** Requires a start before the end of a contest given by this field; an empty end needs no start. */
internal fun <T> Binder.BindingBuilder<T, LocalDateTime?>.scheduleRules(
    start: () -> LocalDateTime?,
): Binder.BindingBuilder<T, LocalDateTime?> = withValidator({ end -> end == null || start() != null }, "Укажите начало тура")
    .withValidator({ end -> start()?.let { begin -> end == null || end > begin } != false }, "Конец должен быть позже начала")

/** Returns the state of [task]: without a working revision it is committed, without a committed one it is new. */
internal fun stateOf(task: TaskVo): TaskState = when {
    task.wip == null -> TaskState.Committed
    task.lastCommitted == null -> TaskState.New
    else -> TaskState.Uncommitted
}

/** Returns the identifiers of the resources of [revision], none without one. */
internal fun idsOf(revision: TaskRevisionVo?): Set<DomainId> = revision?.let { content ->
    (content.tests + content.exercises + listOfNotNull(content.statement) + content.developerSolutions).toSet()
}.orEmpty()

/** Returns the route parameters of a section of [DeveloperView]. */
internal fun developerSectionParameters(section: String): RouteParameters = RouteParameters(DEVELOPER_SECTION_PARAMETER, section)

/** Returns the route parameters of the page of [taskId]. */
internal fun taskParameters(taskId: TaskId): RouteParameters = RouteParameters(TASK_ID_PARAMETER, taskId.value.toString())

/** Returns the route parameters of the page of the resource chain [versionBucket] of [taskId]. */
internal fun resourceParameters(taskId: TaskId, versionBucket: VersionBucket): RouteParameters = RouteParameters(
    mapOf(TASK_ID_PARAMETER to taskId.value.toString(), RESOURCE_ID_PARAMETER to versionBucket.value.toString()),
)

/** Returns the route parameters of the page of [contestId]. */
internal fun contestParameters(contestId: ContestId): RouteParameters = RouteParameters(CONTEST_ID_PARAMETER, contestId.value.toString())

/** Opens the page of the task [taskId]. */
internal fun openDeveloperTask(taskId: TaskId) {
    UI.getCurrent().navigate(DeveloperTaskView::class.java, taskParameters(taskId))
}

/** Opens the page of the resource chain [versionBucket] of [taskId]. */
internal fun openDeveloperResource(taskId: TaskId, versionBucket: VersionBucket) {
    UI.getCurrent().navigate(DeveloperResourceView::class.java, resourceParameters(taskId, versionBucket))
}

/** Opens the page of the contest [contestId]. */
internal fun openDeveloperContest(contestId: ContestId) {
    UI.getCurrent().navigate(DeveloperContestView::class.java, contestParameters(contestId))
}

/** Returns the label of the type of [resource]. */
internal fun typeOf(resource: ResourceVo): String = when (resource) {
    is StatementVo -> "Условие"
    is ExerciseVo -> "Упражнение"
    is TestVo -> "Полигон"
    is DeveloperSolutionVo -> "Авторское решение"
}

/** Returns the text of a diagnostic [report]: its severity, place and finding. */
internal fun diagnosticText(report: DiagnosticReport): String {
    val severity = when (report.severity) {
        DiagnosticSeverity.Info -> "Сведения"
        DiagnosticSeverity.Warning -> "Предупреждение"
        DiagnosticSeverity.Error -> "Ошибка"
    }
    val place = report.location?.let { location ->
        val path = location.path.joinToString("/") { segment -> "${segment.tag}[${segment.index}]" }
        location.attribute?.let { attribute -> "$path@$attribute" } ?: path
    }
    val finding = when (val data = report.data) {
        is DiagnosticData.UnknownElement -> "неизвестный элемент «${data.tag}»"
        is DiagnosticData.MalformedXml -> "некорректный XML: ${data.details}"
        is DiagnosticData.MissingChild -> "нет вложенного элемента «${data.tag}»"
        is DiagnosticData.MissingAttribute -> "нет атрибута «${data.attribute}»"
        is DiagnosticData.InvalidAttributeValue ->
            "атрибут «${data.attribute}» равен «${data.actual}», ожидается ${data.expected}"
        is DiagnosticData.InvalidChildCount -> "вложенных элементов ${data.actual}, ожидается ${data.expected}"
        DiagnosticData.MissingTimeLimit -> "не задано ограничение времени"
        is DiagnosticData.MultipleTimeLimits -> "ограничений времени ${data.count}, ожидается одно"
        is DiagnosticData.NegativeTimeLimit -> "отрицательное ограничение времени ${data.value}"
        is DiagnosticData.ExcessiveTimeLimit -> "ограничение времени ${data.value} больше допустимого ${data.maximum}"
        is DiagnosticData.InvalidEventId -> "некорректный идентификатор события «${data.id}»"
        DiagnosticData.MissingScoreOutput -> "полигон не выводит балл"
    }
    return listOfNotNull(severity, place, finding).joinToString(" · ")
}

/** Returns why testing or committing a task was refused, or `null` for a refusal the common handler shows. */
internal fun testingRefusalOf(error: OperationError): String? = when (error) {
    is TaskAlreadyCommittedError -> "У задачи нет незафиксированных изменений."
    is TaskTestingNoStatementError -> "Прикрепите условие."
    is TaskTestingNoPolygonsError -> "Прикрепите хотя бы один полигон."
    is TaskTestingNoDeveloperSolutionsError -> "Прикрепите хотя бы одно авторское решение."
    is TaskTestingNoTrikStudioVersionsError -> "Укажите поддерживаемые версии TRIK Studio."
    is TaskTestingNoExerciseForLanguageError -> "Прикрепите упражнение для языка «${LANGUAGE_LABELS.getValue(error.language)}»."
    is TaskTrikStudioVersionNotSupportedError ->
        "Задача прикреплена к туру с версией TRIK Studio ${error.trikStudioVersion.version}, которую она не поддерживает."
    is TaskNotTestedError -> "Рабочая версия задачи не прошла тестирование."
    else -> null
}

/** Route parameter of the selected testing request. */
internal const val TESTING_ID_PARAMETER = "testingId"

/** Returns the parameters of a testing request belonging to [taskId]. */
internal fun testingParameters(taskId: TaskId, requestId: TaskValidationRequestId): RouteParameters = RouteParameters(
    mapOf(TASK_ID_PARAMETER to taskId.value.toString(), TESTING_ID_PARAMETER to requestId.value.toString()),
)

/** Opens the results of [requestId] of [taskId]. */
internal fun openDeveloperTesting(taskId: TaskId, requestId: TaskValidationRequestId) {
    UI.getCurrent().navigate(DeveloperTestingView::class.java, testingParameters(taskId, requestId))
}

/** Returns a short result of [execution]. */
internal fun summaryOf(execution: TaskValidationExecutionVo): String = when (execution) {
    TaskValidationExecutionVo.PendingDiagnostics -> "Выполняются диагностики"
    is TaskValidationExecutionVo.AwaitingSubmissions -> "Диагностики пройдены, создаются посылки"
    is TaskValidationExecutionVo.StoppedByDiagnostics -> "Диагностики нашли ошибки в полигонах"
    is TaskValidationExecutionVo.SubmissionsCreated -> "Проверяются посылки: ${execution.submissions.size}"
    is TaskValidationExecutionVo.Completed -> if (execution.failures.isEmpty()) {
        "Все посылки набрали ожидаемый балл"
    } else {
        "Провалено посылок: ${execution.failures.size} из ${execution.submissions.size}"
    }
    is TaskValidationExecutionVo.IncompleteDiagnostics -> "Техническая остановка: ${execution.failure.description}"
    is TaskValidationExecutionVo.CompletedDiagnostics -> "Техническая остановка: ${execution.failure.description}"
    is TaskValidationExecutionVo.CreatedSubmissions -> "Техническая остановка: ${execution.failure.description}"
}
