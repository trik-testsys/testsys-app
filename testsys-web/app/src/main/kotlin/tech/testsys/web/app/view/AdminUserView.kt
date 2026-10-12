package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
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
import tech.testsys.infra.localization.PageAdmin.UserHeadKind
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.infra.localization.PageCabinet.ValidationStatusStatus
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
import tech.testsys.web.components.texts.currentLocalization
import java.time.Instant

/**
 * Tab of the page of a user with non-fixed roles with the sections of one role.
 *
 * @property value the value of the route parameter of the tab.
 * @property role the message variant of the role, which names the tab.
 */
internal enum class AdminUserSection(val value: String, val role: RoleNameRole) {
    /** Tasks and contests of a developer. */
    Developer("developer", RoleNameRole.DEVELOPER),

    /** Classes and competitions of a manager. */
    Manager("manager", RoleNameRole.MANAGER),

    /** Judgment orders of a judge. */
    Judge("judge", RoleNameRole.JUDGE),
}

/** Message variants naming the roles an administrator grants. */
private val GRANTED_ROLE_NAMES: Map<CommunityRole, RoleNameRole> = mapOf(
    CommunityRole.Developer to RoleNameRole.DEVELOPER,
    CommunityRole.Manager to RoleNameRole.MANAGER,
    CommunityRole.Student to RoleNameRole.STUDENT,
)

/**
 * User available to an Administrator (testsys.web.page.admin.user): its common data, its roles in the communities of the
 * administrator with granting and removal, and the sections of its roles on tabs with their own addresses. An observer
 * has its own address, shows its section below the common data and can be deleted.
 *
 * @since %CURRENT_VERSION%
 */
