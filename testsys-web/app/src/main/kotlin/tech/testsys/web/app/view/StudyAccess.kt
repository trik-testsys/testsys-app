package tech.testsys.web.app.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.router.RouteParameters
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.StudyService
import tech.testsys.web.app.service.study.StudyTaskVo
import tech.testsys.web.components.display.TimerHandle
import tech.testsys.web.components.display.TimerValue
import tech.testsys.web.components.display.TimerVariant
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.display.timer
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.navigation.PageHeadScope
import java.time.Duration
import java.time.Instant

/** Route parameter of the pages of a student with the class id. */
internal const val STUDY_CLASS_ID_PARAMETER = "classId"

/** Route parameter of the contest and task pages of a participant or a student with the contest id. */
internal const val STUDY_CONTEST_ID_PARAMETER = "contestId"

/** Route parameter of the task pages of a participant or a student with the task id. */
internal const val STUDY_TASK_ID_PARAMETER = "taskId"

/**
 * How a participant or a student studies contests: the services the contest and task pages call, their breadcrumbs, the
 * active section of the Cabinet header and the pages they lead to.
 */
internal sealed class StudyAccess {
    /** Section of the Cabinet header active on the study pages. */
    abstract val activeSection: String

    /** Page of a contest of this user. */
    abstract val contestPage: Class<out Component>

    /** Page of a task of a contest of this user. */
    abstract val taskPage: Class<out Component>

    /** Returns the first entry time, the contest [contestId] and its tasks. */
    abstract fun viewContest(contestId: ContestId): Triple<Instant?, ContestVo, List<TaskVo>>

    /** Saves the first entry into [contestId]. */
    abstract fun enterContest(contestId: ContestId)

    /** Returns [taskId] of [contestId] with the submissions of the user. */
    abstract fun viewTask(contestId: ContestId, taskId: TaskId): StudyTaskVo

    /** Returns the file of the statement or exercise [resourceId] of [taskId]. */
    abstract fun downloadTaskResource(contestId: ContestId, taskId: TaskId, resourceId: DomainId): FileData

    /** Sends [file] in [language] as a solution of [taskId]. */
    abstract fun sendSolution(contestId: ContestId, taskId: TaskId, file: FileData, language: TrikSupportedLanguage)

    /** Adds to [head] the breadcrumbs from the start page to the page that lists the contests. */
    abstract fun cabinetCrumbs(head: PageHeadScope)

    /** Returns the route parameters of the page of [contestId]. */
    abstract fun contestParameters(contestId: ContestId): RouteParameters

    /** Returns the route parameters of the page of [taskId] of [contestId]. */
    abstract fun taskParameters(contestId: ContestId, taskId: TaskId): RouteParameters

    /** Opens the page of [taskId] of [contestId]. */
    fun openTask(contestId: ContestId, taskId: TaskId) {
        UI.getCurrent().navigate(taskPage, taskParameters(contestId, taskId))
    }

    /** A student studies the contests of the class [classId]. */
    class Student(
        private val classId: ClassId,
        private val studyService: StudyService,
        private val studentService: StudentService,
    ) : StudyAccess() {
        override val activeSection: String = CabinetHeaders.MENU_SECTION
        override val contestPage: Class<out Component> = StudentContestView::class.java
        override val taskPage: Class<out Component> = StudentTaskView::class.java

        override fun viewContest(contestId: ContestId) = studyService.viewContest(classId, contestId)

        override fun enterContest(contestId: ContestId) {
            studentService.enterContest(classId, contestId)
        }

        override fun viewTask(contestId: ContestId, taskId: TaskId) = studyService.viewTask(classId, contestId, taskId)

        override fun downloadTaskResource(contestId: ContestId, taskId: TaskId, resourceId: DomainId) =
            studyService.downloadTaskResource(classId, contestId, taskId, resourceId)

        override fun sendSolution(contestId: ContestId, taskId: TaskId, file: FileData, language: TrikSupportedLanguage) {
            studyService.sendSolution(classId = classId, contestId = contestId, taskId = taskId, file = file, language = language)
        }

        override fun cabinetCrumbs(head: PageHeadScope) {
            val studyClass = studentService.viewClasses().first { studyClass -> studyClass.id == classId }
            head.crumb("Главная", MultiMainView::class.java)
            head.crumb("Кабинет Ученика", StudentView::class.java)
            head.crumb("Класс «${studyClass.name}»", StudentClassView::class.java, studentClassParameters(classId))
        }

        override fun contestParameters(contestId: ContestId) = studentContestParameters(classId, contestId)

        override fun taskParameters(contestId: ContestId, taskId: TaskId) = RouteParameters(
            mapOf(
                STUDY_CLASS_ID_PARAMETER to classId.value.toString(),
                STUDY_CONTEST_ID_PARAMETER to contestId.value.toString(),
                STUDY_TASK_ID_PARAMETER to taskId.value.toString(),
            ),
        )
    }

