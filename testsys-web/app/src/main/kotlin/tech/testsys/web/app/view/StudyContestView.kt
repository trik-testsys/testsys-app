package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.StudyContestTaskVo
import tech.testsys.web.app.service.study.StudyService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.texts.currentLocalization
import java.time.Clock
import java.time.Instant

/** Columns of the contest details block; the remaining time takes the rest of the row beside it. */
private const val CONTEST_DETAILS_COLUMNS = 16

/**
 * Contest of a Participant or a Student (testsys.web.page.study.contest): its details, starting it, the time left and its
 * tasks, which open after the start. Each kind of user has its own page with its own route and texts.
 *
 * @since 1.0.0-beta.1
 */
abstract class StudyContestView(private val headers: CabinetHeaders, private val clock: Clock) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    internal abstract val texts: StudyTexts

    override fun getPageTitle(): String = texts.contestTabTitle()

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
        val localization = currentLocalization()
        page(headers.cabinet(active = access.activeSection)) {
            head(texts.contestTitle(name = contest.name)) {
                access.cabinetCrumbs(this)
                if (enteredAt == null) {
                    actions {
                        mainAction(texts.contestStart()) {
                            isEnabled = canEnter(contest, now)
                            onClick {
                                confirm(
                                    title = texts.contestStartConfirmTitle(name = contest.name),
                                    text = texts.contestStartConfirmText(),
                                    action = texts.contestStartConfirmAction(),
                                ) {
                                    access.enterContest(contestId)
                                    toast(FeedbackKind.Success, texts.contestStarted())
                                    show(access, contestId)
                                }
                            }
                        }
                    }
                }
            }
            guide(texts.contestGuide())
            row {
                contestDetails(contest)
                highlightBlock(title = texts.remainingTitle()) {
                    guide(texts.remainingGuide())
                    studyRemainingTime(contest, enteredAt, now, texts)
                }
            }
            if (enteredAt != null && contest.startsAt?.let { start -> !now.isBefore(start) } != false) {
                row {
                    block(title = localization.pageCabinet.sectionTasks()) {
                        guide(texts.contestTasksGuide())
                        table(key = { item: StudyContestTaskVo -> item.task.id }, fetch = { request -> pageOf(tasks, request) }) {
                            textColumn(texts.name(), size = 12) { item -> item.task.name }
                            numberColumn(texts.contestTasksScore(), size = 4) { item -> item.bestScore }
                            column(texts.contestTasksLastSubmission()) { item -> submissionStatus(item.lastSubmission?.status, texts) }
                            empty(texts.contestTasksEmpty())
                            onRowClick(isNavigation = true) { item -> access.openTask(contestId, item.task.id) }
                        }
                    }
                }
            }
        }
    }

    /** Checks whether the first entry is allowed at [now]: not before the start and before the end of [contest]. */
    private fun canEnter(contest: ContestVo, now: Instant): Boolean =
        contest.startsAt?.let { start -> !now.isBefore(start) } != false && contest.endsAt?.let { end -> now.isBefore(end) } != false

    private fun PageRowScope.contestDetails(contest: ContestVo) {
        block(size = CONTEST_DETAILS_COLUMNS, title = texts.detailsTitle()) {
            guide(texts.contestDetailsGuide())
            row { codeInput(texts.id(), labelSize = 5, size = 6) { value = contest.id.value.toString() } }
            row { textInput(texts.name(), labelSize = 5, size = 19) { value = contest.name } }
            row { textArea(texts.description(), labelSize = 5, size = 19, maxLines = 4) { value = contest.description } }
            row {
                dateTimeInput(texts.startsAt(), labelSize = 5, size = 7) { value = contest.startsAt?.toUserDateTime() }
                dateTimeInput(texts.endsAt(), labelSize = 5, size = 7) { value = contest.endsAt?.toUserDateTime() }
            }
            row { textInput(texts.attemptDuration(), labelSize = 5, size = 7) { value = studyDurationText(contest.attemptDuration) } }
        }.isEditable = false
    }
}

/**
 * Contest of a class of a Student (testsys.web.page.study.contest).
 *
 * @since 1.0.0-beta.1
 */
@Route("student/classes/:classId([0-9]+)/contests/:contestId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class StudentContestView(
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val studentService: StudentService,
) : StudyContestView(headers, clock) {
    override val texts: StudyTexts = StudentStudyTexts

    override fun accessOf(parameters: RouteParameters): StudyAccess =
        StudyAccess.Student(ClassId(parameters.getLong(STUDY_CLASS_ID_PARAMETER).orElseThrow()), studyService, studentService)
}

/**
 * Contest of the competition of a Participant (testsys.web.page.study.contest).
 *
 * @since 1.0.0-beta.1
 */
@Route("participant/contests/:contestId([0-9]+)")
@RolesAllowed("PARTICIPANT")
class ParticipantContestView(
    headers: CabinetHeaders,
    clock: Clock,
    private val studyService: StudyService,
    private val participantService: ParticipantService,
) : StudyContestView(headers, clock) {
    override val texts: StudyTexts = ParticipantStudyTexts

    override fun accessOf(parameters: RouteParameters): StudyAccess = StudyAccess.Participant(studyService, participantService)
}
