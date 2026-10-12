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
import tech.testsys.infra.localization.PageDeveloper.DiagnosticLineAtSeverity
import tech.testsys.infra.localization.PageDeveloper.DiagnosticLineSeverity
import tech.testsys.infra.localization.PageDeveloper.RefusalNoExerciseLanguage
import tech.testsys.infra.localization.PageDeveloper.TaskStateState
import tech.testsys.infra.localization.PageJudge.SolutionLanguageLanguage
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
import tech.testsys.web.components.texts.currentLocalization
import tech.testsys.web.components.texts.currentTimeZone
import java.time.Instant
import java.time.LocalDateTime

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

/** Languages of exercises and developer solutions in the order a page offers them. */
internal val LANGUAGES: List<TrikSupportedLanguage> =
    listOf(TrikSupportedLanguage.Python, TrikSupportedLanguage.JavaScript, TrikSupportedLanguage.VisualLanguage)

/** Returns the label of the programming [language] of an exercise or a developer solution. */
internal fun languageLabel(language: TrikSupportedLanguage): String = currentLocalization().pageJudge.solutionLanguage(
    language = when (language) {
        TrikSupportedLanguage.Python -> SolutionLanguageLanguage.PYTHON
        TrikSupportedLanguage.JavaScript -> SolutionLanguageLanguage.JAVASCRIPT
        TrikSupportedLanguage.VisualLanguage -> SolutionLanguageLanguage.VISUAL_LANGUAGE
    },
)

/**
 * State of a task as a page shows it.
 *
 * @property variant the message variant naming the state.
 * @property tone the tone of its badge.
 */
internal enum class TaskState(val variant: TaskStateState, val tone: Tone) {
    /** Never committed. */
    New(TaskStateState.NEW, Tone.Info),

    /** Committed with changes in the working revision. */
    Uncommitted(TaskStateState.UNCOMMITTED, Tone.Warning),

    /** Committed without changes. */
    Committed(TaskStateState.COMMITTED, Tone.Success),
}

/** Copies this temporary upload into a file the page keeps after the upload handler returns. */
internal fun UploadedFile.toFileData(): FileData =
    FileData(uploadedFilename = filename, content = openStream().use { stream -> stream.readBytes() })

/** Returns this date and time in the time zone of the user of the current UI as a moment. */
internal fun LocalDateTime.toUserInstant(): Instant = atZone(currentTimeZone()).toInstant()

/** Requires a start before the end of a contest given by this field; an empty end needs no start. */
internal fun <T> Binder.BindingBuilder<T, LocalDateTime?>.scheduleRules(
    start: () -> LocalDateTime?,
): Binder.BindingBuilder<T, LocalDateTime?> {
    val texts = currentLocalization().pageDeveloper
    return withValidator({ end -> end == null || start() != null }, texts.scheduleStartRequired())
        .withValidator({ end -> start()?.let { begin -> end == null || end > begin } != false }, texts.scheduleEndBeforeStart())
}

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

/** Returns the kind of [resource]. */
internal fun kindOf(resource: ResourceVo): ResourceKind = when (resource) {
    is StatementVo -> ResourceKind.Statement
    is ExerciseVo -> ResourceKind.Exercise
    is TestVo -> ResourceKind.Test
    is DeveloperSolutionVo -> ResourceKind.DeveloperSolution
}

/** Returns the label of the resource type [kind]. */
internal fun kindLabelOf(kind: ResourceKind): String = currentLocalization().pageDeveloper.resourceType(type = kind.type)

/** Returns the label of the type of [resource]. */
internal fun typeOf(resource: ResourceVo): String = kindLabelOf(kindOf(resource))

