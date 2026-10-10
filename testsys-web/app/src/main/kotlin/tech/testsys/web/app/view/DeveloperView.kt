package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.TaskFilter
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.DeveloperRoleVo
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.Page
import tech.testsys.web.components.data.filters
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.integerInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textArea
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.DialogSize
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts
import java.time.Duration
import java.time.LocalDateTime

/** Labels of the task states a task list is filtered by. */
private val STATE_FILTER_LABELS: Map<TaskFilter.State, String> = mapOf(
    TaskFilter.State.NEW to TaskState.New.label,
    TaskFilter.State.UNCOMMITTED to TaskState.Uncommitted.label,
    TaskFilter.State.COMMITTED to TaskState.Committed.label,
)

/**
 * Cabinet of a Developer (testsys.web.page.developer): the tabs «Задачи» and «Туры» are the sections of the route; without
 * a section the page opens «Задачи». A task opens its page only for its owner; any contest opens its page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/:section?(tasks|contests)")
@PageTitle("Кабинет Разработчика")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val multipleRoleUserService: MultipleRoleUserService,
    private val currentUser: CurrentUser,
) : TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val section = event.routeParameters.get(DEVELOPER_SECTION_PARAMETER).orElse(null)
        if (section == null) {
            event.forwardTo(DeveloperView::class.java, developerSectionParameters(TASKS_SECTION))
            return
        }

        // Also checks the developer role before the tables load, so that its absence opens the forbidden screen.
        val versions = developerService.viewTrikStudioVersions()
        val communities = developerCommunities(multipleRoleUserService)
        val me = currentUser.multipleRoleUser().id
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("Кабинет Разработчика") {
                crumb("Главная", MultiMainView::class.java)
                tabs(matchRouteParameters = true) {
                    tab("Задачи", DeveloperView::class.java, developerSectionParameters(TASKS_SECTION))
                    tab("Туры", DeveloperView::class.java, developerSectionParameters(CONTESTS_SECTION))
                }
            }
            if (section == CONTESTS_SECTION) {
                contestsBlock(me, communities, versions)
            } else {
                tasksBlock(me, communities, owned = true)
                tasksBlock(me, communities, owned = false)
            }
        }
    }

    private fun PageScope.tasksBlock(me: MultipleRoleUserId, communities: List<CommunityVo>, owned: Boolean) {
        row {
            block(title = if (owned) "Мои задачи" else "Доступные через сообщества") {
                val initial = TaskFilter(ownerId = me.takeIf { owned }, excludeOwnerId = me.takeUnless { owned })
                var applied = initial
                val rows = table(
                    key = { task: TaskVo -> task.id },
                    fetch = { request ->
                        val tasks = developerService.viewTasks(request.toPagination(), applied)
                        Page(tasks.content, tasks.totalElements.toInt())
                    },
                ) {
                    codeColumn("ID", sortKey = "id", size = 5) { task -> task.id.value.toString() }
                    textColumn("Название", sortKey = "name", size = 13) { task -> task.name }
                    if (owned) {
                        onRowClick(isNavigation = true) { task -> openDeveloperTask(task.id) }
                        column("Состояние") { task ->
                            val state = stateOf(task)
                            badge(state.label, state.tone)
                        }
                    } else {
                        column("Сообщества") { task ->
                            communities.filter { community -> community.id in task.sharedTo }.forEach { community -> tag(community.name) }
                        }
                    }
                    empty("Задач пока нет")
                }

                lateinit var name: ValueInput<String>
                lateinit var state: ValueInput<TaskFilter.State?>
                lateinit var community: ValueInput<CommunityVo?>
                filters(
                    onApply = {
                        applied = TaskFilter(
                            name = name.value.ifEmpty { null },
                            ownerId = me.takeIf { owned },
                            excludeOwnerId = me.takeUnless { owned },
                            state = state.value,
                            communityId = community.value?.id,
                        )
                        true
                    },
                    onReset = {
                        name.value = ""
                        state.value = null
                        community.value = null
                        applied = initial
                    },
                    onRefresh = { rows.refresh(toFirstPage = true) },
                ) {
                    row {
                        name = textInput("Название", labelSize = 4, size = 8, hint = "Часть названия, без учёта регистра")
                        state = select(
                            "Состояние",
                            labelSize = 4,
                            size = 8,
                            items = TaskFilter.State.entries,
                            itemLabel = STATE_FILTER_LABELS::getValue,
                        )
                    }
                    row {
                        community = select("Сообщество", labelSize = 4, size = 8, items = communities, itemLabel = CommunityVo::name)
                    }
                }

                if (owned) {
                    val creation = taskDialog { rows.refresh() }
                    actions { action("Создать задачу") { onClick { creation() } } }
                }
            }
        }
    }

    private fun PageScope.contestsBlock(me: MultipleRoleUserId, communities: List<CommunityVo>, versions: List<TrikStudioVersion>) {
        row {
            block(title = "Туры") {
                var applied = ContestFilter(ownerId = me)
                val rows = table(
                    key = { (contest, _): Pair<ContestVo, List<CommunityVo>> -> contest.id },
                    fetch = { request ->
                        val contests = developerService.viewContests(request.toPagination(), applied)
                        Page(contests.content, contests.totalElements.toInt())
                    },
                ) {
                    codeColumn("ID", sortKey = "id", size = 3) { (contest, _) -> contest.id.value.toString() }
                    textColumn("Название", sortKey = "name", size = 5) { (contest, _) -> contest.name }
                    numberColumn("Задачи", size = 2) { (contest, _) -> contest.tasks.size }
                    textColumn("Доступ предоставлен", size = 3) { (contest, _) -> if (contest.sharedTo.isEmpty()) "Нет" else "Да" }
                    dateTimeColumn("Начало", size = 4) { (contest, _) -> contest.startsAt?.toServerDateTime() }
                    dateTimeColumn("Конец", size = 4) { (contest, _) -> contest.endsAt?.toServerDateTime() }
                    textColumn("Время на прохождение") { (contest, _) -> studyDurationText(contest.attemptDuration) }
                    empty("Туров пока нет", "Создайте тур.")
                    onRowClick(isNavigation = true) { (contest, _) -> openDeveloperContest(contest.id) }
                }

                lateinit var name: ValueInput<String>
                lateinit var community: ValueInput<CommunityVo?>
                filters(
                    onApply = {
                        applied = ContestFilter(
                            name = name.value.ifEmpty { null },
                            ownerId = me,
                            communityId = community.value?.id,
                        )
                        true
                    },
                    onReset = {
                        name.value = ""
                        community.value = null
                        applied = ContestFilter(ownerId = me)
                    },
                    onRefresh = { rows.refresh(toFirstPage = true) },
                ) {
                    row {
                        name = textInput("Название", labelSize = 4, size = 8, hint = "Часть названия, без учёта регистра")
                        community = select("Сообщество", labelSize = 4, size = 8, items = communities, itemLabel = CommunityVo::name)
                    }
                }

                val creation = contestDialog(versions) { rows.refresh() }
                actions { action("Создать тур") { onClick { creation() } } }
            }
        }
    }

    /** Builds the task creation dialog and returns its opening; [onCreated] refreshes the current list after creation. */
    private fun taskDialog(onCreated: () -> Unit): () -> Unit {
        val draft = Binder<TaskDraft>()
        val creation = dialog(title = "Новая задача") {
            row {
                textInput("Название", labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules("Укажите название")
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                textArea("Описание", labelSize = 6, size = 18, maxLines = 4) {
                    draft.forField(this)
                        .bind({ values -> values.description }, { values, description -> values.description = description })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Создать") {
                    onClick {
                        val values = TaskDraft()
                        if (draft.writeBeanIfValid(values)) {
                            developerService.createTask(taskName = values.name, taskDescription = values.description)
                            dialog.close()
                            toast(FeedbackKind.Success, "Задача создана")
                            onCreated()
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(TaskDraft())
            creation.open()
        }
    }

    /** Builds the contest creation dialog of [versions] and returns its opening, which starts with an empty form. */
    private fun contestDialog(versions: List<TrikStudioVersion>, onCreated: () -> Unit): () -> Unit {
        val draft = Binder<ContestDraft>()
        lateinit var startsAt: ValueInput<LocalDateTime?>
        lateinit var endsAt: ValueInput<LocalDateTime?>
        val creation = dialog(title = "Новый тур", size = DialogSize.L) {
            row {
                textInput("Название", labelSize = 7, size = 17) {
                    draft.forField(this)
                        .nameRules("Укажите название")
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                select("Версия TRIK Studio", labelSize = 7, size = 17, items = versions, itemLabel = TrikStudioVersion::version) {
                    draft.forField(this)
                        .asRequired("Выберите версию")
                        .bind({ values -> values.version }, { values, version -> values.version = version })
                }
            }
            row {
                textArea("Описание", labelSize = 7, size = 17, maxLines = 4) {
                    draft.forField(this)
                        .bind({ values -> values.description }, { values, description -> values.description = description })
                }
            }
            row {
                startsAt = dateTimeInput("Начало", labelSize = 7, size = 17) {
                    draft.forField(this).bind({ values -> values.startsAt }, { values, start -> values.startsAt = start })
                }
            }
            row {
                endsAt = dateTimeInput("Конец", labelSize = 7, size = 17) {
                    draft.forField(this)
                        .scheduleRules { startsAt.value }
                        .bind({ values -> values.endsAt }, { values, end -> values.endsAt = end })
                }
            }
            row {
                integerInput("Индивидуальный лимит", labelSize = 7, size = 17, min = 1, unit = "мин", hint = "Время на прохождение") {
                    draft.forField(this)
                        .withValidator(
                            { minutes -> fitsSchedule(minutes, startsAt.value, endsAt.value) },
                            "Лимит больше длительности тура",
                        )
                        .bind({ values -> values.attemptMinutes }, { values, minutes -> values.attemptMinutes = minutes })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Создать") {
                    onClick {
                        val values = ContestDraft()
                        if (draft.writeBeanIfValid(values)) {
                            developerService.createContest(
                                contestName = values.name,
                                trikStudioVersion = checkNotNull(values.version),
                                attemptDuration = values.attemptMinutes?.let { minutes -> Duration.ofMinutes(minutes.toLong()) },
                                startsAt = values.startsAt?.toServerInstant(),
                                endsAt = values.endsAt?.toServerInstant(),
                                contestDescription = values.description,
                            )
                            dialog.close()
                            toast(FeedbackKind.Success, "Тур создан")
                            onCreated()
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(ContestDraft())
            creation.open()
        }
    }

    /** Values of the task creation form. */
    private class TaskDraft(var name: String = "", var description: String = "")

    /** Values of the contest creation form. */
    private class ContestDraft {
        var name: String = ""
        var version: TrikStudioVersion? = null
        var description: String = ""
        var startsAt: LocalDateTime? = null
        var endsAt: LocalDateTime? = null
        var attemptMinutes: Int? = null
    }
}

/** Returns the communities of the developer role of the current user, which a list is filtered by and access is shared to. */
internal fun developerCommunities(service: MultipleRoleUserService): List<CommunityVo> =
    service.viewProfile().roles.filter { (role, _) -> role is DeveloperRoleVo }.flatMap { (_, communities) -> communities }

/** Returns whether an attempt of [minutes] fits the contest from [start] to [end]; an open schedule fits any. */
private fun fitsSchedule(minutes: Int?, start: LocalDateTime?, end: LocalDateTime?): Boolean =
    minutes == null || start == null || end == null || Duration.ofMinutes(minutes.toLong()) <= Duration.between(start, end)
