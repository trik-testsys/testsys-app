package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
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
import tech.testsys.web.components.texts.currentLocalization
import java.time.Duration
import java.time.LocalDateTime

/** Task states a task list is filtered by. */
private val FILTER_STATES: Map<TaskFilter.State, TaskState> = mapOf(
    TaskFilter.State.NEW to TaskState.New,
    TaskFilter.State.UNCOMMITTED to TaskState.Uncommitted,
    TaskFilter.State.COMMITTED to TaskState.Committed,
)

/**
 * Cabinet of a Developer (testsys.web.page.developer): the tabs «Задачи» and «Туры» are the sections of the route; without
 * a section the page opens «Задачи». A task opens its page only for its owner; any contest opens its page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("developer/:section?(tasks|contests)")
@RolesAllowed("MULTIPLE_ROLE")
class DeveloperView(
    private val headers: CabinetHeaders,
    private val developerService: DeveloperService,
    private val multipleRoleUserService: MultipleRoleUserService,
    private val currentUser: CurrentUser,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageDeveloper.title()

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
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.title()) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                tabs(matchRouteParameters = true) {
                    tab(localization.pageCabinet.sectionTasks(), DeveloperView::class.java, developerSectionParameters(TASKS_SECTION))
                    tab(localization.pageCabinet.sectionContests(), DeveloperView::class.java, developerSectionParameters(CONTESTS_SECTION))
                }
            }
            guide(texts.guide())
            if (section == CONTESTS_SECTION) {
                contestsBlock(me, communities, versions)
            } else {
                tasksBlock(me, communities, owned = false)
                tasksBlock(me, communities, owned = true)
            }
        }
    }

    private fun PageScope.tasksBlock(me: MultipleRoleUserId, communities: List<CommunityVo>, owned: Boolean) {
        val texts = currentLocalization().pageDeveloper
        row {
            block(title = if (owned) texts.tasksOwned() else texts.tasksShared()) {
                guide(texts.tasksGuide())
                val initial = TaskFilter(ownerId = me.takeIf { owned }, excludeOwnerId = me.takeUnless { owned })
                var applied = initial
                val rows = table(
                    key = { task: TaskVo -> task.id },
                    fetch = { request ->
                        val tasks = developerService.viewTasks(request.toPagination(), applied)
                        Page(tasks.content, tasks.totalElements.toInt())
                    },
                ) {
                    codeColumn(texts.id(), sortKey = "id", size = 5) { task -> task.id.value.toString() }
                    textColumn(texts.name(), sortKey = "name", size = 13) { task -> task.name }
                    if (owned) {
                        onRowClick(isNavigation = true) { task -> openDeveloperTask(task.id) }
                        column(texts.state()) { task ->
                            val state = stateOf(task)
                            badge(texts.taskState(state = state.variant), state.tone)
                        }
                    } else {
                        column(texts.communities()) { task ->
                            communities.filter { community -> community.id in task.sharedTo }.forEach { community -> tag(community.name) }
                        }
                    }
                    empty(texts.tasksEmpty())
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
                        name = textInput(texts.name(), labelSize = 4, size = 8, hint = texts.nameHint())
                        state = select(
                            texts.state(),
                            labelSize = 4,
                            size = 8,
                            items = TaskFilter.State.entries,
                            itemLabel = { filtered -> texts.taskState(state = FILTER_STATES.getValue(filtered).variant) },
                        )
                    }
                    row {
                        community = select(texts.community(), labelSize = 4, size = 8, items = communities, itemLabel = CommunityVo::name)
                    }
                }

                if (owned) {
                    val creation = taskDialog { rows.refresh() }
                    actions { action(texts.tasksCreate()) { onClick { creation() } } }
                }
            }
        }
    }

    private fun PageScope.contestsBlock(me: MultipleRoleUserId, communities: List<CommunityVo>, versions: List<TrikStudioVersion>) {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        row {
            block(title = localization.pageCabinet.sectionContests()) {
                guide(texts.contestsGuide())
                var applied = ContestFilter(ownerId = me)
                val rows = table(
                    key = { (contest, _): Pair<ContestVo, List<CommunityVo>> -> contest.id },
                    fetch = { request ->
                        val contests = developerService.viewContests(request.toPagination(), applied)
                        Page(contests.content, contests.totalElements.toInt())
                    },
                ) {
                    codeColumn(texts.id(), sortKey = "id", size = 3) { (contest, _) -> contest.id.value.toString() }
                    textColumn(texts.name(), sortKey = "name", size = 5) { (contest, _) -> contest.name }
                    numberColumn(texts.contestsTasks(), size = 2) { (contest, _) -> contest.tasks.size }
                    textColumn(texts.contestsShared(), size = 3) { (contest, _) ->
                        if (contest.sharedTo.isEmpty()) texts.contestsSharedNo() else texts.contestsSharedYes()
                    }
                    dateTimeColumn(texts.startsAt(), size = 4) { (contest, _) -> contest.startsAt?.toUserDateTime() }
                    dateTimeColumn(texts.endsAt(), size = 4) { (contest, _) -> contest.endsAt?.toUserDateTime() }
                    textColumn(texts.attemptDuration()) { (contest, _) -> studyDurationText(contest.attemptDuration) }
                    empty(texts.contestsEmpty(), texts.contestsEmptyHint())
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
                        name = textInput(texts.name(), labelSize = 4, size = 8, hint = texts.nameHint())
                        community = select(texts.community(), labelSize = 4, size = 8, items = communities, itemLabel = CommunityVo::name)
                    }
                }

                val creation = contestDialog(versions) { rows.refresh() }
                actions { action(texts.contestsCreate()) { onClick { creation() } } }
            }
        }
    }

    /** Builds the task creation dialog and returns its opening; [onCreated] refreshes the current list after creation. */
    private fun taskDialog(onCreated: () -> Unit): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val draft = Binder<TaskDraft>()
        val creation = dialog(title = texts.tasksDialogTitle()) {
            row {
                textInput(texts.name(), labelSize = 6, size = 18) {
                    draft.forField(this)
                        .nameRules(texts.nameRequired())
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                textArea(texts.description(), labelSize = 6, size = 18, maxLines = 4) {
                    draft.forField(this)
                        .bind({ values -> values.description }, { values, description -> values.description = description })
                }
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.create()) {
                    onClick {
                        val values = TaskDraft()
                        if (draft.writeBeanIfValid(values)) {
                            developerService.createTask(taskName = values.name, taskDescription = values.description)
                            dialog.close()
                            toast(FeedbackKind.Success, currentLocalization().pageDeveloper.tasksCreated())
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
        val localization = currentLocalization()
        val texts = localization.pageDeveloper
        val draft = Binder<ContestDraft>()
        lateinit var startsAt: ValueInput<LocalDateTime?>
        lateinit var endsAt: ValueInput<LocalDateTime?>
        val creation = dialog(title = texts.contestsDialogTitle(), size = DialogSize.L) {
            row {
                textInput(texts.name(), labelSize = 7, size = 17) {
                    draft.forField(this)
                        .nameRules(texts.nameRequired())
                        .bind({ values -> values.name }, { values, name -> values.name = name })
                }
            }
            row {
                select(texts.trikStudioVersion(), labelSize = 7, size = 17, items = versions, itemLabel = TrikStudioVersion::version) {
                    draft.forField(this)
                        .asRequired(texts.contestsVersionRequired())
                        .bind({ values -> values.version }, { values, version -> values.version = version })
                }
            }
            row {
                textArea(texts.description(), labelSize = 7, size = 17, maxLines = 4) {
                    draft.forField(this)
                        .bind({ values -> values.description }, { values, description -> values.description = description })
                }
            }
            row {
                startsAt = dateTimeInput(texts.startsAt(), labelSize = 7, size = 17) {
                    draft.forField(this).bind({ values -> values.startsAt }, { values, start -> values.startsAt = start })
                }
            }
            row {
                endsAt = dateTimeInput(texts.endsAt(), labelSize = 7, size = 17) {
                    draft.forField(this)
                        .scheduleRules { startsAt.value }
                        .bind({ values -> values.endsAt }, { values, end -> values.endsAt = end })
                }
            }
            row {
                integerInput(
                    texts.attemptLimit(),
                    labelSize = 7,
                    size = 17,
                    min = 1,
                    unit = texts.minutesUnit(),
                    hint = texts.attemptDuration(),
                ) {
                    draft.forField(this)
                        .withValidator({ minutes -> fitsSchedule(minutes, startsAt.value, endsAt.value) }, texts.contestsLimitExceeds())
                        .bind({ values -> values.attemptMinutes }, { values, minutes -> values.attemptMinutes = minutes })
                }
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.create()) {
                    onClick {
                        val values = ContestDraft()
                        if (draft.writeBeanIfValid(values)) {
                            developerService.createContest(
                                contestName = values.name,
                                trikStudioVersion = checkNotNull(values.version),
                                attemptDuration = values.attemptMinutes?.let { minutes -> Duration.ofMinutes(minutes.toLong()) },
                                startsAt = values.startsAt?.toUserInstant(),
                                endsAt = values.endsAt?.toUserInstant(),
                                contestDescription = values.description,
                            )
                            dialog.close()
                            toast(FeedbackKind.Success, currentLocalization().pageDeveloper.contestsCreated())
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
