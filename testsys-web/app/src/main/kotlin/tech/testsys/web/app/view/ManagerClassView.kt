package tech.testsys.web.app.view

import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.manager.ClassDetailsVo
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.copyAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.menu
import tech.testsys.web.components.texts.UiTexts
import java.time.Instant

/** Columns of the class details block; the invite code takes the rest of the row beside it. */
private const val DETAILS_COLUMNS = 12

/**
 * Class of a Manager (testsys.web.page.manager.class): its details, invite code with replacement and extension, students
 * and contests with adding a contest; a contest opens its results in the class.
 *
 * @since %CURRENT_VERSION%
 */
@Route("manager/classes/:classId([0-9]+)")
@PageTitle("Класс")
@RolesAllowed("MULTIPLE_ROLE")
class ManagerClassView(texts: UiTexts, private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        show(ClassId(event.routeParameters.getLong(CLASS_ID_PARAMETER).orElseThrow()))
    }

    private fun show(classId: ClassId) {
        val details = managerService.viewClass(classId)
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Класс «${details.studyClass.name}»") { managerCrumbs(CLASSES_SECTION) }
            row {
                detailsBlock(details)
                inviteBlock(details)
            }
            studentsBlock(details.students)
            managerContestsBlock(
                details.contests,
                managerService,
                onOpen = { contest -> openClassContest(classId, contest.id) },
                onAdd = { contest ->
                    managerService.addClassContest(classId, contest.id)
                    show(classId)
                },
            )
        }
    }

    private fun PageRowScope.detailsBlock(details: ClassDetailsVo) {
        block(size = DETAILS_COLUMNS, title = "Сведения") {
            row { codeInput("ID", labelSize = 8, size = 16) { value = details.studyClass.id.value.toString() } }
            row { textInput("Название", labelSize = 8, size = 16) { value = details.studyClass.name } }
            row { codeInput("Учеников", labelSize = 8, size = 16) { value = details.students.size.toString() } }
            row { codeInput("Туров", labelSize = 8, size = 16) { value = details.contests.size.toString() } }
        }.isEditable = false
    }

    @RawInviteCodeDependency(reason = "Shows and copies the stored invite code of the class as the issued one.")
    private fun PageRowScope.inviteBlock(details: ClassDetailsVo) {
        val classId = details.studyClass.id
        val invite = details.invite
        block(title = "Код-приглашение") {
            row {
                codeInput("Код-приглашение", labelSize = 8, size = 16) {
                    value = invite.codeHash.value
                    isObscured = true
                }
            }
            row { dateTimeInput("Действует до", labelSize = 8, size = 16) { value = invite.expiresAt.toServerDateTime() } }
            actions {
                copyAction("Скопировать", value = { invite.codeHash.value })
                menu {
                    item("Заменить") {
                        confirm(
                            title = "Заменить код-приглашение?",
                            text = "Прежний Код-приглашение перестанет действовать.",
                            action = "Заменить",
                            isDanger = true,
                        ) {
                            managerService.createClassInvite(classId)
                            show(classId)
                        }
                    }
                    item("Продлить") {
                        managerService.extendClassInvite(classId)
                        show(classId)
                    }
                }
            }
        }.isEditable = false
    }

    private fun PageScope.studentsBlock(students: List<Pair<MultipleRoleUserVo, Instant?>>) {
        row {
            block(title = "Ученики") {
                table(
                    key = { (student, _): Pair<MultipleRoleUserVo, Instant?> -> student.id },
                    fetch = { request -> pageOf(students, request) },
                ) {
                    codeColumn("ID", size = 6) { (student, _) -> student.id.value.toString() }
                    textColumn("Псевдоним", size = 12) { (student, _) -> student.name }
                    dateTimeColumn("Последний вход") { (_, lastLogin) -> lastLogin?.toServerDateTime() }
                    empty("Учеников пока нет", "Ученики присоединяются к классу по коду-приглашению.")
                }
            }
        }
    }
}
