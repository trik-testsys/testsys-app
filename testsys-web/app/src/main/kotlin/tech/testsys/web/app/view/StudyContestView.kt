package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.StudyService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.texts.UiTexts
import java.time.Clock
import java.time.Instant

/** Columns of the contest details block; the remaining time takes the rest of the row beside it. */
private const val CONTEST_DETAILS_COLUMNS = 16

/**
 * Contest of a Participant or a Student (testsys.web.page.study.contest): its details, starting it, the time left and its
 * tasks, which open after the start. Each kind of user has its own page with its own route.
 *
 * @since %CURRENT_VERSION%
 */
abstract class StudyContestView(texts: UiTexts, private val headers: CabinetHeaders, private val clock: Clock) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val contestId = ContestId(event.routeParameters.getLong(STUDY_CONTEST_ID_PARAMETER).orElseThrow())
        show(accessOf(event.routeParameters), contestId)
    }

    /** Returns how the signed-in user studies the contest of [parameters]. */
    internal abstract fun accessOf(parameters: RouteParameters): StudyAccess

    private fun show(access: StudyAccess, contestId: ContestId) {
        // Checks the access before building, so that a refusal opens its error screen.
        val (enteredAt, contest, tasks) = access.viewContest(contestId)
        val now = clock.instant()
        page(headers.cabinet(active = access.activeSection)) {
            head("Тур «${contest.name}»") {
                access.cabinetCrumbs(this)
                if (enteredAt == null) {
                    actions {
                        mainAction("Начать тур") {
                            isEnabled = canEnter(contest, now)
                            onClick {
                                confirm(
                                    title = "Начать тур «${contest.name}»?",
                                    text = "С этого момента отсчитывается время на прохождение.",
                                    action = "Начать",
                                ) {
                                    access.enterContest(contestId)
                                    show(access, contestId)
                                }
                            }
                        }
                    }
                }
            }
            row {
                contestDetails(contest)
                highlightBlock(title = "Осталось") { studyRemainingTime(contest, enteredAt, now) }
            }
            row {
                block(title = "Задачи", subtitle = "Задачи откроются после начала тура".takeIf { enteredAt == null }) {
                    table(key = { task: TaskVo -> task.id }, fetch = { request -> pageOf(tasks, request) }) {
                        textColumn("Название") { task -> task.name }
                        empty("Задач пока нет")
                        if (enteredAt != null) onRowClick(isNavigation = true) { task -> access.openTask(contestId, task.id) }
                    }
                }
            }
        }
    }

    /** Checks whether the first entry is allowed at [now]: not before the start and before the end of [contest]. */
    private fun canEnter(contest: ContestVo, now: Instant): Boolean =
        contest.startsAt?.let { start -> !now.isBefore(start) } != false && contest.endsAt?.let { end -> now.isBefore(end) } != false

    private fun PageRowScope.contestDetails(contest: ContestVo) {
        block(size = CONTEST_DETAILS_COLUMNS, title = "Сведения") {
            row { codeInput("ID", labelSize = 6, size = 10) { value = contest.id.value.toString() } }
            row { textInput("Название", labelSize = 6, size = 18) { value = contest.name } }
            row { textArea("Описание", labelSize = 6, size = 18) { value = contest.description } }
            row {
                dateTimeInput("Начало", labelSize = 6, size = 6) { value = contest.startsAt?.toServerDateTime() }
                dateTimeInput("Конец", labelSize = 6, size = 6) { value = contest.endsAt?.toServerDateTime() }
            }
            row { textInput("Время на прохождение", labelSize = 6, size = 6) { value = studyDurationText(contest.attemptDuration) } }
        }.isEditable = false
    }
}

/**
 * Contest of a class of a Student (testsys.web.page.study.contest).
 *
 * @since %CURRENT_VERSION%
 */
@Route("student/classes/:classId([0-9]+)/contests/:contestId([0-9]+)")
@PageTitle("Тур")
@RolesAllowed("MULTIPLE_ROLE")
class StudentContestView(
    texts: UiTexts,
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val studentService: StudentService,
) : StudyContestView(texts, headers, clock) {
    override fun accessOf(parameters: RouteParameters): StudyAccess =
        StudyAccess.Student(ClassId(parameters.getLong(STUDY_CLASS_ID_PARAMETER).orElseThrow()), studyService, studentService)
}

/**
 * Contest of the competition of a Participant (testsys.web.page.study.contest).
 *
 * @since %CURRENT_VERSION%
 */
@Route("participant/contests/:contestId([0-9]+)")
@PageTitle("Тур")
@RolesAllowed("PARTICIPANT")
class ParticipantContestView(
    texts: UiTexts,
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val participantService: ParticipantService,
) : StudyContestView(texts, headers, clock) {
    override fun accessOf(parameters: RouteParameters): StudyAccess = StudyAccess.Participant(studyService, participantService)
}