    /** A participant studies the contests of the competition. */
    class Participant(private val studyService: StudyService, private val participantService: ParticipantService) : StudyAccess() {
        override val activeSection: String = CabinetHeaders.MAIN_SECTION
        override val contestPage: Class<out Component> = ParticipantContestView::class.java
        override val taskPage: Class<out Component> = ParticipantTaskView::class.java

        override fun viewContest(contestId: ContestId) = studyService.viewContest(contestId)

        override fun enterContest(contestId: ContestId) {
            participantService.enterContest(contestId)
        }

        override fun viewTask(contestId: ContestId, taskId: TaskId) = studyService.viewTask(contestId, taskId)

        override fun downloadTaskResource(contestId: ContestId, taskId: TaskId, resourceId: DomainId) =
            studyService.downloadTaskResource(contestId, taskId, resourceId)

        override fun sendSolution(contestId: ContestId, taskId: TaskId, file: FileData, language: TrikSupportedLanguage) {
            studyService.sendSolution(contestId, taskId, file, language)
        }

        override fun cabinetCrumbs(head: PageHeadScope) {
            head.crumb("Кабинет Участника", ParticipantView::class.java)
        }

        override fun contestParameters(contestId: ContestId) = RouteParameters(STUDY_CONTEST_ID_PARAMETER, contestId.value.toString())

        override fun taskParameters(contestId: ContestId, taskId: TaskId) = RouteParameters(
            mapOf(STUDY_CONTEST_ID_PARAMETER to contestId.value.toString(), STUDY_TASK_ID_PARAMETER to taskId.value.toString()),
        )
    }
}

/** Returns the route parameters of the class page of a student. */
internal fun studentClassParameters(classId: ClassId): RouteParameters = RouteParameters(STUDY_CLASS_ID_PARAMETER, classId.value.toString())

/** Returns the route parameters of the page of a student with [contestId] of [classId]. */
internal fun studentContestParameters(classId: ClassId, contestId: ContestId): RouteParameters = RouteParameters(
    mapOf(STUDY_CLASS_ID_PARAMETER to classId.value.toString(), STUDY_CONTEST_ID_PARAMETER to contestId.value.toString()),
)

/**
 * Returns the moment the attempt of a user who entered [contest] at [enteredAt] ends: the earlier of the contest end and the
 * end of the time to pass it, or `null` without either.
 */
internal fun studyDeadline(contest: ContestVo, enteredAt: Instant?): Instant? {
    val attemptEnd = enteredAt?.let { entered -> contest.attemptDuration?.let { duration -> entered + duration } }
    return listOfNotNull(contest.endsAt, attemptEnd).minOrNull()
}

/** Returns [duration] as «1 ч 30 мин», or «Без ограничения» without it. */
internal fun studyDurationText(duration: Duration?): String {
    if (duration == null) return "Без ограничения"

    val hours = duration.toHours()
    val minutes = duration.toMinutesPart()
    return listOfNotNull("$hours ч".takeIf { hours > 0 }, "$minutes мин".takeIf { minutes > 0 || hours == 0L }).joinToString(" ")
}

/**
 * Adds the state of [contest] for a user who entered it at [enteredAt]: not started without an entry before its end, running
 * with the remaining time before the deadline, otherwise completed.
 */
internal fun ContentScope.studyState(contest: ContestVo, enteredAt: Instant?, now: Instant) {
    val deadline = studyDeadline(contest, enteredAt)
    when {
        deadline != null && !now.isBefore(deadline) -> badge("Завершён", Tone.Neutral)
        enteredAt == null -> badge("Не начат", Tone.Neutral)
        else -> {
            badge("Идёт", Tone.Info)
            deadline?.let { end -> timer("Осталось", TimerValue.Until(end)) }
        }
    }
}

/**
 * Fills the body of the block with the time left to a user who entered [contest] at [enteredAt] and returns the live timer, if
 * the time is running out.
 */
internal fun BlockScope.studyRemainingTime(contest: ContestVo, enteredAt: Instant?, now: Instant): TimerHandle? {
    val deadline = studyDeadline(contest, enteredAt)
    var liveTimer: TimerHandle? = null
    row {
        when {
            deadline != null && !now.isBefore(deadline) -> text("Время вышло")
            enteredAt == null -> text("Отсчёт начнётся с началом тура")
            deadline == null -> text("Без ограничения по времени")
            else -> {
                liveTimer = timer("Осталось", TimerValue.Until(deadline), variant = TimerVariant.Hero)
            }
        }
    }
    return liveTimer
}
