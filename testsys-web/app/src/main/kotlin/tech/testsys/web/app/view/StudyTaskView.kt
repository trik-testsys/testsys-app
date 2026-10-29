package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.StudyService
import tech.testsys.web.app.service.study.StudySubmissionVo
import tech.testsys.web.app.service.study.StudyTaskVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.actions.iconDownloadAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.TagKind
import tech.testsys.web.components.display.TimerHandle
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.FileDropHandle
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.PageScope
import java.time.Clock
import java.util.concurrent.atomic.AtomicReference

/** Columns of the task details block; the remaining time takes the rest of the row beside it. */
private const val TASK_DETAILS_COLUMNS = 16

/** Largest solution file a user can send: 10 MiB. */
private const val MAX_SOLUTION_BYTES = 10 * 1024 * 1024

/** Content type of the downloaded statements and exercises. */
private const val RESOURCE_CONTENT_TYPE = "application/octet-stream"

/**
 * Task of a contest of a Participant or a Student (testsys.web.page.study.task): its details with the statement and the
 * exercises to download, the time left, sending a solution in an allowed language and the sent solutions with their results,
 * the best of them marked. Each kind of user has its own page with its own route and texts.
 *
 * @since 1.0.0-beta.1
 */
abstract class StudyTaskView(
    private val headers: CabinetHeaders,
    private val clock: Clock,
    private val fileContentReader: FileContentReader,
) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    internal abstract val texts: StudyTexts

    override fun getPageTitle(): String = texts.taskTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val contestId = ContestId(event.routeParameters.getLong(STUDY_CONTEST_ID_PARAMETER).orElseThrow())
        val taskId = TaskId(event.routeParameters.getLong(STUDY_TASK_ID_PARAMETER).orElseThrow())
        show(accessOf(event.routeParameters), contestId, taskId)
    }

    /** Returns how the signed-in user studies the contest of [parameters]. */
    internal abstract fun accessOf(parameters: RouteParameters): StudyAccess

    private fun show(access: StudyAccess, contestId: ContestId, taskId: TaskId) {
        // Checks the access and the entry before building, so that a refusal opens its error screen.
        val task = access.viewTask(contestId, taskId)
        val (enteredAt, contest, _) = access.viewContest(contestId)
        val now = clock.instant()
        val deadline = studyDeadline(contest, enteredAt)
        val isSendable = task.languages.isNotEmpty() && (deadline == null || now.isBefore(deadline))
        page(headers.cabinet(active = access.activeSection)) {
            head(texts.taskTitle(name = task.task.name)) {
                access.cabinetCrumbs(this)
                crumb(texts.contestTitle(name = contest.name), access.contestPage, access.contestParameters(contestId))
            }
            guide(texts.taskGuide())
            var timer: TimerHandle? = null
            row {
                block(size = TASK_DETAILS_COLUMNS, title = texts.detailsTitle()) {
                    guide(texts.taskDetailsGuide())
                    row { codeInput(texts.id(), labelSize = 4, size = 6) { value = task.task.id.value.toString() } }
                    row { textInput(texts.name(), labelSize = 4, size = 20) { value = task.task.name } }
                    row { textArea(texts.description(), labelSize = 4, size = 20, maxLines = 4) { value = task.task.description } }
                    row { field(texts.taskFiles(), labelSize = 4, size = 20) { resourceDownloads(access, contestId, task) } }
                    row {
                        field(texts.taskBest(), labelSize = 4, size = 20) {
                            task.best?.score?.let { score -> verdict(score.toDouble()) } ?: text(texts.taskBestNone())
                        }
                    }
                }.isEditable = false
                highlightBlock(title = texts.remainingTitle()) {
                    guide(texts.remainingGuide())
                    timer = studyRemainingTime(contest, enteredAt, now, texts)
                }
            }
            solutionForm(access, contestId, task, isSendable, timer)
            submissionsBlock(access, contestId, task, isSendable)
        }
    }

    private fun ContentScope.resourceDownloads(access: StudyAccess, contestId: ContestId, task: StudyTaskVo) {
        if (task.statement == null && task.exercises.isEmpty()) text(texts.taskFilesEmpty())
        task.statement?.let { statement -> resourceDownload(texts.taskDownloadStatement(), access, contestId, task.task.id, statement.id) }
        task.exercises.forEach { exercise ->
            val label = texts.taskDownloadExercise(language = languageLabel(exercise.language))
            resourceDownload(label, access, contestId, task.task.id, exercise.id)
        }
    }

    private fun ContentScope.resourceDownload(
        label: String,
        access: StudyAccess,
        contestId: ContestId,
        taskId: TaskId,
        resourceId: DomainId,
    ) {
        downloadAction(
            label,
            produce = {
                val file = access.downloadTaskResource(contestId, taskId, resourceId)
                val content = fileContentReader.read(file)
                DownloadContent(file.uploadedFilename, RESOURCE_CONTENT_TYPE, content.size.toLong()) { content.inputStream() }
            },
        )
    }

    /**
     * Adds the form sending a solution in one of the allowed languages; it is disabled if [isSendable] is false and follows the
     * live [timer] otherwise.
     */
    private fun PageScope.solutionForm(
        access: StudyAccess,
        contestId: ContestId,
        task: StudyTaskVo,
        isSendable: Boolean,
        timer: TimerHandle?,
    ) {
        val fileCopy = AtomicReference<Pair<String, FileData>?>()
        lateinit var drop: FileDropHandle
        lateinit var language: ValueInput<TrikSupportedLanguage?>
        row {
            block(title = texts.taskSendTitle()) {
                if (task.languages.isEmpty()) {
                    row { alert(FeedbackKind.Warning, texts.taskSendUnavailable()) }
                }
                row {
                    language = select(
                        texts.taskSendLanguage(),
                        labelSize = 2,
                        size = 10,
                        items = task.languages,
                        itemLabel = ::languageLabel,
                    ) {
                        task.languages.singleOrNull()?.let { only -> value = only }
                        isEnabled = isSendable
                    }
                    drop = fileDrop(
                        texts.taskSendFile(),
                        labelSize = 2,
                        size = 10,
                        limits = UploadLimits(
                            maxFiles = 1,
                            maxFileBytes = MAX_SOLUTION_BYTES,
                            maxMemoryBytes = MAX_SOLUTION_BYTES.toLong(),
                        ),
                        consume = { file ->
                            val content = file.openStream().use { stream -> stream.readBytes() }
                            fileCopy.set(file.id to FileData(uploadedFilename = file.filename, content = content))
                        },
                    ) { isEnabled = isSendable }
                }
                guide(texts.taskSendGuide()) {
                    step(language, title = texts.taskSendLanguageStepTitle(), description = texts.taskSendLanguageStepDescription())
                    step(drop, title = texts.taskSendFile(), description = texts.taskSendFileStepDescription())
                }
                footer {
                    mainAction(texts.taskSendSubmit()) {
                        if (isSendable && timer != null) {
                            bindEnabled(timer.remainingSeconds.map { seconds -> seconds > 0 })
                        } else {
                            isEnabled = isSendable
                        }
                        onClick {
                            val file = fileCopy.get()?.takeIf { (id, _) -> id in drop.fileIds }?.second
                            send(access, contestId, task.task.id, language.value, file)
                        }
                    }
                }
            }
        }
    }

    private fun send(access: StudyAccess, contestId: ContestId, taskId: TaskId, language: TrikSupportedLanguage?, file: FileData?) {
        when {
            language == null -> toast(FeedbackKind.Error, texts.taskSendLanguageRequired())
            file == null -> toast(FeedbackKind.Error, texts.taskSendFileRequired())
            else -> {
                access.sendSolution(contestId, taskId, file, language)
                toast(FeedbackKind.Success, texts.taskSendSent())
                show(access, contestId, taskId)
            }
        }
    }

    private fun PageScope.submissionsBlock(access: StudyAccess, contestId: ContestId, task: StudyTaskVo, isSendable: Boolean) {
        row {
            block(title = texts.taskSubmissionsTitle()) {
                guide(texts.taskSubmissionsGuide())
                table(
                    key = { submission: StudySubmissionVo -> submission.submission.id },
                    fetch = { request -> pageOf(task.submissions.asReversed(), request) },
                ) {
                    dateTimeColumn(texts.taskSubmissionsSent(), size = 5) { submission -> submission.submission.createdAt.toUserDateTime() }
                    textColumn(texts.taskSubmissionsFile(), size = 9) { submission -> submission.filename }
                    column(texts.taskSubmissionsResult(), size = 8) { submission ->
                        result(submission)
                        if (submission == task.best) tag(texts.taskSubmissionsBest(), TagKind.Rating)
                    }
                    column("") { submission ->
                        iconDownloadAction(texts.taskSubmissionsDownload(), produce = {
                            val file = access.downloadSolution(contestId, task.task.id, submission.submission.id)
                            val content = fileContentReader.read(file)
                            DownloadContent(file.uploadedFilename, RESOURCE_CONTENT_TYPE, content.size.toLong()) { content.inputStream() }
                        })
                    }
                    empty(
                        texts.taskSubmissionsEmpty(),
                        if (isSendable) texts.taskSubmissionsEmptyHintSendable() else texts.taskSubmissionsEmptyHintClosed(),
                    )
                }
            }
        }
    }

    /** Adds the final score of [submission], or its grading status without one. */
    private fun ContentScope.result(submission: StudySubmissionVo) {
        val score = submission.score
        if (score != null) {
            verdict(score.toDouble())
            return
        }

        submissionStatus(submission.submission.status, texts)
    }

    private fun languageLabel(language: TrikSupportedLanguage): String {
        val variant = when (language) {
            TrikSupportedLanguage.Python -> StudyTexts.Language.PYTHON
            TrikSupportedLanguage.JavaScript -> StudyTexts.Language.JAVASCRIPT
            TrikSupportedLanguage.VisualLanguage -> StudyTexts.Language.VISUAL_LANGUAGE
        }
        return texts.taskSendLanguageName(variant)
    }
}

