package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
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
import tech.testsys.web.components.texts.currentLocalization

/** Columns of the classes block; joining a class takes the rest of the row beside it. */
private const val CLASSES_COLUMNS = 16

/**
 * Cabinet of a Student (testsys.web.page.student): the classes of the student with links to their pages and joining a class
 * by an invite code.
 *
 * @since %CURRENT_VERSION%
 */
@Route("student/:section?(classes)")
@RolesAllowed("MULTIPLE_ROLE")
class StudentView(private val headers: CabinetHeaders, private val studentService: StudentService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageStudent.title()

    override fun beforeEnter(event: BeforeEnterEvent) {
        show()
    }

    private fun show() {
        // Also checks the student role before the table loads, so that its absence opens the forbidden screen.
        val classes = studentService.viewClasses()
        val localization = currentLocalization()
        val texts = localization.pageStudent
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.title()) { crumb(localization.pageCabinet.home(), MultiMainView::class.java) }
            guide(texts.guide())
            row {
                block(size = CLASSES_COLUMNS, title = localization.pageCabinet.sectionClasses()) {
                    guide(texts.classesGuide())
                    table(key = { studyClass: ClassVo -> studyClass.id }, fetch = { request -> pageOf(classes, request) }) {
                        textColumn(texts.name(), size = 8) { studyClass -> studyClass.name }
                        textColumn(texts.description()) { studyClass -> studyClass.description }
                        empty(texts.classesEmpty(), texts.classesEmptyHint())
                        onRowClick(isNavigation = true) { studyClass ->
                            UI.getCurrent().navigate(StudentClassView::class.java, studentClassParameters(studyClass.id))
                        }
                    }
                }
                block(title = texts.joinTitle()) {
                    guide(texts.joinGuide())
                    lateinit var inviteCode: ValueInput<String>
                    row { inviteCode = codeInput(texts.joinInviteCode(), labelSize = 10, size = 14) }
                    footer {
                        mainAction(texts.joinSubmit()) {
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
        val texts = currentLocalization().pageStudent
        val studyClass = try {
            studentService.joinClass(inviteCode)
        } catch (exception: OperationException) {
            when (exception.error) {
                is ClassInviteCodeNotValidError -> toast(FeedbackKind.Error, texts.joinInviteCodeInvalid())
                is ClassInviteCodeExpiredError -> toast(FeedbackKind.Error, texts.joinInviteCodeExpired())
                else -> throw exception
            }
            return
        }
        toast(FeedbackKind.Success, texts.joinJoined(name = studyClass.name))
        show()
    }
}
