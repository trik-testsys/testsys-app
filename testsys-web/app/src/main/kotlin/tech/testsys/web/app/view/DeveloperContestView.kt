package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.operation.error.ContestAlreadySharedError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.TaskAlreadyAttachedToContestError
import tech.testsys.operation.error.TaskNotCommittedError
import tech.testsys.operation.error.TaskTrikStudioVersionNotSupportedError
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.destructiveAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.lookup
import tech.testsys.web.components.forms.multiSelect
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageRowScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization
import java.time.Duration
import java.time.LocalDateTime

/** Columns of the contest details block; the access block takes the rest of the row beside it. */
private const val DETAILS_COLUMNS = 14

/**
 * Contest of a Developer (testsys.web.page.developer.contest): its details, tasks and access of communities. Only the
 * owner changes the contest, and only until access is shared; otherwise the page explains why it cannot be changed.
 *
 * @since 1.0.0-beta.1
 */
@Route("developer/contests/:contestId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperContestView(
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val multipleRoleUserService: MultipleRoleUserService,
    private val currentUser: CurrentUser,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageDeveloper.contestTabTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        show(ContestId(event.routeParameters.getLong(CONTEST_ID_PARAMETER).orElseThrow()))
    }

    private fun show(contestId: ContestId) {
        val (contest, tasks, shared) = developerService.viewContest(contestId)
        val me = currentUser.multipleRoleUser().id
        val isOwner = contest.owner == me
        val isChangeable = isOwner && contest.sharedTo.isEmpty()
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.contestTitle(name = contest.name)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), DeveloperView::class.java)
                crumb(localization.pageCabinet.sectionContests(), DeveloperView::class.java, developerSectionParameters(CONTESTS_SECTION))
                actions {
                    destructiveAction(texts.contestDelete()) {
                        isEnabled = isChangeable
                        onClick { delete(contest) }
                    }
                }
            }
            guide(texts.contestGuide())
            if (!isChangeable) {
                val reason = if (isOwner) texts.contestLockedShared() else texts.contestLockedNotOwner()
                row {
                    block {
                        guide(description = reason, title = texts.contestLocked())
                        row { alert(kind = FeedbackKind.Info, title = texts.contestLocked(), text = reason) }
                    }
                }
            }
            row {
                detailsBlock(contest, isChangeable)
                accessBlock(contest, shared, isOwner)
            }
            tasksBlock(contest, tasks, me, isChangeable)
        }
    }

    private fun PageRowScope.detailsBlock(contest: ContestVo, isChangeable: Boolean) {
        val texts = currentLocalization().pageDeveloper
        val draft = Binder<ContestDraft>()
        val initial = ContestDraft(
            name = contest.name,
            startsAt = contest.startsAt?.toUserDateTime(),
            endsAt = contest.endsAt?.toUserDateTime(),
        )
        lateinit var startsAt: ValueInput<LocalDateTime?>
        val details = block(size = DETAILS_COLUMNS, title = texts.details()) {
            guide(if (isChangeable) texts.contestDetailsGuide() else texts.contestDetailsGuideLocked())
            if (isChangeable) {
                editing(
                    onSave = {
                        val values = ContestDraft()
                        val isValid = draft.writeBeanIfValid(values)
                        if (isValid) {
                            developerService.editContest(
                                contestId = contest.id,
                                contestName = values.name,
                                startsAt = values.startsAt?.toUserInstant(),
                                endsAt = values.endsAt?.toUserInstant(),
                            )
                            toast(FeedbackKind.Success, currentLocalization().pageDeveloper.contestEdited())
                            show(contest.id)
                        }
                        isValid
                    },
                    onCancel = { draft.readBean(initial) },
                )
            }
            row {
                codeInput(texts.id(), labelSize = 5, size = 6) {
                    value = contest.id.value.toString()
                    isEditable = false
                }
            }
            row {
                textInput(texts.name(), labelSize = 5, size = 19) {
                    draft.forField(this)
                        .nameRules(texts.nameRequired())
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                textInput(texts.trikStudioVersion(), labelSize = 5, size = 8) {
                    value = contest.trikStudioVersion.version
                    isEditable = false
                }
            }
            row {
                textArea(texts.description(), labelSize = 5, size = 19, maxLines = 4) {
                    value = contest.description
                    isEditable = false
                }
            }
            row {
                startsAt = dateTimeInput(texts.startsAt(), labelSize = 5, size = 8) {
                    draft.forField(this).bind({ values -> values.startsAt }, { values, start -> values.startsAt = start })
                }
            }
            row {
                dateTimeInput(texts.endsAt(), labelSize = 5, size = 8) {
                    draft.forField(this)
                        .scheduleRules { startsAt.value }
                        .withValidator({ end -> fitsAttempt(contest.attemptDuration, startsAt.value, end) }, texts.contestLimitExceeds())
                        .bind({ values -> values.endsAt }, { values, end -> values.endsAt = end })
                }
            }
            row {
                textInput(texts.attemptLimit(), labelSize = 5, size = 8) {
                    value = contest.attemptDuration?.let { duration -> texts.contestAttemptMinutes(minutes = duration.toMinutes()) }
                        ?: texts.contestNoAttemptLimit()
                    isEditable = false
                }
            }
        }
        if (!isChangeable) details.isEditable = false

        draft.readBean(initial)
    }

    private fun PageRowScope.accessBlock(contest: ContestVo, shared: List<CommunityVo>, isOwner: Boolean) {
        val texts = currentLocalization().pageDeveloper
        block(title = texts.access()) {
            guide(texts.contestAccessGuide())
            table(key = { community: CommunityVo -> community.id }, fetch = { request -> pageOf(shared, request) }) {
                textColumn(texts.community()) { community -> community.name }
                empty(texts.accessEmpty())
            }
            val sharing = shareDialog(contest, shared)
            actions {
                action(texts.share()) {
                    isEnabled = isOwner
                    onClick { sharing() }
                }
            }
        }
    }

    private fun PageScope.tasksBlock(contest: ContestVo, tasks: List<TaskVo>, me: MultipleRoleUserId, isChangeable: Boolean) {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        row {
            block(title = localization.pageCabinet.sectionTasks()) {
                guide(texts.contestTasksGuide())
                table(key = { task: TaskVo -> task.id }, fetch = { request -> pageOf(tasks, request) }) {
                    codeColumn(texts.id(), size = 5) { task -> task.id.value.toString() }
                    textColumn(texts.name(), size = 12) { task -> task.name }
                    onRowClick(isNavigation = true, isClickable = { task -> task.owner == me }) { task -> openDeveloperTask(task.id) }
                    column(texts.state(), size = 6) { task ->
                        val state = stateOf(task)
                        badge(texts.taskState(state = state.variant), state.tone)
                    }
                    menuColumn(ariaLabel = { task -> texts.contestTaskActions(name = task.name) }) { task ->
                        item(texts.detach(), isEnabled = isChangeable) {
                            developerService.detachTask(contest.id, task.id)
                            toast(FeedbackKind.Success, currentLocalization().pageDeveloper.contestTaskDetached())
                            show(contest.id)
                        }
                    }
                    empty(texts.tasksEmpty(), texts.contestTasksEmptyHint())
                }
                val attaching = attachDialog(contest)
                actions {
                    action(texts.contestAttachTask()) {
                        isEnabled = isChangeable
                        onClick { attaching() }
                    }
                }
            }
        }
    }

    /** Builds the dialog attaching a task available to the developer to [contest] and returns its opening. */
    private fun attachDialog(contest: ContestVo): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        lateinit var chosen: ValueInput<TaskVo?>
        val attaching = dialog(title = texts.contestAttachTitle()) {
            row {
                chosen = lookup(
                    texts.contestAttachLookup(),
                    labelSize = 6,
                    size = 18,
                    fetch = { query, request ->
                        val found = developerService.viewTasks(request.toPagination(), TaskFilter(name = query.ifEmpty { null }))
                        Page(found.content, found.totalElements.toInt())
                    },
                    display = TaskVo::name,
                    columns = {
                        codeColumn(texts.id(), size = 6) { task -> task.id.value.toString() }
                        textColumn(texts.name()) { task -> task.name }
                    },
                    hint = texts.contestAttachHint(),
                    dialogSize = DialogSize.L,
                )
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.attach()) {
                    onClick {
                        chosen.value?.let { task ->
                            val current = currentLocalization().pageDeveloper
                            try {
                                developerService.attachTask(contest.id, task.id)
                                toast(FeedbackKind.Success, current.contestAttachDone())
                                dialog.close()
                                show(contest.id)
                            } catch (exception: OperationException) {
                                toast(
                                    FeedbackKind.Error,
                                    current.contestAttachFailed(),
                                    attachRefusalOf(exception.error) ?: throw exception,
                                )
                            }
                        }
                    }
                }
            }
        }
        return {
            chosen.value = null
            attaching.open()
        }
    }

    /** Builds the sharing dialog of [contest] for the developer communities without access and returns its opening. */
    private fun shareDialog(contest: ContestVo, shared: List<CommunityVo>): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val sharedIds = shared.map { community -> community.id }.toSet()
        val candidates = developerCommunities(multipleRoleUserService).filter { community -> community.id !in sharedIds }
        lateinit var chosen: ValueInput<Set<CommunityVo>>
        val sharing = dialog(title = texts.contestShareTitle()) {
            row {
                chosen = multiSelect(texts.communities(), labelSize = 6, size = 18, items = candidates, itemLabel = CommunityVo::name)
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.shareSubmit()) {
                    onClick {
                        val communities = chosen.value
                        if (communities.isNotEmpty()) {
                            val current = currentLocalization().pageDeveloper
                            dialog.close()
                            confirm(
                                title = current.contestShareConfirmTitle(),
                                text = current.contestShareConfirmText(),
                                action = current.shareSubmit(),
                                isDanger = true,
                            ) {
                                developerService.shareContest(contest.id, communities.map { community -> community.id }.toSet())
                                toast(FeedbackKind.Success, currentLocalization().pageDeveloper.contestShareDone())
                                show(contest.id)
                            }
                        }
                    }
                }
            }
        }
        return {
            chosen.value = emptySet()
            sharing.open()
        }
    }

    private fun delete(contest: ContestVo) {
        val texts = currentLocalization().pageDeveloper
        confirm(
            title = texts.contestDeleteConfirmTitle(name = contest.name),
            text = texts.contestDeleteConfirmText(),
            action = texts.contestDeleteConfirmAction(),
            isDanger = true,
            typeToConfirm = contest.name,
        ) {
            developerService.deleteContest(contest.id)
            toast(FeedbackKind.Success, currentLocalization().pageDeveloper.contestDeleted())
            UI.getCurrent().navigate(DeveloperView::class.java, developerSectionParameters(CONTESTS_SECTION))
        }
    }

    /** Values of the contest editing form. */
    private class ContestDraft(var name: String = "", var startsAt: LocalDateTime? = null, var endsAt: LocalDateTime? = null)
}

/** Returns whether an [attempt] limit fits the contest from [start] to [end]; an open schedule fits any. */
private fun fitsAttempt(attempt: Duration?, start: LocalDateTime?, end: LocalDateTime?): Boolean =
    attempt == null || start == null || end == null || attempt <= Duration.between(start, end)

/** Returns why attaching a task to a contest was refused, or `null` for a refusal the common handler shows. */
private fun attachRefusalOf(error: OperationError): String? {
    val texts = currentLocalization().pageDeveloper
    return when (error) {
        is TaskNotCommittedError -> texts.contestAttachNotCommitted()
        is TaskTrikStudioVersionNotSupportedError -> texts.contestAttachVersionNotSupported()
        is TaskAlreadyAttachedToContestError -> texts.contestAttachAlreadyAttached()
        is ContestAlreadySharedError -> texts.contestAttachShared()
        else -> null
    }
}