/**
 * Task of a contest of a class of a Student (testsys.web.page.study.task).
 *
 * @since 1.0.0-beta.1
 */
@Route("student/classes/:classId([0-9]+)/contests/:contestId([0-9]+)/tasks/:taskId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class StudentTaskView(
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val studentService: StudentService,
    fileContentReader: FileContentReader,
) : StudyTaskView(headers, clock, fileContentReader) {
    override val texts: StudyTexts = StudentStudyTexts

    override fun accessOf(parameters: RouteParameters): StudyAccess =
        StudyAccess.Student(ClassId(parameters.getLong(STUDY_CLASS_ID_PARAMETER).orElseThrow()), studyService, studentService)
}

/**
 * Task of a contest of the competition of a Participant (testsys.web.page.study.task).
 *
 * @since 1.0.0-beta.1
 */
@Route("participant/contests/:contestId([0-9]+)/tasks/:taskId([0-9]+)")
@RolesAllowed("PARTICIPANT")
class ParticipantTaskView(
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val participantService: ParticipantService,
    fileContentReader: FileContentReader,
) : StudyTaskView(headers, clock, fileContentReader) {
    override val texts: StudyTexts = ParticipantStudyTexts

    override fun accessOf(parameters: RouteParameters): StudyAccess = StudyAccess.Participant(studyService, participantService)
}
