package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.ClassId
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization
import java.time.Clock
import java.time.Instant

/**
 * Class of a Student (testsys.web.page.student.class): the name and description of the class and its contests with their
 * dates, time to pass and state for the student; a contest row opens its page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("student/classes/:classId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class StudentClassView(
    private val headers: CabinetHeaders,
    private val studentService: StudentService,
    private val clock: Clock,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageStudent.classTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val classId = ClassId(event.routeParameters.getLong(STUDY_CLASS_ID_PARAMETER).orElseThrow())
        // Checks the role, the class and the membership first, so that a refusal opens its error screen.
        val contests = studentService.viewContests(classId)
        val studyClass = studentService.viewClasses().first { studyClass -> studyClass.id == classId }
        val now = clock.instant()
        val localization = currentLocalization()
        val texts = localization.pageStudent
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.classTitle(name = studyClass.name)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), StudentView::class.java)
            }
            guide(texts.classGuide())
            row {
                block(title = texts.detailsTitle()) {
                    guide(texts.classDetailsGuide())
                    row { codeInput(texts.id(), labelSize = 2, size = 4) { value = studyClass.id.value.toString() } }
                    row { textInput(texts.name(), labelSize = 2, size = 22) { value = studyClass.name } }
                    row { textArea(texts.description(), labelSize = 2, size = 22, maxLines = 4) { value = studyClass.description } }
                }.isEditable = false
            }
            row {
                block(title = localization.pageCabinet.sectionContests()) {
                    guide(texts.contestsGuide())
                    table(
                        key = { (_, contest): Pair<Instant?, ContestVo> -> contest.id },
                        fetch = { request -> pageOf(contests, request) },
                    ) {
                        textColumn(texts.name(), size = 5) { (_, contest) -> contest.name }
                        dateTimeColumn(texts.startsAt(), size = 4) { (_, contest) -> contest.startsAt?.toUserDateTime() }
                        dateTimeColumn(texts.endsAt(), size = 4) { (_, contest) -> contest.endsAt?.toUserDateTime() }
                        textColumn(texts.attemptDuration(), size = 3) { (_, contest) -> studyDurationText(contest.attemptDuration) }
                        column(texts.contestStateTitle(), size = 4) { (enteredAt, contest) ->
                            studyState(contest, enteredAt, now, StudentStudyTexts)
                        }
                        column(texts.remainingTitle()) { (enteredAt, contest) -> studyTimer(contest, enteredAt, now, StudentStudyTexts) }
                        empty(texts.contestsEmpty(), texts.classContestsEmptyHint())
                        onRowClick(isNavigation = true) { (_, contest) ->
                            UI.getCurrent().navigate(StudentContestView::class.java, studentContestParameters(classId, contest.id))
                        }
                    }
                }
            }
        }
    }
}
