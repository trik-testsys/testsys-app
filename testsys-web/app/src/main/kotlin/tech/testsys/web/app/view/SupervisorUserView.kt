package tech.testsys.web.app.view

import com.vaadin.flow.data.binder.Binder
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.infra.localization.PageSupervisor.UserHeadKind
import tech.testsys.operation.config.CommunityConfig
import tech.testsys.web.app.service.AdminUserVo
import tech.testsys.web.app.service.AdministratorRoleVo
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.DeveloperRoleVo
import tech.testsys.web.app.service.JudgeRoleVo
import tech.testsys.web.app.service.ManagerRoleVo
import tech.testsys.web.app.service.MultipleRoleUserVo
import tech.testsys.web.app.service.ObserverVo
import tech.testsys.web.app.service.ParticipantVo
import tech.testsys.web.app.service.supervisor.SupervisorService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.data.table
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.select
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization
import java.time.Instant

/** Message variants naming the community roles a supervisor grants. */
private val COMMUNITY_ROLE_NAMES: Map<CommunityRole, RoleNameRole> = mapOf(
    CommunityRole.Administrator to RoleNameRole.ADMINISTRATOR,
    CommunityRole.Manager to RoleNameRole.MANAGER,
    CommunityRole.Developer to RoleNameRole.DEVELOPER,
    CommunityRole.Student to RoleNameRole.STUDENT,
    CommunityRole.Judge to RoleNameRole.JUDGE,
)

/**
 * Details of any non-supervisor user, with membership actions only for users with non-fixed roles.
 *
 * @since %CURRENT_VERSION%
 */
