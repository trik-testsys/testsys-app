package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteAlias
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.service.AdminUserVo
import tech.testsys.web.app.service.AdministratorRoleVo
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.DeveloperRoleVo
import tech.testsys.web.app.service.JudgeRoleVo
import tech.testsys.web.app.service.ManagerRoleVo
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.ObserverVo
import tech.testsys.web.app.service.RoleVo
import tech.testsys.web.app.service.StudentRoleVo
import tech.testsys.web.app.service.administrator.AdministratorService
import tech.testsys.web.app.service.developer.TaskValidationExecutionVo
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.destructiveAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.CounterKind
import tech.testsys.web.components.display.Tone
import tech.testsys.web.components.display.badge
import tech.testsys.web.components.display.counter
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.MenuScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.UiTexts
import java.time.Instant

/**
 * Tab of the page of a user with non-fixed roles with the sections of one role.
 *
 * @property value the value of the route parameter of the tab.
 * @property label the label of the tab.
 */
internal enum class AdminUserSection(val value: String, val label: String) {
    /** Tasks and contests of a developer. */
    Developer("developer", "Разработчик"),

    /** Classes and competitions of a manager. */
    Manager("manager", "Организатор"),

    /** Judgment orders of a judge. */
    Judge("judge", "Судья"),
}

/** Labels of the roles an administrator grants. */
private val GRANTED_ROLE_LABELS: Map<CommunityRole, String> = mapOf(
    CommunityRole.Developer to "Разработчик",
    CommunityRole.Manager to "Организатор",
    CommunityRole.Student to "Ученик",
)

