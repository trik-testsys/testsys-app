package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TaskValidationRequestId
import tech.testsys.infra.localization.PageDeveloper.TestingSubmissionsStatusStatus
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.app.service.developer.TaskValidationSubmissionVo
import tech.testsys.web.app.service.judge.TestVerdictVo
import tech.testsys.web.app.service.study.GradingResultVo
import tech.testsys.web.app.service.study.SubmissionStatusVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.field
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.texts.currentLocalization

/** Content type of downloaded files, whose real type the system does not know. */
private const val FILE_CONTENT_TYPE = "application/octet-stream"

/**
 * Submission of a testing request owned by the Developer (testsys.web.page.developer.submission): its details with the
 * reason of a failed grading and the scores of its successful verdict per polygon with the logs and recordings.
 *
 * @since 1.0.0-beta.3
 */
@Route("developer/tasks/:taskId([0-9]+)/testing/:testingId([0-9]+)/submissions/:submissionId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperSubmissionView(
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val fileContentReader: FileContentReader,
) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageDeveloper.submissionTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val taskId = TaskId(event.routeParameters.getLong(TASK_ID_PARAMETER).orElseThrow())
        val requestId = TaskValidationRequestId(event.routeParameters.getLong(TESTING_ID_PARAMETER).orElseThrow())
        val submissionId = SubmissionId(event.routeParameters.getLong(SUBMISSION_ID_PARAMETER).orElseThrow())
        show(taskId, requestId, submissionId)
    }

    private fun show(taskId: TaskId, requestId: TaskValidationRequestId, submissionId: SubmissionId) {
        // Checks access before the blocks load, so that a refusal or a submission of another request opens its error screen.
        val checked = developerService.checkTaskValidationSubmissionAccess(taskId, requestId, submissionId)
        val loaded = SharedFetch {
            val current = developerService.checkTaskValidationSubmissionAccess(taskId, requestId, submissionId)
            current to resourceNames(developerService, current.task, current.request)
        }
        val texts = currentLocalization().pageDeveloper
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.submissionTitle(id = submissionId.value)) {
                crumb(texts.title(), DeveloperView::class.java)
                crumb(texts.taskTitle(name = checked.task.name), DeveloperTaskView::class.java, taskParameters(taskId))
                crumb(
                    texts.testingTitle(startedAt = checked.request.createdAt),
                    DeveloperTestingView::class.java,
                    testingParameters(taskId, requestId),
                )
            }
            guide(texts.submissionGuide())
            detailsBlock(loaded)
            verdictBlock(loaded)
        }
    }

    private fun PageScope.detailsBlock(loaded: SharedFetch<Pair<TaskValidationSubmissionVo, Map<DomainId, String>>>) {
        val texts = currentLocalization().pageDeveloper
        row {
            block(title = texts.details()) {
                guide(texts.submissionDetailsGuide())
                load(loaded.forLoad()) { (current, names) ->
                    val submission = current.submission
                    val (input, version) = checkNotNull(submissionRunsOf(current.request).firstOrNull { (_, id) -> id == submission.id }) {
                        "Submission ${submission.id.value} has no developer solution run in request ${current.request.id.value}"
                    }.first

                    val score = current.verdict?.testVerdicts?.sumOf { outcome -> outcome.score.value.toLong() }
                    val (status, tone) = ownStatusOf(current, isExpectedScore = score == input.expectedScore.value.toLong())

                    val reason = when (val grade = (submission.status as? SubmissionStatusVo.Graded)?.grade) {
                        is GradingResultVo.GradingError -> grade.description
                        GradingResultVo.Timeout -> texts.submissionTimeout()
                        is GradingResultVo.InvalidSubmission -> grade.description
                        is GradingResultVo.Success, null -> null
                    }

                    row {
                        textInput(texts.testingSubmissionsDeveloperSolution(), labelSize = 3, size = 9) {
                            value = names[input.developerSolution] ?: texts.testingUnknownResource(id = input.developerSolution.value)
                        }
                        codeInput(texts.trikStudioVersion(), labelSize = 3, size = 9) { value = version.version }
                    }
                    row {
                        field(texts.status(), labelSize = 3, size = 9) { badge(texts.testingSubmissionsStatus(status = status), tone) }
                        codeInput(texts.score(), labelSize = 3, size = 3) { value = score?.toString() ?: "—" }
                        codeInput(texts.expectedScore(), labelSize = 3, size = 3) { value = input.expectedScore.value.toString() }
                    }

                    if (reason != null) {
                        row { textArea(texts.submissionReason(), labelSize = 3, size = 21, maxLines = 4) { value = reason } }
                    }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.verdictBlock(loaded: SharedFetch<Pair<TaskValidationSubmissionVo, Map<DomainId, String>>>) {
        val texts = currentLocalization().pageDeveloper
        row {
            block(title = texts.submissionVerdictTitle()) {
                guide(texts.submissionVerdictGuide())
                load(loaded.forLoad()) { (current, names) ->
                    val verdict = current.verdict
                    if (verdict == null) {
                        emptyState(texts.submissionVerdictEmpty(), texts.submissionVerdictEmptyHint())
                    } else {
                        val outcomes = verdict.testVerdicts
                        table(key = { outcome: TestVerdictVo -> outcome.test }, fetch = { query -> pageOf(outcomes, query) }) {
                            textColumn(texts.submissionVerdictPolygon(), size = 12) { outcome -> nameOf(outcome, names) }
                            numberColumn(texts.score(), size = 6) { outcome -> outcome.score.value }
                            column(texts.submissionVerdictLogs(), size = 3) { outcome ->
                                downloadAction(texts.download(), produce = { download(logsOf(current, outcome)) })
                            }
                            column(texts.submissionVerdictRecording()) { outcome ->
                                if (outcome.recording != null) {
                                    downloadAction(texts.download(), produce = { download(recordingOf(current, outcome)) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun logsOf(current: TaskValidationSubmissionVo, outcome: TestVerdictVo): FileData =
        developerService.downloadTaskValidationLogs(current.request.task, current.request.id, current.submission.id, outcome.test)

    private fun recordingOf(current: TaskValidationSubmissionVo, outcome: TestVerdictVo): FileData =
        developerService.downloadTaskValidationRecording(current.request.task, current.request.id, current.submission.id, outcome.test)

    private fun download(file: FileData): DownloadContent {
        val content = fileContentReader.read(file)
        return DownloadContent(file.uploadedFilename, FILE_CONTENT_TYPE, content.size.toLong()) { content.inputStream() }
    }
}

/**
 * Returns the message variant and tone of the grading status of the submission of [current] itself, so that a graded
 * submission shows its result while its request is still checking the others.
 */
private fun ownStatusOf(current: TaskValidationSubmissionVo, isExpectedScore: Boolean): Pair<TestingSubmissionsStatusStatus, Tone> =
    when (val status = current.submission.status) {
        is SubmissionStatusVo.Graded -> when (status.grade) {
            is GradingResultVo.Success -> if (isExpectedScore) {
                TestingSubmissionsStatusStatus.EXPECTED_SCORE to Tone.Success
            } else {
                TestingSubmissionsStatusStatus.SCORE_MISMATCH to Tone.Danger
            }
            is GradingResultVo.GradingError, GradingResultVo.Timeout -> TestingSubmissionsStatusStatus.GRADING_FAILED to Tone.Danger
            is GradingResultVo.InvalidSubmission -> TestingSubmissionsStatusStatus.INVALID_SUBMISSION to Tone.Danger
        }
        // A request stopped by a technical failure no longer waits for its ungraded submissions.
        SubmissionStatusVo.Queued, SubmissionStatusVo.InProgress ->
            if (current.request.execution is TaskValidationExecutionVo.CreatedSubmissions) {
                TestingSubmissionsStatusStatus.NO_RESULT to Tone.Neutral
            } else {
                TestingSubmissionsStatusStatus.CHECKING to Tone.Info
            }
    }

/** Returns the name of the polygon of [outcome] recorded in the request, or its id if the name is unknown. */
private fun nameOf(outcome: TestVerdictVo, names: Map<DomainId, String>): String =
    names[outcome.test] ?: currentLocalization().pageDeveloper.testingUnknownResource(id = outcome.test.value)
