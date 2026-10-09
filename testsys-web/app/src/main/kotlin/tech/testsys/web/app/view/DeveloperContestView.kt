package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.destructiveAction
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.alert
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
import tech.testsys.web.components.texts.UiTexts
import java.time.Duration
import java.time.LocalDateTime

/** Columns of the contest details block; the access block takes the rest of the row beside it. */
private const val DETAILS_COLUMNS = 14

/**
 * Contest of a Developer (testsys.web.page.developer.contest): its details, tasks and access of communities. Only the
 * owner changes the contest, and only until access is shared; otherwise the page explains why it cannot be changed.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/contests/:contestId([0-9]+)")
@PageTitle("Тур")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperContestView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val multipleRoleUserService: MultipleRoleUserService,
    private val currentUser: CurrentUser,
) : TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        show(ContestId(event.routeParameters.getLong(CONTEST_ID_PARAMETER).orElseThrow()))
    }

    private fun show(contestId: ContestId) {
        val (contest, tasks, shared) = developerService.viewContest(contestId)
        val me = currentUser.multipleRoleUser().id
        val isOwner = contest.owner == me
        val isChangeable = isOwner && contest.sharedTo.isEmpty()
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Тур «${contest.name}»") {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Разработчика", DeveloperView::class.java)
                crumb("Туры", DeveloperView::class.java, developerSectionParameters(CONTESTS_SECTION))
                actions {
                    destructiveAction("Удалить тур") {
                        isEnabled = isChangeable
                        onClick { delete(contest) }
                    }
                }
            }
            if (!isChangeable) {
                val reason = if (isOwner) {
                    "Доступ к туру предоставлен сообществам, поэтому тур нельзя изменить или удалить, " +
                        "а задачи — прикрепить к нему или открепить."
                } else {
                    "Изменять тур может только его владелец."
                }
                row { block { row { alert(kind = FeedbackKind.Info, title = "Тур нельзя изменить", text = reason) } } }
            }
            row {
                detailsBlock(contest, isChangeable)
                accessBlock(contest, shared, isOwner)
            }
            tasksBlock(contest, tasks, me, isChangeable)
        }
    }

    private fun PageRowScope.detailsBlock(contest: ContestVo, isChangeable: Boolean) {
        val draft = Binder<ContestDraft>()
        val initial = ContestDraft(
            name = contest.name,
            startsAt = contest.startsAt?.toServerDateTime(),
            endsAt = contest.endsAt?.toServerDateTime(),
        )
        lateinit var startsAt: ValueInput<LocalDateTime?>
        val details = block(size = DETAILS_COLUMNS, title = "Сведения") {
            if (isChangeable) {
                editing(
                    onSave = {
                        val values = ContestDraft()
                        val isValid = draft.writeBeanIfValid(values)
                        if (isValid) {
                            developerService.editContest(
                                contestId = contest.id,
                                contestName = values.name,
                                startsAt = values.startsAt?.toServerInstant(),
                                endsAt = values.endsAt?.toServerInstant(),
                            )
                            show(contest.id)
                        }
                        isValid
                    },
                    onCancel = { draft.readBean(initial) },
                )
            }
            row {
                codeInput("ID", labelSize = 7, size = 17) {
                    value = contest.id.value.toString()
                    isEditable = false
                }
            }
            row {
                textInput("Название", labelSize = 7, size = 17) {
                    draft.forField(this)
                        .nameRules("Укажите название")
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                textInput("Версия TRIK Studio", labelSize = 7, size = 17) {
                    value = contest.trikStudioVersion.version
                    isEditable = false
                }
            }
            row {
                textArea("Описание", labelSize = 7, size = 17) {
                    value = contest.description
                    isEditable = false
                }
            }
            row {
                startsAt = dateTimeInput("Начало", labelSize = 7, size = 17) {
                    draft.forField(this).bind({ values -> values.startsAt }, { values, start -> values.startsAt = start })
                }
            }
            row {
                dateTimeInput("Конец", labelSize = 7, size = 17) {
                    draft.forField(this)
                        .scheduleRules { startsAt.value }
                        .withValidator(
                            { end -> fitsAttempt(contest.attemptDuration, startsAt.value, end) },
                            "Индивидуальный лимит больше длительности тура",
                        )
                        .bind({ values -> values.endsAt }, { values, end -> values.endsAt = end })
                }
            }
            row {
                textInput("Индивидуальный лимит", labelSize = 7, size = 17) {
                    value = contest.attemptDuration?.let { duration -> "${duration.toMinutes()} мин" } ?: "Нет"
                    isEditable = false
                }
            }
        }
        if (!isChangeable) details.isEditable = false

        draft.readBean(initial)
    }

    private fun PageRowScope.accessBlock(contest: ContestVo, shared: List<CommunityVo>, isOwner: Boolean) {
        block(title = "Доступ") {
            table(key = { community: CommunityVo -> community.id }, fetch = { request -> pageOf(shared, request) }) {
                textColumn("Сообщество") { community -> community.name }
                empty("Доступ не предоставлен")
            }
            val sharing = shareDialog(contest, shared)
            actions {
                action("Предоставить доступ") {
                    isEnabled = isOwner
                    onClick { sharing() }
                }
            }
        }
    }

    private fun PageScope.tasksBlock(contest: ContestVo, tasks: List<TaskVo>, me: MultipleRoleUserId, isChangeable: Boolean) {
        row {
            block(title = "Задачи") {
                table(key = { task: TaskVo -> task.id }, fetch = { request -> pageOf(tasks, request) }) {
                    codeColumn("ID", size = 5) { task -> task.id.value.toString() }
                    column("Название", size = 12) { task ->
                        if (task.owner == me) linkAction(task.name) { onClick { openDeveloperTask(task.id) } } else text(task.name)
                    }
                    column("Состояние", size = 6) { task ->
                        val state = stateOf(task)
                        badge(state.label, state.tone)
                    }
                    menuColumn(ariaLabel = { task -> "Действия с задачей «${task.name}»" }) { task ->
                        item("Открепить", isEnabled = isChangeable) {
                            developerService.detachTask(contest.id, task.id)
                            show(contest.id)
                        }
                    }
                    empty("Задач пока нет", "Прикрепите задачу, у которой есть зафиксированная версия.")
                }
                val attaching = attachDialog(contest)
                actions {
                    action("Прикрепить задачу") {
                        isEnabled = isChangeable
                        onClick { attaching() }
                    }
                }
            }
        }
    }

    /** Builds the dialog attaching a task available to the developer to [contest] and returns its opening. */
    private fun attachDialog(contest: ContestVo): () -> Unit {
        lateinit var chosen: ValueInput<TaskVo?>
        val attaching = dialog(title = "Прикрепление задачи") {
            row {
                chosen = lookup(
                    "Задача",
                    labelSize = 6,
                    size = 18,
                    fetch = { query, request ->
                        val found = developerService.viewTasks(request.toPagination(), TaskFilter(name = query.ifEmpty { null }))
                        Page(found.content, found.totalElements.toInt())
                    },
                    display = TaskVo::name,
                    columns = {
                        codeColumn("ID", size = 6) { task -> task.id.value.toString() }
                        textColumn("Название") { task -> task.name }
                    },
                    hint = "В туре используется последняя зафиксированная версия задачи",
                    dialogSize = DialogSize.L,
                )
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Прикрепить") {
                    onClick {
                        chosen.value?.let { task ->
                            developerService.attachTask(contest.id, task.id)
                            dialog.close()
                            show(contest.id)
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
        val sharedIds = shared.map { community -> community.id }.toSet()
        val candidates = developerCommunities(multipleRoleUserService).filter { community -> community.id !in sharedIds }
        lateinit var chosen: ValueInput<Set<CommunityVo>>
        val sharing = dialog(title = "Доступ к туру") {
            row {
                chosen = multiSelect("Сообщества", labelSize = 6, size = 18, items = candidates, itemLabel = CommunityVo::name)
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Предоставить") {
                    onClick {
                        val communities = chosen.value
                        if (communities.isNotEmpty()) {
                            dialog.close()
                            confirm(
                                title = "Предоставить доступ к туру?",
                                text = "Отозвать доступ нельзя. После этого тур нельзя будет изменить и удалить.",
                                action = "Предоставить",
                                isDanger = true,
                            ) {
                                developerService.shareContest(contest.id, communities.map { community -> community.id }.toSet())
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
        confirm(
            title = "Удалить тур «${contest.name}»?",
            text = "Тур удалится вместе со списком его задач. Сами задачи не изменятся.",
            action = "Удалить",
            isDanger = true,
        ) {
            developerService.deleteContest(contest.id)
            UI.getCurrent().navigate(DeveloperView::class.java, developerSectionParameters(CONTESTS_SECTION))
        }
    }

    /** Values of the contest editing form. */
    private class ContestDraft(var name: String = "", var startsAt: LocalDateTime? = null, var endsAt: LocalDateTime? = null)
}

/** Returns whether an [attempt] limit fits the contest from [start] to [end]; an open schedule fits any. */
private fun fitsAttempt(attempt: Duration?, start: LocalDateTime?, end: LocalDateTime?): Boolean =
    attempt == null || start == null || end == null || attempt <= Duration.between(start, end)