/**
 * User available to an Administrator (testsys.web.page.admin.user): the common data of the user, the roles in the
 * communities of the administrator with granting and removal, the sections of its roles and deleting an observer. The
 * sections of each role of a user with non-fixed roles are on a tab of the page with its own address; without a section
 * the page opens the common data. Observers have their own address and show their section below the common data.
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/users/:userId([0-9]+)/:section?(developer|manager|judge)")
@RouteAlias("admin/observers/:observerId([0-9]+)")
@PageTitle("Пользователь")
@RolesAllowed("MULTIPLE_ROLE")
class AdminUserView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val administratorService: AdministratorService,
    private val communityConfig: CommunityConfig,
) : TestSysView(texts),
    BeforeEnterObserver {
    override fun beforeEnter(event: BeforeEnterEvent) {
        val parameters = event.routeParameters
        val userId: UserId = parameters.getLong(USER_ID_PARAMETER).map<UserId>(::MultipleRoleUserId)
            .orElseGet { SingleRoleUserId(parameters.getLong(OBSERVER_ID_PARAMETER).orElseThrow()) }
        val section = parameters.get(ADMIN_USER_SECTION_PARAMETER).orElse(null)
            ?.let { value -> AdminUserSection.entries.first { entry -> entry.value == value } }
        show(userId, section)?.let { parameters -> event.forwardTo(AdminUserView::class.java, parameters) }
    }

    /**
     * Shows [section] of [userId], or its common data without one; returns the parameters of the common data instead if
     * the user has no such section.
     */
    private fun show(userId: UserId, section: AdminUserSection? = null): RouteParameters? {
        val (user, lastLogin) = administratorService.viewUser(userId)
        val communities = administratorService.viewCommunities().map { (community, _) -> community }
        val sections = sectionsOf(user, communities.map { community -> community.id }.toSet())
        if (section != null && section !in sections) return userParameters(userId)

        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head("${if (user is ObserverVo) "Наблюдатель" else "Пользователь"} «${user.name}»") {
                crumb("Главная", MultiMainView::class.java)
                crumb("Кабинет Администратора", AdminView::class.java)
                crumb("Пользователи", AdminView::class.java, RouteParameters(ADMIN_SECTION_PARAMETER, "users"))
                if (sections.isNotEmpty()) {
                    tabs(matchRouteParameters = true) {
                        tab("Сведения", AdminUserView::class.java, userParameters(userId))
                        sections.forEach { entry -> tab(entry.label, AdminUserView::class.java, userParameters(userId, entry)) }
                    }
                }
            }
            when (section) {
                null -> {
                    detailsBlock(user, lastLogin)
                    rolesBlock(user, communities)
                    if (user is ObserverVo) assignedContestsBlock(user.id)
                }
                AdminUserSection.Developer -> {
                    tasksBlock(userId)
                    contestsBlock(userId)
                }
                AdminUserSection.Manager -> {
                    classesBlock(userId)
                    competitionsBlock(userId)
                }
                AdminUserSection.Judge -> judgmentsBlock(userId)
            }
        }
        return null
    }

    @RawAccessTokenDependency(reason = "Shows the stored access code of an observer as the issued one.")
    private fun PageScope.detailsBlock(user: AdminUserVo, lastLogin: Instant?) {
        row {
            block(title = "Сведения") {
                row {
                    codeInput("ID", labelSize = 3, size = 6) { value = user.id.value.toString() }
                    textInput("Псевдоним", labelSize = 3, size = 8) { value = user.name }
                }
                row { dateTimeInput("Последний вход", labelSize = 3, size = 6) { value = lastLogin?.toServerDateTime() } }
                if (user is ObserverVo) {
                    row {
                        codeInput("Код-доступа", labelSize = 3, size = 6) {
                            value = user.accessTokenHash.value
                            isObscured = true
                        }
                    }
                    actions { destructiveAction("Удалить наблюдателя") { onClick { deleteObserver(user) } } }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.rolesBlock(user: AdminUserVo, communities: List<CommunityVo>) {
        val memberships = communities.mapNotNull { community ->
            when (user) {
                is MultipleRoleUserVo -> user.roles.filter { role -> community.id in role.memberOf }
                    .takeIf { roles -> roles.isNotEmpty() }
                    ?.let { roles -> Membership(community, roles.map(::labelOf), roles) }
                is ObserverVo -> Membership(community, listOf("Наблюдатель"), emptyList()).takeIf { user.community == community.id }
            }
        }
        row {
            block(title = "Роли в сообществах") {
                table(key = { membership: Membership -> membership.community.id }, fetch = { request -> pageOf(memberships, request) }) {
                    textColumn("Сообщество", size = 12) { membership -> membership.community.name }
                    column("Роли") { membership -> membership.labels.forEach { label -> tag(label) } }
                    if (user is MultipleRoleUserVo) {
                        menuColumn(ariaLabel = { membership -> "Роли в сообществе «${membership.community.name}»" }) { membership ->
                            removalItems(user, membership, communities)
                        }
                    }
                    empty("Ролей в ваших сообществах нет")
                    onRowClick(isNavigation = true) { membership -> openAdminCommunity(membership.community.id) }
                }
                if (user is MultipleRoleUserVo) {
                    val granting = grantDialog(user, communities)
                    actions { action("Включить в сообщество") { onClick { granting() } } }
                }
            }
        }
    }

    /** Adds an item removing [user] from the community of [membership] in each of its roles, or a disabled explanation. */
    private fun MenuScope.removalItems(user: MultipleRoleUserVo, membership: Membership, communities: List<CommunityVo>) {
        val removable = membership.roles.mapNotNull { role -> communityRoleOf(role)?.let { removed -> removed to labelOf(role) } }
        if (membership.community.id == communityConfig.publicCommunityId || removable.isEmpty()) {
            item("Нет ролей для исключения", isEnabled = false) {}
            return
        }

        removable.forEach { (role, label) ->
            destructiveItem("Исключить в роли $label") {
                confirm(
                    title = "Исключить «${user.name}» из сообщества «${membership.community.name}» в роли $label?",
                    text = "Пользователь потеряет доступ к тому, что было доступно ему только через это сообщество в этой роли.",
                    action = "Исключить",
                    isDanger = true,
                ) {
                    removeFromCommunity(user, membership.community.id, role, communities)
                    toast(FeedbackKind.Success, "Пользователь исключён из сообщества", "Роль: $label.")
                }
            }
        }
    }

    /**
     * Removes [user] from [communityId] in [role] and shows the user again, or the users of the administrator once the user
     * is no longer a member of any of [communities].
     */
    private fun removeFromCommunity(
        user: MultipleRoleUserVo,
        communityId: CommunityId,
        role: CommunityRole,
        communities: List<CommunityVo>,
    ) {
        val updated = administratorService.removeFromCommunity(user.id, communityId, role)
        val isAvailable = communities.any { community ->
            community.owner == updated.id || updated.roles.any { held -> community.id in held.memberOf }
        }
        if (isAvailable) {
            show(updated.id)
        } else {
            UI.getCurrent().navigate(AdminView::class.java, RouteParameters(ADMIN_SECTION_PARAMETER, "users"))
        }
    }

    private fun deleteObserver(observer: ObserverVo) {
        confirm(
            title = "Удалить наблюдателя «${observer.name}»?",
            text = "Код-доступа наблюдателя перестанет действовать.",
            action = "Удалить",
            isDanger = true,
            typeToConfirm = observer.name,
        ) {
            administratorService.deleteObserver(observer.id)
            openAdminCommunity(observer.community)
            toast(FeedbackKind.Success, "Наблюдатель удалён")
        }
    }

    /** Returns the sections of the roles [user] holds in [communities] of the administrator, in the order of the tabs. */
    private fun sectionsOf(user: AdminUserVo, communities: Set<CommunityId>): List<AdminUserSection> {
        if (user !is MultipleRoleUserVo) return emptyList()

        val roles = user.roles.filter { role -> role.memberOf.any { communityId -> communityId in communities } }
        return AdminUserSection.entries.filter { section ->
            roles.any { role ->
                when (section) {
                    AdminUserSection.Developer -> role is DeveloperRoleVo
                    AdminUserSection.Manager -> role is ManagerRoleVo
                    AdminUserSection.Judge -> role is JudgeRoleVo
                }
            }
        }
    }

    private fun PageScope.tasksBlock(userId: UserId) {
        val tasks = administratorService.viewUserTasks(userId)
        row {
            block(title = "Задачи") {
                table(key = { (task, _, _) -> task.id }, fetch = { request -> pageOf(tasks, request) }) {
                    textColumn("Название", size = 10) { (task, _, _) -> task.name }
                    column("Статус проверки", size = 6) { (_, request, _) ->
                        val (text, tone) = validationStatusOf(request?.execution)
                        badge(text, tone)
                    }
                    textColumn("Сообщества") { (_, _, shared) -> shared.joinToString(", ") { community -> community.name } }
                    empty("Задач нет")
                }
            }
        }
    }

    private fun PageScope.contestsBlock(userId: UserId) {
        val contests = administratorService.viewUserContests(userId)
        row {
            block(title = "Туры") {
                table(key = { (contest, _) -> contest.id }, fetch = { request -> pageOf(contests, request) }) {
                    textColumn("Название", size = 10) { (contest, _) -> contest.name }
                    numberColumn("Задачи", size = 3) { (_, tasks) -> tasks.size }
                    column("Решения по задачам") { (_, tasks) ->
                        tasks.forEach { (task, submissions) ->
                            tag(task.name)
                            counter(Math.toIntExact(submissions), CounterKind.Neutral)
                        }
                    }
                    empty("Туров нет")
                }
            }
        }
    }

    private fun PageScope.classesBlock(userId: UserId) {
        val classes = administratorService.viewUserClasses(userId)
        row {
            block(title = "Классы") {
                table(key = { (group, _) -> group.id }, fetch = { request -> pageOf(classes, request) }) {
                    textColumn("Название", size = 9) { (group, _) -> group.name }
                    numberColumn("Ученики", size = 5) { (group, _) -> group.students.size }
                    numberColumn("Решения", size = 5) { (_, count) -> count.submissions }
                    numberColumn("Ученики с решениями") { (_, count) -> count.authors }
                    empty("Классов нет")
                }
            }
        }
    }

    private fun PageScope.competitionsBlock(userId: UserId) {
        val competitions = administratorService.viewUserCompetitions(userId)
        row {
            block(title = "Соревнования") {
                table(key = { (competition, _) -> competition.id }, fetch = { request -> pageOf(competitions, request) }) {
                    textColumn("Название", size = 9) { (competition, _) -> competition.name }
                    numberColumn("Участники", size = 5) { (competition, _) -> competition.participants.size }
                    numberColumn("Решения", size = 5) { (_, count) -> count.submissions }
                    numberColumn("Участники с решениями") { (_, count) -> count.authors }
                    empty("Соревнований нет")
                }
            }
        }
    }

    private fun PageScope.judgmentsBlock(userId: UserId) {
        val judgments = administratorService.viewUserJudgments(userId)
        row {
            block(title = "Судейские вердикты") {
                table(key = { (order, _, _) -> order.id }, fetch = { request -> pageOf(judgments, request) }) {
                    codeColumn("Решение", size = 6) { (_, solution, _) -> solution.value.toString() }
                    dateTimeColumn("Выставлен", size = 6) { (order, _, _) -> order.createdAt.toServerDateTime() }
                    numberColumn("Предыдущий результат", size = 6) { (_, _, previous) -> previous }
                    numberColumn("Новый результат") { (order, _, _) -> order.score.value }
                    empty("Судейских вердиктов нет")
                }
            }
        }
    }

    private fun PageScope.assignedContestsBlock(userId: UserId) {
        val contests = administratorService.viewUserAssignedContests(userId)
        row {
            block(title = "Назначенные туры") {
                table(key = { (contest, _) -> contest.id }, fetch = { request -> pageOf(contests, request) }) {
                    textColumn("Тур", size = 10) { (contest, _) -> contest.name }
                    textColumn("Соревнования") { (_, competitions) -> competitions.joinToString(", ") { competition -> competition.name } }
                    empty("Назначенных туров нет")
                }
            }
        }
    }

    /** Builds the role granting dialog of [user] and returns its opening, which starts with an empty form. */
    private fun grantDialog(user: MultipleRoleUserVo, communities: List<CommunityVo>): () -> Unit {
        val draft = Binder<GrantDraft>()
        val granting = dialog(title = "Включение в сообщество") {
            row {
                select("Сообщество", labelSize = 6, size = 18, items = communities, itemLabel = CommunityVo::name) {
                    draft.forField(this)
                        .asRequired("Выберите сообщество")
                        .bind({ values -> values.community }, { values, community -> values.community = community })
                }
            }
            row {
                val roles = GRANTED_ROLE_LABELS.keys.toList()
                select("Роль", labelSize = 6, size = 18, items = roles, itemLabel = GRANTED_ROLE_LABELS::getValue) {
                    draft.forField(this)
                        .asRequired("Выберите роль")
                        .bind({ values -> values.role }, { values, role -> values.role = role })
                }
            }
            footer { dialog ->
                action("Отменить") { onClick { dialog.close() } }
                mainAction("Включить") {
                    onClick {
                        val values = GrantDraft()
                        if (draft.writeBeanIfValid(values)) {
                            administratorService.grantRole(user.id, checkNotNull(values.community).id, checkNotNull(values.role))
                            toast(
                                FeedbackKind.Success,
                                "Пользователь включён в сообщество",
                                "Роль: ${GRANTED_ROLE_LABELS.getValue(checkNotNull(values.role))}.",
                            )
                            dialog.close()
                            show(user.id)
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(GrantDraft())
            granting.open()
        }
    }

    /** Roles of the viewed user in one community of the administrator: their [labels] and the removable [roles]. */
    private class Membership(val community: CommunityVo, val labels: List<String>, val roles: List<RoleVo>)

    /** Values of the role granting form. */
    private class GrantDraft {
        var community: CommunityVo? = null
        var role: CommunityRole? = null
    }
}

/** Returns the community role of [role], or `null` for the administrator role, which an administrator does not remove. */
private fun communityRoleOf(role: RoleVo): CommunityRole? = when (role) {
    is AdministratorRoleVo -> null
    is DeveloperRoleVo -> CommunityRole.Developer
    is ManagerRoleVo -> CommunityRole.Manager
    is JudgeRoleVo -> CommunityRole.Judge
    is StudentRoleVo -> CommunityRole.Student
}

/**
 * Returns the text and tone of the status of the latest validation [execution] of a task, or that testing was never
 * started without one.
 */
internal fun validationStatusOf(execution: TaskValidationExecutionVo?): Pair<String, Tone> = when (execution) {
    null -> "Не запускалась" to Tone.Neutral
    TaskValidationExecutionVo.PendingDiagnostics,
    is TaskValidationExecutionVo.AwaitingSubmissions,
    is TaskValidationExecutionVo.SubmissionsCreated,
    -> "Идёт" to Tone.Info
    is TaskValidationExecutionVo.StoppedByDiagnostics -> "Неуспех" to Tone.Danger
    is TaskValidationExecutionVo.Completed -> if (execution.failures.isEmpty()) "Успех" to Tone.Success else "Неуспех" to Tone.Danger
    is TaskValidationExecutionVo.IncompleteDiagnostics,
    is TaskValidationExecutionVo.CompletedDiagnostics,
    is TaskValidationExecutionVo.CreatedSubmissions,
    -> "Техническая остановка" to Tone.Warning
}
