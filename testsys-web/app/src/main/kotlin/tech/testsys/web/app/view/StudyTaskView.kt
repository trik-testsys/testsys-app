package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.GradingResultVo
import tech.testsys.web.app.service.study.StudyService
import tech.testsys.web.app.service.study.StudySubmissionVo
import tech.testsys.web.app.service.study.StudyTaskVo
import tech.testsys.web.app.service.study.SubmissionStatusVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.DownloadContent
import tech.testsys.web.components.actions.downloadAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.TagKind
import tech.testsys.web.components.display.TimerHandle
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.verdict
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.UploadLimits
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.fileDrop
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.texts.UiTexts
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
 * the best of them marked. Each kind of user has its own page with its own route.
 *
 * @since %CURRENT_VERSION%
 */
abstract class StudyTaskView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val clock: Clock,
    private val fileContentReader: FileContentReader,
) :
    TestSysView(texts),
    BeforeEnterObserver {
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
            head("Задача «${task.task.name}»") {
                access.cabinetCrumbs(this)
                crumb("Тур «${contest.name}»", access.contestPage, access.contestParameters(contestId))
            }
            var timer: TimerHandle? = null
            row {
                block(size = TASK_DETAILS_COLUMNS, title = "Сведения") {
                    row { codeInput("ID", labelSize = 6, size = 10) { value = task.task.id.value.toString() } }
                    row { textInput("Название", labelSize = 6, size = 18) { value = task.task.name } }
                    row { textArea("Описание", labelSize = 6, size = 18) { value = task.task.description } }
                    row { field("Файлы", labelSize = 6, size = 18) { resourceDownloads(access, contestId, task) } }
                    row {
                        field("Лучший результат", labelSize = 6, size = 18) {
                            task.best?.score?.let { score -> verdict(score.toDouble()) } ?: text("Нет")
                        }
                    }
                }.isEditable = false
                highlightBlock(title = "Осталось") { timer = studyRemainingTime(contest, enteredAt, now) }
            }
            solutionForm(access, contestId, task, isSendable, timer)
            submissionsBlock(task)
        }
    }

    private fun ContentScope.resourceDownloads(access: StudyAccess, contestId: ContestId, task: StudyTaskVo) {
        if (task.statement == null && task.exercises.isEmpty()) text("Нет файлов")
        task.statement?.let { statement -> resourceDownload("Скачать условие", access, contestId, task.task.id, statement.id) }
        task.exercises.forEach { exercise ->
            resourceDownload("Скачать упражнение «${exercise.name}»", access, contestId, task.task.id, exercise.id)
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
        // ponytail: keeps the last received file after its removal from the list; clear it on the drop state if that confuses.
        val upload = AtomicReference<FileData?>()
        lateinit var language: ValueInput<TrikSupportedLanguage?>
        row {
            block(title = "Отправка решения") {
                if (task.languages.isEmpty()) {
                    row { alert(FeedbackKind.Warning, "Отправка недоступна: у Задачи нет разрешённых языков") }
                }
                row {
                    language = select("Язык", labelSize = 4, size = 8, items = task.languages, itemLabel = ::languageLabel) {
                        task.languages.singleOrNull()?.let { only -> value = only }
                        isEnabled = isSendable
                    }
                }
                row {
                    fileDrop(
                        "Файл решения",
                        limits = UploadLimits(
                            maxFiles = 1,
                            maxFileBytes = MAX_SOLUTION_BYTES,
                            maxMemoryBytes = MAX_SOLUTION_BYTES.toLong(),
                        ),
                        consume = { file ->
                            val content = file.openStream().use { stream -> stream.readBytes() }
                            upload.set(FileData(uploadedFilename = file.filename, content = content))
                        },
                    ) { isEnabled = isSendable }
                }
                footer {
                    mainAction("Отправить") {
                        if (isSendable && timer != null) {
                            bindEnabled(timer.remainingSeconds.map { seconds -> seconds > 0 })
                        } else {
                            isEnabled = isSendable
                        }
                        onClick { send(access, contestId, task.task.id, language.value, upload.get()) }
                    }
                }
            }
        }
    }

    private fun send(access: StudyAccess, contestId: ContestId, taskId: TaskId, language: TrikSupportedLanguage?, file: FileData?) {
        when {
            language == null -> toast(FeedbackKind.Error, "Выберите язык")
            file == null -> toast(FeedbackKind.Error, "Выберите файл")
            else -> {
                access.sendSolution(contestId, taskId, file, language)
                toast(FeedbackKind.Success, "Решение отправлено")
                show(access, contestId, taskId)
            }
        }
    }

    private fun PageScope.submissionsBlock(task: StudyTaskVo) {
        row {
            block(title = "Решения") {
                table(
                    key = { submission: StudySubmissionVo -> submission.submission.id },
                    fetch = { request -> pageOf(task.submissions, request) },
                ) {
                    dateTimeColumn("Отправлено", size = 6) { submission -> submission.submission.createdAt.toServerDateTime() }
                    textColumn("Файл", size = 10) { submission -> submission.filename }
                    column("Результат") { submission ->
                        result(submission)
                        if (submission == task.best) tag("Лучшее", TagKind.Rating)
                    }
                    empty("Решений пока нет", "Отправьте решение в форме выше.")
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

        val (label, tone) = when (val status = submission.submission.status) {
            SubmissionStatusVo.Queued -> "В очереди" to Tone.Neutral
            SubmissionStatusVo.InProgress -> "Проверяется" to Tone.Info
            is SubmissionStatusVo.Graded -> when (status.grade) {
                is GradingResultVo.Success -> "Проверено" to Tone.Success
                is GradingResultVo.GradingError -> "Ошибка проверки" to Tone.Danger
                GradingResultVo.Timeout -> "Превышено время проверки" to Tone.Warning
            }
        }
        badge(label, tone)
    }

    private fun languageLabel(language: TrikSupportedLanguage): String = when (language) {
        TrikSupportedLanguage.Python -> "Python"
        TrikSupportedLanguage.JavaScript -> "JavaScript"
        TrikSupportedLanguage.VisualLanguage -> "Визуальный язык TRIK Studio"
    }
}

/**
 * Task of a contest of a class of a Student (testsys.web.page.study.task).
 *
 * @since %CURRENT_VERSION%
 */
@Route("student/classes/:classId([0-9]+)/contests/:contestId([0-9]+)/tasks/:taskId([0-9]+)")
@PageTitle("Задача")
@RolesAllowed("MULTIPLE_ROLE")
class StudentTaskView(
    texts: UiTexts,
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val studentService: StudentService,
    fileContentReader: FileContentReader,
) : StudyTaskView(texts, headers, clock, fileContentReader) {
    override fun accessOf(parameters: RouteParameters): StudyAccess =
        StudyAccess.Student(ClassId(parameters.getLong(STUDY_CLASS_ID_PARAMETER).orElseThrow()), studyService, studentService)
}

/**
 * Task of a contest of the competition of a Participant (testsys.web.page.study.task).
 *
 * @since %CURRENT_VERSION%
 */
@Route("participant/contests/:contestId([0-9]+)/tasks/:taskId([0-9]+)")
@PageTitle("Задача")
@RolesAllowed("PARTICIPANT")
class ParticipantTaskView(
    texts: UiTexts,
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val participantService: ParticipantService,
    fileContentReader: FileContentReader,
) : StudyTaskView(texts, headers, clock, fileContentReader) {
    override fun accessOf(parameters: RouteParameters): StudyAccess = StudyAccess.Participant(studyService, participantService)
}
