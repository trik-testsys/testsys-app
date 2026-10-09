package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.operation.error.ClassInviteCodeExpiredError
import tech.testsys.operation.error.ClassInviteCodeNotValidError
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.service.student.ClassVo
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.texts.UiTexts

/** Columns of the classes block; joining a class takes the rest of the row beside it. */
private const val CLASSES_COLUMNS = 16

/**
 * Cabinet of a Student (testsys.web.page.student): the classes of the student with links to their pages and joining a class
 * by an invite code.
 *
 * @since %CURRENT_VERSION%
 */
@Route("student/:section?(classes)")
@PageTitle("Кабинет Ученика")
@RolesAllowed("MULTIPLE_ROLE")
class StudentView(texts: UiTexts, private val headers: CabinetHeaders, private val studentService: StudentService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        show()
    }

    private fun show() {
        // Also checks the student role before the table loads, so that its absence opens the forbidden screen.
        val classes = studentService.viewClasses()
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Кабинет Ученика") { crumb("Главная", MultiMainView::class.java) }
            row {
                block(size = CLASSES_COLUMNS, title = "Классы") {
                    table(key = { studyClass: ClassVo -> studyClass.id }, fetch = { request -> pageOf(classes, request) }) {
                        textColumn("Название", size = 8) { studyClass -> studyClass.name }
                        textColumn("Описание") { studyClass -> studyClass.description }
                        empty("Классов пока нет", "Присоединитесь к классу по Коду-приглашению.")
                        onRowClick(isNavigation = true) { studyClass ->
                            UI.getCurrent().navigate(StudentClassView::class.java, studentClassParameters(studyClass.id))
                        }
                    }
                }
                block(title = "Присоединиться к классу") {
                    lateinit var inviteCode: ValueInput<String>
                    row { inviteCode = codeInput("Код-приглашение", labelSize = 10, size = 14) }
                    footer {
                        mainAction("Присоединиться") {
                            clickOnEnter()
                            onClick { joinClass(inviteCode.value) }
                        }
                    }
                }
            }
        }
    }

    /** Joins the class by [inviteCode] and rebuilds the page with it. */
    private fun joinClass(inviteCode: String) {
        val studyClass = try {
            studentService.joinClass(inviteCode)
        } catch (exception: OperationException) {
            when (exception.error) {
                is ClassInviteCodeNotValidError -> toast(FeedbackKind.Error, "Код-приглашение недействителен")
                is ClassInviteCodeExpiredError -> toast(FeedbackKind.Error, "Срок Кода-приглашения истёк")
                else -> throw exception
            }
            return
        }
        toast(FeedbackKind.Success, "Вы присоединились к классу «${studyClass.name}»")
        show()
    }
}