/** Returns the text of a diagnostic [report]: its severity, place and finding. */
internal fun diagnosticText(report: DiagnosticReport): String {
    val texts = currentLocalization().pageDeveloper
    // The place is a technical XML path, the same in every language.
    val place = report.location?.let { location ->
        val path = location.path.joinToString("/") { segment -> "${segment.tag}[${segment.index}]" }
        location.attribute?.let { attribute -> "$path@$attribute" } ?: path
    }
    val finding = when (val data = report.data) {
        is DiagnosticData.UnknownElement -> texts.diagnosticFindingUnknownElement(tag = data.tag)
        is DiagnosticData.MalformedXml -> texts.diagnosticFindingMalformedXml(details = data.details)
        is DiagnosticData.MissingChild -> texts.diagnosticFindingMissingChild(tag = data.tag)
        is DiagnosticData.MissingAttribute -> texts.diagnosticFindingMissingAttribute(attribute = data.attribute)
        is DiagnosticData.InvalidAttributeValue -> texts.diagnosticFindingInvalidAttributeValue(
            attribute = data.attribute,
            actual = data.actual,
            expected = data.expected,
        )
        is DiagnosticData.InvalidChildCount -> texts.diagnosticFindingInvalidChildCount(actual = data.actual, expected = data.expected)
        DiagnosticData.MissingTimeLimit -> texts.diagnosticFindingMissingTimeLimit()
        is DiagnosticData.MultipleTimeLimits -> texts.diagnosticFindingMultipleTimeLimits(count = data.count)
        is DiagnosticData.NegativeTimeLimit -> texts.diagnosticFindingNegativeTimeLimit(value = data.value)
        is DiagnosticData.ExcessiveTimeLimit -> texts.diagnosticFindingExcessiveTimeLimit(value = data.value, maximum = data.maximum)
        is DiagnosticData.InvalidEventId -> texts.diagnosticFindingInvalidEventId(id = data.id)
        DiagnosticData.MissingScoreOutput -> texts.diagnosticFindingMissingScoreOutput()
    }
    return if (place == null) {
        val severity = when (report.severity) {
            DiagnosticSeverity.Info -> DiagnosticLineSeverity.INFO
            DiagnosticSeverity.Warning -> DiagnosticLineSeverity.WARNING
            DiagnosticSeverity.Error -> DiagnosticLineSeverity.ERROR
        }
        texts.diagnosticLine(severity = severity, finding = finding)
    } else {
        val severity = when (report.severity) {
            DiagnosticSeverity.Info -> DiagnosticLineAtSeverity.INFO
            DiagnosticSeverity.Warning -> DiagnosticLineAtSeverity.WARNING
            DiagnosticSeverity.Error -> DiagnosticLineAtSeverity.ERROR
        }
        texts.diagnosticLineAt(severity = severity, place = place, finding = finding)
    }
}

/** Returns why testing or committing a task was refused, or `null` for a refusal the common handler shows. */
internal fun testingRefusalOf(error: OperationError): String? {
    val texts = currentLocalization().pageDeveloper
    return when (error) {
        is TaskAlreadyCommittedError -> texts.refusalAlreadyCommitted()
        is TaskTestingNoStatementError -> texts.refusalNoStatement()
        is TaskTestingNoPolygonsError -> texts.refusalNoPolygons()
        is TaskTestingNoDeveloperSolutionsError -> texts.refusalNoDeveloperSolutions()
        is TaskTestingNoTrikStudioVersionsError -> texts.refusalNoTrikStudioVersions()
        is TaskTestingNoExerciseForLanguageError -> texts.refusalNoExercise(
            language = when (error.language) {
                TrikSupportedLanguage.Python -> RefusalNoExerciseLanguage.PYTHON
                TrikSupportedLanguage.JavaScript -> RefusalNoExerciseLanguage.JAVASCRIPT
                TrikSupportedLanguage.VisualLanguage -> RefusalNoExerciseLanguage.VISUAL_LANGUAGE
            },
        )
        is TaskTrikStudioVersionNotSupportedError -> texts.refusalVersionNotSupported(version = error.trikStudioVersion.version)
        is TaskNotTestedError -> texts.refusalNotTested()
        else -> null
    }
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
internal fun summaryOf(execution: TaskValidationExecutionVo): String {
    val texts = currentLocalization().pageDeveloper
    return when (execution) {
        TaskValidationExecutionVo.PendingDiagnostics -> texts.summaryPending()
        is TaskValidationExecutionVo.AwaitingSubmissions -> texts.summaryAwaiting()
        is TaskValidationExecutionVo.StoppedByDiagnostics -> texts.summaryStopped()
        is TaskValidationExecutionVo.SubmissionsCreated -> texts.summaryChecking(count = execution.submissions.size)
        is TaskValidationExecutionVo.Completed -> if (execution.failures.isEmpty()) {
            texts.summaryPassed()
        } else {
            texts.summaryFailed(failed = execution.failures.size, total = execution.submissions.size)
        }
        is TaskValidationExecutionVo.IncompleteDiagnostics -> texts.summaryTechnical(description = execution.failure.description)
        is TaskValidationExecutionVo.CompletedDiagnostics -> texts.summaryTechnical(description = execution.failure.description)
        is TaskValidationExecutionVo.CreatedSubmissions -> texts.summaryTechnical(description = execution.failure.description)
    }
}