@Route("admin/users/:userId([0-9]+)/:section?(developer|manager|judge)")
@RouteAlias("admin/observers/:observerId([0-9]+)")
@RolesAllowed("MULTIPLE_ROLE")
class AdminUserView(
    private val headers: CabinetHeaders,
    private val administratorService: AdministratorService,
    private val communityConfig: CommunityConfig,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageAdmin.userTitle()

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

        val localization = currentLocalization()
        val texts = localization.pageAdmin
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.userHead(kind = if (user is ObserverVo) UserHeadKind.OBSERVER else UserHeadKind.USER, name = user.name)) {
                crumb(localization.pageCabinet.home(), MultiMainView::class.java)
                crumb(texts.title(), AdminView::class.java)
                crumb(localization.pageCabinet.sectionUsers(), AdminView::class.java, RouteParameters(ADMIN_SECTION_PARAMETER, "users"))
                if (sections.isNotEmpty()) {
                    tabs(matchRouteParameters = true) {
                        tab(texts.details(), AdminUserView::class.java, userParameters(userId))
                        sections.forEach { entry ->
                            val label = localization.pageCabinet.roleName(role = entry.role)
                            tab(label, AdminUserView::class.java, userParameters(userId, entry))
                        }
                    }
                }
            }
            guide(texts.userGuide())
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
        val texts = currentLocalization().pageAdmin
        row {
            block(title = texts.details()) {
                guide(texts.userDetailsGuide())
                row {
                    codeInput(texts.id(), labelSize = 3, size = 6) { value = user.id.value.toString() }
                    textInput(texts.nickname(), labelSize = 3, size = 8) { value = user.name }
                }
                row { dateTimeInput(texts.lastLogin(), labelSize = 3, size = 6) { value = lastLogin?.toUserDateTime() } }
                if (user is ObserverVo) {
                    row {
                        codeInput(texts.accessCode(), labelSize = 3, size = 6) {
                            value = user.accessTokenHash.value
                            isObscured = true
                        }
                    }
                    actions { destructiveAction(texts.userObserverDelete()) { onClick { deleteObserver(user) } } }
                }
            }.isEditable = false
        }
    }

    private fun PageScope.rolesBlock(user: AdminUserVo, communities: List<CommunityVo>) {
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        val memberships = communities.mapNotNull { community ->
            when (user) {
                is MultipleRoleUserVo -> user.roles.filter { role -> community.id in role.memberOf }
                    .takeIf { roles -> roles.isNotEmpty() }
                    ?.let { roles -> Membership(community, roles.map(::labelOf), roles) }
                is ObserverVo -> Membership(community, listOf(localization.pageCabinet.roleName(role = RoleNameRole.OBSERVER)), emptyList())
                    .takeIf { user.community == community.id }
            }
        }
        row {
            block(title = texts.userRolesTitle()) {
                guide(texts.userRolesGuide())
                table(key = { membership: Membership -> membership.community.id }, fetch = { request -> pageOf(memberships, request) }) {
                    textColumn(texts.userCommunity(), size = 12) { membership -> membership.community.name }
                    column(texts.roles()) { membership -> membership.labels.forEach { label -> tag(label) } }
                    if (user is MultipleRoleUserVo) {
                        menuColumn(
                            ariaLabel = { membership -> texts.userRolesActions(community = membership.community.name) },
                        ) { membership -> removalItems(user, membership, communities) }
                    }
                    empty(texts.userRolesEmpty())
                    onRowClick(isNavigation = true) { membership -> openAdminCommunity(membership.community.id) }
                }
                if (user is MultipleRoleUserVo) {
                    val granting = grantDialog(user, communities)
                    actions { action(texts.userRolesGrant()) { onClick { granting() } } }
                }
            }
        }
    }

    /** Adds an item removing [user] from the community of [membership] in each of its roles, or a disabled explanation. */
    private fun MenuScope.removalItems(user: MultipleRoleUserVo, membership: Membership, communities: List<CommunityVo>) {
        val texts = currentLocalization().pageAdmin
        val removable = membership.roles.mapNotNull { role -> communityRoleOf(role)?.let { removed -> removed to labelOf(role) } }
        if (membership.community.id == communityConfig.publicCommunityId || removable.isEmpty()) {
            item(texts.userRolesNothingToRemove(), isEnabled = false) {}
            return
        }

        removable.forEach { (role, label) ->
            destructiveItem(texts.userRolesRemove(role = label)) {
                confirm(
                    title = texts.userRolesRemoveTitle(user = user.name, community = membership.community.name, role = label),
                    text = texts.userRolesRemoveText(),
                    action = texts.userRolesRemoveAction(),
                    isDanger = true,
                ) {
                    removeFromCommunity(user, membership.community.id, role, communities)
                    val shown = currentLocalization().pageAdmin
                    toast(FeedbackKind.Success, shown.userRolesRemoved(), shown.userRole(role = label))
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
        val texts = currentLocalization().pageAdmin
        confirm(
            title = texts.userObserverDeleteTitle(name = observer.name),
            text = texts.userObserverDeleteText(),
            action = texts.userObserverDeleteAction(),
            isDanger = true,
            typeToConfirm = observer.name,
        ) {
            administratorService.deleteObserver(observer.id)
            openAdminCommunity(observer.community)
            toast(FeedbackKind.Success, currentLocalization().pageAdmin.userObserverDeleted())
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
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        val tasks = administratorService.viewUserTasks(userId)
        row {
            block(title = localization.pageCabinet.sectionTasks()) {
                guide(texts.userTasksGuide())
                table(key = { (task, _, _) -> task.id }, fetch = { request -> pageOf(tasks, request) }) {
                    textColumn(texts.name(), size = 10) { (task, _, _) -> task.name }
                    column(texts.userTasksStatus(), size = 6) { (_, request, _) ->
                        val (text, tone) = validationStatusOf(request?.execution)
                        badge(text, tone)
                    }
                    textColumn(texts.userTasksCommunities()) { (_, _, shared) -> shared.joinToString(", ") { community -> community.name } }
                    empty(texts.userTasksEmpty())
                }
            }
        }
    }

    private fun PageScope.contestsBlock(userId: UserId) {
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        val contests = administratorService.viewUserContests(userId)
        row {
            block(title = localization.pageCabinet.sectionContests()) {
                guide(texts.userContestsGuide())
                table(key = { (contest, _) -> contest.id }, fetch = { request -> pageOf(contests, request) }) {
                    textColumn(texts.name(), size = 10) { (contest, _) -> contest.name }
                    numberColumn(texts.userContestsTasks(), size = 3) { (_, tasks) -> tasks.size }
                    column(texts.userContestsSolutions()) { (_, tasks) ->
                        tasks.forEach { (task, submissions) ->
                            tag(task.name)
                            counter(Math.toIntExact(submissions), CounterKind.Neutral)
                        }
                    }
                    empty(texts.userContestsEmpty())
                }
            }
        }
    }

    private fun PageScope.classesBlock(userId: UserId) {
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        val classes = administratorService.viewUserClasses(userId)
        row {
            block(title = localization.pageCabinet.sectionClasses()) {
                guide(texts.userClassesGuide())
                table(key = { (group, _) -> group.id }, fetch = { request -> pageOf(classes, request) }) {
                    textColumn(texts.name(), size = 9) { (group, _) -> group.name }
                    numberColumn(texts.userClassesStudents(), size = 5) { (group, _) -> group.students.size }
                    numberColumn(texts.userSolutions(), size = 5) { (_, count) -> count.submissions }
                    numberColumn(texts.userClassesAuthors()) { (_, count) -> count.authors }
                    empty(texts.userClassesEmpty())
                }
            }
        }
    }

    private fun PageScope.competitionsBlock(userId: UserId) {
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        val competitions = administratorService.viewUserCompetitions(userId)
        row {
            block(title = localization.pageCabinet.sectionCompetitions()) {
                guide(texts.userCompetitionsGuide())
                table(key = { (competition, _) -> competition.id }, fetch = { request -> pageOf(competitions, request) }) {
                    textColumn(texts.name(), size = 9) { (competition, _) -> competition.name }
                    numberColumn(texts.userCompetitionsParticipants(), size = 5) { (competition, _) -> competition.participants.size }
                    numberColumn(texts.userSolutions(), size = 5) { (_, count) -> count.submissions }
                    numberColumn(texts.userCompetitionsAuthors()) { (_, count) -> count.authors }
                    empty(texts.userCompetitionsEmpty())
                }
            }
        }
    }

    private fun PageScope.judgmentsBlock(userId: UserId) {
        val texts = currentLocalization().pageAdmin
        val judgments = administratorService.viewUserJudgments(userId)
        row {
            block(title = texts.userJudgmentsTitle()) {
                guide(texts.userJudgmentsGuide())
                table(key = { (order, _, _) -> order.id }, fetch = { request -> pageOf(judgments, request) }) {
                    codeColumn(texts.userJudgmentsSolution(), size = 6) { (_, solution, _) -> solution.value.toString() }
                    dateTimeColumn(texts.userJudgmentsIssued(), size = 6) { (order, _, _) -> order.createdAt.toUserDateTime() }
                    numberColumn(texts.userJudgmentsPreviousResult(), size = 6) { (_, _, previous) -> previous }
                    numberColumn(texts.userJudgmentsNewResult()) { (order, _, _) -> order.score.value }
                    empty(texts.userJudgmentsEmpty())
                }
            }
        }
    }

    private fun PageScope.assignedContestsBlock(userId: UserId) {
        val texts = currentLocalization().pageAdmin
        val contests = administratorService.viewUserAssignedContests(userId)
        row {
            block(title = texts.userAssignedTitle()) {
                guide(texts.userAssignedGuide())
                table(key = { (contest, _) -> contest.id }, fetch = { request -> pageOf(contests, request) }) {
                    textColumn(texts.userAssignedContest(), size = 10) { (contest, _) -> contest.name }
                    textColumn(texts.userAssignedCompetitions()) { (_, competitions) ->
                        competitions.joinToString(", ") { competition -> competition.name }
                    }
                    empty(texts.userAssignedEmpty())
                }
            }
        }
    }

    /** Builds the role granting dialog of [user] and returns its opening, which starts with an empty form. */
    private fun grantDialog(user: MultipleRoleUserVo, communities: List<CommunityVo>): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageAdmin
        val draft = Binder<GrantDraft>()
        val granting = dialog(title = texts.userGrantTitle()) {
            row {
                select(texts.userCommunity(), labelSize = 6, size = 18, items = communities, itemLabel = CommunityVo::name) {
                    draft.forField(this)
                        .asRequired(texts.userGrantCommunityRequired())
                        .bind({ values -> values.community }, { values, community -> values.community = community })
                }
            }
            row {
                val roles = GRANTED_ROLE_NAMES.keys.toList()
                select(
                    texts.userGrantRole(),
                    labelSize = 6,
                    size = 18,
                    items = roles,
                    itemLabel = { role -> localization.pageCabinet.roleName(role = GRANTED_ROLE_NAMES.getValue(role)) },
                ) {
                    draft.forField(this)
                        .asRequired(texts.userGrantRoleRequired())
                        .bind({ values -> values.role }, { values, role -> values.role = role })
                }
            }
            footer { dialog ->
                action(localization.uiDialog.cancel()) { onClick { dialog.close() } }
                mainAction(texts.userGrantSubmit()) {
                    onClick {
                        val values = GrantDraft()
                        if (draft.writeBeanIfValid(values)) {
                            administratorService.grantRole(user.id, checkNotNull(values.community).id, checkNotNull(values.role))
                            val shown = currentLocalization()
                            toast(
                                FeedbackKind.Success,
                                shown.pageAdmin.userGrantGranted(),
                                shown.pageAdmin.userRole(
                                    role = shown.pageCabinet.roleName(role = GRANTED_ROLE_NAMES.getValue(checkNotNull(values.role))),
                                ),
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
internal fun validationStatusOf(execution: TaskValidationExecutionVo?): Pair<String, Tone> {
    val (status, tone) = when (execution) {
        null -> ValidationStatusStatus.NOT_STARTED to Tone.Neutral
        TaskValidationExecutionVo.PendingDiagnostics,
        is TaskValidationExecutionVo.AwaitingSubmissions,
        is TaskValidationExecutionVo.SubmissionsCreated,
        -> ValidationStatusStatus.RUNNING to Tone.Info
        is TaskValidationExecutionVo.StoppedByDiagnostics -> ValidationStatusStatus.FAILURE to Tone.Danger
        is TaskValidationExecutionVo.Completed -> if (execution.failures.isEmpty()) {
            ValidationStatusStatus.SUCCESS to Tone.Success
        } else {
            ValidationStatusStatus.FAILURE to Tone.Danger
        }
        is TaskValidationExecutionVo.IncompleteDiagnostics,
        is TaskValidationExecutionVo.CompletedDiagnostics,
        is TaskValidationExecutionVo.CreatedSubmissions,
        -> ValidationStatusStatus.STOPPED to Tone.Warning
    }
    return currentLocalization().pageCabinet.validationStatus(status = status) to tone
}
