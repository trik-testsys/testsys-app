package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.manager.ClassDetailsVo
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.copyAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.load
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.BlockScope
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.menu
import tech.testsys.web.components.texts.currentLocalization
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
@RolesAllowed("MULTIPLE_ROLE")
class ManagerClassView(private val headers: CabinetHeaders, private val managerService: ManagerService) :
    TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageManager.classTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        show(ClassId(event.routeParameters.getLong(CLASS_ID_PARAMETER).orElseThrow()))
    }

    private fun show(classId: ClassId) {
        // Checks access before the blocks load, so that a refusal opens its error screen.
        val studyClass = managerService.checkClassAccess(classId)
        val loaded = lazy { managerService.viewClass(classId) }
        val texts = currentLocalization().pageManager
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.classTitle(name = studyClass.name)) { managerCrumbs(CLASSES_SECTION) }
            guide(texts.classGuide())
            row {
                detailsBlock(loaded)
                inviteBlock(classId, loaded)
            }
            studentsBlock(loaded)
            managerContestsBlock(
                fetch = { loaded.value.contests },
                managerService,
                onOpen = { contest -> openClassContest(classId, contest.id) },
                onAdd = { contest ->
                    managerService.addClassContest(classId, contest.id)
                    toast(FeedbackKind.Success, currentLocalization().pageManager.classContestAdded())
                    show(classId)
                },
            )
        }
    }

    private fun PageRowScope.detailsBlock(loaded: Lazy<ClassDetailsVo>) {
        val texts = currentLocalization().pageManager
        var form: DetailsForm? = null
        block(size = DETAILS_COLUMNS, title = texts.detailsTitle()) {
            guide(texts.detailsGuide())
            editing(onSave = { form?.save?.invoke() == true }, onCancel = { form?.cancel?.invoke() })
            load({ loaded.value }) { details -> form = detailsRows(details) }
        }
    }

    /** Adds the rows of the details of the class in [details] and returns their form. */
    private fun BlockScope.detailsRows(details: ClassDetailsVo): DetailsForm {
        val texts = currentLocalization().pageManager
        val entity = details.studyClass
        val draft = Binder<DetailsDraft>()
        row {
            codeInput(texts.id(), labelSize = 6, size = 18) {
                value = entity.id.value.toString()
                isEditable = false
            }
        }
        row {
            textInput(texts.name(), labelSize = 6, size = 18) {
                draft.forField(this).nameRules(texts.nameRequired())
                    .bind({ values -> values.name }, { values, name -> values.name = name })
            }
        }
        row {
            textArea(texts.description(), labelSize = 6, size = 18, maxLines = 4) {
                draft.forField(this).bind({ values -> values.description }, { values, description -> values.description = description })
            }
        }
        row {
            codeInput(texts.studentCount(), labelSize = 6, size = 6) {
                value = details.students.size.toString()
                isEditable = false
            }
            codeInput(texts.contestCount(), labelSize = 6, size = 6) {
                value = details.contests.size.toString()
                isEditable = false
            }
        }

        draft.readBean(DetailsDraft(entity.name, entity.description))
        return DetailsForm(
            save = {
                val values = DetailsDraft()
                val isValid = draft.writeBeanIfValid(values)
                if (isValid) {
                    managerService.editClass(classId = entity.id, className = values.name, description = values.description)
                    toast(FeedbackKind.Success, currentLocalization().pageManager.classSaved())
                    show(entity.id)
                }
                isValid
            },
            cancel = { draft.readBean(DetailsDraft(entity.name, entity.description)) },
        )
    }

    @RawInviteCodeDependency(reason = "Shows and copies the stored invite code of the class as the issued one.")
    private fun PageRowScope.inviteBlock(classId: ClassId, loaded: Lazy<ClassDetailsVo>) {
        val texts = currentLocalization().pageManager
        block(title = texts.classInviteCode()) {
            guide(texts.classInviteGuide())
            lateinit var copy: ActionHandle
            actions {
                // Enabled once the invite code is loaded.
                copy = copyAction(texts.classInviteCopy(), value = { loaded.value.invite.codeHash.value }) { isEnabled = false }
                menu {
                    item(texts.classInviteReplace()) {
                        confirm(
                            title = texts.classInviteReplaceTitle(),
                            text = texts.classInviteReplaceText(),
                            action = texts.classInviteReplace(),
                            isDanger = true,
                        ) {
                            managerService.createClassInvite(classId)
                            toast(FeedbackKind.Success, currentLocalization().pageManager.classInviteReplaced())
                            show(classId)
                        }
                    }
                    item(texts.classInviteExtend()) {
                        managerService.extendClassInvite(classId)
                        toast(FeedbackKind.Success, currentLocalization().pageManager.classInviteExtended())
                        show(classId)
                    }
                }
            }
            load({ loaded.value.invite }) { invite ->
                row {
                    codeInput(texts.classInviteCode(), labelSize = 8, size = 16) {
                        value = invite.codeHash.value
                        isObscured = true
                    }
                }
                row { dateTimeInput(texts.classInviteExpires(), labelSize = 8, size = 16) { value = invite.expiresAt.toUserDateTime() } }
                copy.isEnabled = true
            }
        }.isEditable = false
    }

    private fun PageScope.studentsBlock(loaded: Lazy<ClassDetailsVo>) {
        val texts = currentLocalization().pageManager
        row {
            block(title = texts.classStudentsTitle()) {
                guide(texts.classStudentsGuide())
                load({ loaded.value.students }) { students ->
                    table(
                        key = { (student, _): Pair<MultipleRoleUserVo, Instant?> -> student.id },
                        fetch = { request -> pageOf(students, request) },
                    ) {
                        codeColumn(texts.id(), size = 6) { (student, _) -> student.id.value.toString() }
                        textColumn(texts.nickname(), size = 12) { (student, _) -> student.name }
                        dateTimeColumn(texts.lastLogin()) { (_, lastLogin) -> lastLogin?.toUserDateTime() }
                        empty(texts.classStudentsEmpty(), texts.classStudentsEmptyHint())
                    }
                }
            }
        }
    }

    private class DetailsDraft(var name: String = "", var description: String = "")
}
