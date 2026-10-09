package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
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
import tech.testsys.web.components.texts.UiTexts
import java.time.Clock
import java.time.Instant

/**
 * Class of a Student (testsys.web.page.student.class): the name and description of the class and its contests with their
 * dates, time to pass and state for the student; a contest row opens its page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("student/classes/:classId([0-9]+)")
@PageTitle("Класс")
@RolesAllowed("MULTIPLE_ROLE")
class StudentClassView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val studentService: StudentService,
    private val clock: Clock,
) : TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val classId = ClassId(event.routeParameters.getLong(STUDY_CLASS_ID_PARAMETER).orElseThrow())
        // Checks the role, the class and the membership first, so that a refusal opens its error screen.
        val contests = studentService.viewContests(classId)
        val studyClass = studentService.viewClasses().first { studyClass -> studyClass.id == classId }
        val now = clock.instant()
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Класс «${studyClass.name}»") {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Ученика", StudentView::class.java)
            }
            row {
                block(title = "Сведения") {
                    row { codeInput("ID", labelSize = 4, size = 8) { value = studyClass.id.value.toString() } }
                    row { textInput("Название", labelSize = 4, size = 20) { value = studyClass.name } }
                    row { textArea("Описание", labelSize = 4, size = 20) { value = studyClass.description } }
                }.isEditable = false
            }
            row {
                block(title = "Туры") {
                    table(
                        key = { (_, contest): Pair<Instant?, ContestVo> -> contest.id },
                        fetch = { request -> pageOf(contests, request) },
                    ) {
                        textColumn("Название", size = 6) { (_, contest) -> contest.name }
                        dateTimeColumn("Начало", size = 4) { (_, contest) -> contest.startsAt?.toServerDateTime() }
                        dateTimeColumn("Конец", size = 4) { (_, contest) -> contest.endsAt?.toServerDateTime() }
                        textColumn("Время на прохождение", size = 4) { (_, contest) -> studyDurationText(contest.attemptDuration) }
                        column("Состояние") { (enteredAt, contest) -> studyState(contest, enteredAt, now) }
                        empty("Туров пока нет", "Туры появятся, когда Организатор добавит их в класс.")
                        onRowClick(isNavigation = true) { (_, contest) ->
                            UI.getCurrent().navigate(StudentContestView::class.java, studentContestParameters(classId, contest.id))
                        }
                    }
                }
            }
        }
    }
}