@Route("supervisor/users/:userId([0-9]+)/:section?(developer|manager|judge)")
@RolesAllowed("SUPERVISOR")
class SupervisorUserView(
    private val headers: CabinetHeaders,
    private val service: SupervisorService,
    private val communityConfig: CommunityConfig,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageSupervisor.userTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val id = event.routeParameters.getLong(USER_ID_PARAMETER).orElseThrow()
        val section = event.routeParameters.get(ADMIN_USER_SECTION_PARAMETER).orElse(null)
            ?.let { value -> AdminUserSection.entries.first { it.value == value } }
        if (!show(id, section)) {
            event.forwardTo(SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, id.toString()))
        }
    }

    /** Shows a section; returns false if the selected user does not hold the requested role. */
    private fun show(id: Long, section: AdminUserSection? = null): Boolean {
        val (user, login) = service.viewUserByIdValue(id)
        val communities = service.viewCommunities().map { it.first }
        val sections = if (user is MultipleRoleUserVo) {
            AdminUserSection.entries.filter { entry ->
                user.roles.any { role ->
                    when (entry) {
                        AdminUserSection.Developer -> role is DeveloperRoleVo
                        AdminUserSection.Manager -> role is ManagerRoleVo
                        AdminUserSection.Judge -> role is JudgeRoleVo
                    }
                }
            }
        } else {
            emptyList()
        }
        if (section != null && section !in sections) {
            return false
        }
        val localization = currentLocalization()
        val texts = localization.pageSupervisor
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.userHead(kind = headKindOf(user), name = user.name)) {
                crumb(localization.pageCabinet.home(), SupervisorView::class.java)
                crumb(texts.title(), SupervisorView::class.java)
                crumb(
                    localization.pageCabinet.sectionUsers(),
                    SupervisorView::class.java,
                    RouteParameters(ADMIN_SECTION_PARAMETER, "users"),
                )
                if (sections.isNotEmpty()) {
                    tabs(matchRouteParameters = true) {
                        tab(texts.details(), SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, id.toString()))
                        sections.forEach { entry ->
                            tab(
                                localization.pageCabinet.roleName(role = entry.role),
                                SupervisorUserView::class.java,
                                RouteParameters(
                                    mapOf(USER_ID_PARAMETER to id.toString(), ADMIN_USER_SECTION_PARAMETER to entry.value),
                                ),
                            )
                        }
                    }
                }
            }
            guide(texts.userGuide())
            when (section) {
                AdminUserSection.Developer -> {
                    tasksBlock(service, user.id)
                    contestsBlock(service, user.id)
                }
                AdminUserSection.Manager -> {
                    classesBlock(service, user.id)
                    competitionsBlock(service, user.id)
                }
                AdminUserSection.Judge -> judgmentsBlock(service, user.id)
                null -> {
                    detailsBlock(user, login)
                    if (user is ObserverVo) {
                        assignedContestsBlock(service, user.id)
                    }
                }
            }
            if (section == null && user is MultipleRoleUserVo) {
                rolesBlock(id, user, communities)
            } else if (section == null && user is ObserverVo) {
                row {
                    block(title = texts.userCommunityTitle()) {
                        guide(texts.userCommunityGuide())
                        val community = communities.firstOrNull { it.id == user.community }
                        row {
                            textInput(texts.name(), labelSize = 6, size = 18) {
                                value = community?.name.orEmpty()
                                isEditable = false
                            }
                        }
                    }
                }
            } else if (section == null && user is ParticipantVo) {
                row {
                    block(title = texts.userParticipantTitle()) {
                        guide(texts.userParticipantGuide())
                        row {
                            codeInput(texts.id(), labelSize = 6, size = 18) {
                                value = user.competition.value.toString()
                                isEditable = false
                            }
                        }
                    }
                }
            }
        }
        return true
    }

    @RawAccessTokenDependency(reason = "Shows the original access code of any user while Identity hashing is in use.")
    private fun PageScope.detailsBlock(user: AdminUserVo, login: Instant?) {
        val texts = currentLocalization().pageSupervisor
        row {
            block(title = texts.details()) {
                guide(texts.userDetailsGuide())
                row {
                    codeInput(texts.id(), labelSize = 3, size = 6) {
                        value = user.id.value.toString()
                        isEditable = false
                    }
                    textInput(texts.nickname(), labelSize = 3, size = 8) {
                        value = user.name
                        isEditable = false
                    }
                }
                row {
                    dateTimeInput(texts.lastLogin(), labelSize = 3, size = 6) {
                        value = login?.toUserDateTime()
                        isEditable = false
                    }
                }
                row {
                    codeInput(texts.accessCode(), labelSize = 3, size = 6) {
                        value = user.accessTokenHash.value
                        isObscured = true
                        isEditable = false
                    }
                }
            }
        }
    }

    /** Shows the community roles of [user] with their removal, the inclusion into a community and the administrator grant. */
    private fun PageScope.rolesBlock(id: Long, user: MultipleRoleUserVo, communities: List<CommunityVo>) {
        val texts = currentLocalization().pageSupervisor
        row {
            block(title = texts.userRolesTitle()) {
                guide(texts.userRolesGuide())
                val memberships = communities.mapNotNull { community ->
                    user.roles.filter { community.id in it.memberOf }.takeIf { it.isNotEmpty() }
                        ?.let { roles -> community to roles }
                }
                table(
                    key = { (community, _) -> community.id },
                    fetch = { request -> pageOf(memberships, request) },
                ) {
                    textColumn(texts.userRolesCommunity(), size = 12) { (community, _) -> community.name }
                    column(texts.roles()) { (_, roles) -> roles.forEach { role -> tag(labelOf(role)) } }
                    menuColumn(
                        ariaLabel = { (community, _) -> texts.userRolesActions(community = community.name) },
                    ) { (community, roles) ->
                        val removable = roles.mapNotNull { role -> communityRoleOf(role)?.let { it to labelOf(role) } }
                        if (community.id == communityConfig.publicCommunityId || removable.isEmpty()) {
                            item(texts.userRolesNothingToRemove(), isEnabled = false) {}
                        } else {
                            removable.forEach { (role, label) ->
                                destructiveItem(texts.userRolesRemove(role = label)) {
                                    confirm(
                                        title = texts.userRolesRemoveTitle(user = user.name, community = community.name, role = label),
                                        text = texts.userRolesRemoveText(),
                                        action = texts.userRolesRemoveAction(),
                                        isDanger = true,
                                    ) {
                                        service.removeFromCommunity(user.id, community.id, role)
                                        val shown = currentLocalization().pageSupervisor
                                        toast(FeedbackKind.Success, shown.userRolesRemoved(), shown.userRole(role = label))
                                        show(id)
                                    }
                                }
                            }
                        }
                    }
                    empty(texts.userRolesEmpty())
                    onRowClick(isNavigation = true) { (community, _) -> openSupervisorCommunity(community.id) }
                }
                val add = addDialog(user, communities) { show(id) }
                actions {
                    action(texts.userRolesGrant()) { onClick { add() } }
                    if (user.roles.none { it is AdministratorRoleVo }) {
                        action(texts.userAdministratorGrant()) {
                            onClick {
                                confirm(
                                    title = texts.userAdministratorGrantTitle(name = user.name),
                                    text = texts.userAdministratorGrantText(),
                                    action = texts.userAdministratorGrantAction(),
                                ) {
                                    service.grantAdministrator(user.id)
                                    toast(FeedbackKind.Success, currentLocalization().pageSupervisor.userAdministratorGranted())
                                    show(id)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addDialog(user: MultipleRoleUserVo, communities: List<CommunityVo>, onAdded: () -> Unit): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageSupervisor
        val draft = Binder<MembershipDraft>()
        val dialog = dialog(title = texts.userGrantTitle()) {
            row {
                select(texts.userGrantCommunity(), labelSize = 6, size = 18, items = communities, itemLabel = CommunityVo::name) {
                    draft.forField(this).asRequired(texts.userGrantCommunityRequired())
                        .bind({ it.community }, { bean, selected -> bean.community = selected })
                }
            }
            row {
                select(
                    texts.userGrantRole(),
                    labelSize = 6,
                    size = 18,
                    items = CommunityRole.entries,
                    itemLabel = { role -> localization.pageCabinet.roleName(role = COMMUNITY_ROLE_NAMES.getValue(role)) },
                ) {
                    draft.forField(this).asRequired(texts.userGrantRoleRequired())
                        .bind({ it.role }, { bean, selected -> bean.role = selected })
                }
            }
            footer { window ->
                action(localization.uiOverlay.dialogCancel()) { onClick { window.close() } }
                mainAction(texts.userGrantSubmit()) {
                    onClick {
                        val values = MembershipDraft()
                        if (draft.writeBeanIfValid(values)) {
                            service.addToCommunity(user.id, checkNotNull(values.community).id, checkNotNull(values.role))
                            toast(FeedbackKind.Success, currentLocalization().pageSupervisor.userGrantGranted())
                            window.close()
                            onAdded()
                        }
                    }
                }
            }
        }
        return {
            draft.readBean(MembershipDraft())
            dialog.open()
        }
    }

    private fun headKindOf(user: AdminUserVo): UserHeadKind = when (user) {
        is MultipleRoleUserVo -> UserHeadKind.USER
        is ObserverVo -> UserHeadKind.OBSERVER
        is ParticipantVo -> UserHeadKind.PARTICIPANT
    }

    private class MembershipDraft {
        var community: CommunityVo? = null
        var role: CommunityRole? = null
    }
}
