package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.bootstrap.ServiceWebSettings
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.infra.database.api.service.ServiceSupervisorRole
import tech.testsys.infra.database.api.service.ServiceUserId
import tech.testsys.infra.database.api.service.ServiceUserRole
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.infra.database.api.service.SupervisorRoleChange
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.web.app.view.loadTable
import tech.testsys.web.app.view.pageOf
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.tag
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.dateTimeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.overlay.confirm
import tech.testsys.web.components.texts.currentLocalization
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Service card of a user of any kind with its data and communities, without the access code, opening the user in the
 * cabinet of a supervisor and granting or revoking the supervisor role (testsys.dev.service.supervisorRole).
 *
 * @since 1.0.0-beta.1
 */
@Route("users/:$SERVICE_USER_ID_PARAMETER")
@RolesAllowed("SERVICE")
class ServiceUserView(
    private val headers: ServiceHeaders,
    private val search: ServiceUserSearch,
    private val supervisorRole: ServiceSupervisorRole,
    private val runtime: RuntimeSettings,
    private val settings: ServiceWebSettings,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageService.userTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val user = event.routeParameters.get(SERVICE_USER_ID_PARAMETER).orElse(null)
            ?.toLongOrNull()
            ?.let { id -> search.findById(ServiceUserId(id)) }
        if (user == null) {
            event.rerouteToError(NotFoundException::class.java)
            return
        }

        show(user)
    }

    private fun show(user: User<*>) {
        val texts = currentLocalization().pageService
        page(headers.header(active = ServiceHeaders.MENU_SECTION)) {
            head(texts.userHead(name = dataOf(user).name)) { crumb(texts.usersTitle(), ServiceUsersView::class.java) }
            detailsBlock(user)
            if (user !is Participant) communitiesBlock(user)
        }
    }

    private fun PageScope.detailsBlock(user: User<*>) {
        val texts = currentLocalization().pageService
        val isPublicCommunityOwner = user is MultipleRoleUser && user.data.email == PublicCommunityOwner.EMAIL
        val isSupervisor = user is MultipleRoleUser && user.data.roles.any { role -> role is Supervisor }
        row {
            block(title = texts.userDetails()) {
                if (!isPublicCommunityOwner) {
                    actions {
                        if (user is MultipleRoleUser) supervisorRoleAction(user)
                        // A supervisor signs in as any user except supervisors and the owner of the public community.
                        if (!isSupervisor) {
                            action(texts.userOpenSupervisor()) {
                                val target = "${settings.appUrl.trimEnd('/')}/supervisor/users/${user.id.value}"
                                onClick { UI.getCurrent().page.setLocation(target) }
                            }
                        }
                    }
                }
                row {
                    codeInput(texts.id(), labelSize = 3, size = 9) { value = user.id.value.toString() }
                    textInput(texts.nickname(), labelSize = 3, size = 9) { value = dataOf(user).name }
                }
                row { field(texts.roles(), labelSize = 3, size = 21) { roleLabelsOf(user).forEach { role -> tag(role) } } }
                row {
                    dateTimeInput(texts.userCreated(), labelSize = 3, size = 9) { value = user.createdAt.toServerDateTime() }
                    emailOf(user)?.let { email -> textInput(texts.email(), labelSize = 3, size = 9) { value = email } }
                }

                when (user) {
                    is MultipleRoleUser -> Unit
                    is Participant -> row {
                        codeInput(texts.competitionId(), labelSize = 3, size = 9) { value = user.data.competition.id.value.toString() }
                    }
                    is Observer -> row {
                        codeInput(texts.contestIds(), labelSize = 3, size = 9) {
                            value = user.data.contests.ids.joinToString(", ") { contestId -> contestId.value.toString() }
                        }
                    }
                }
            }.isEditable = false
        }
    }

    /** Adds the table of the communities of [user] in ascending id order with its roles in each of them. */
    private fun PageScope.communitiesBlock(user: User<*>) {
        val texts = currentLocalization().pageService
        // Role labels need the UI of this request; the background load reads only the names.
        val roles = rolesByCommunity(user)
        row {
            block(title = texts.userCommunities()) {
                loadTable(
                    key = { community: UserCommunity -> community.id },
                    fetch = { request -> pageOf(communitiesOf(roles), request) },
                ) {
                    codeColumn(texts.id(), size = 5) { community -> community.id.value.toString() }
                    textColumn(texts.userCommunityName(), size = 9) { community -> community.name }
                    column(texts.roles()) { community -> community.roles.forEach { role -> tag(role) } }
                    empty(texts.userCommunitiesEmpty())
                }
            }
        }
    }

    private fun rolesByCommunity(user: User<*>): Map<CommunityId, List<String>> {
        val roles: List<Pair<CommunityId, String>> = when (user) {
            is MultipleRoleUser -> user.data.roles.flatMap { role -> role.memberOf.ids.map { id -> id to roleLabelOf(role) } }
            is Observer -> listOf(user.data.community.id to roleLabel(ServiceUserRole.OBSERVER))
            is Participant -> emptyList()
        }
        return roles.groupBy(keySelector = { (id, _) -> id }, valueTransform = { (_, role) -> role })
    }

    private fun communitiesOf(rolesByCommunity: Map<CommunityId, List<String>>): List<UserCommunity> {
        val names = search.findCommunityNames(rolesByCommunity.keys)
        return rolesByCommunity.entries
            .sortedBy { (id, _) -> id.value }
            .map { (id, communityRoles) -> UserCommunity(id = id, name = names[id], roles = communityRoles) }
    }

    /** Community [id] of a user with its [name], or `null` if it is missing, and the [roles] of the user in it. */
    private data class UserCommunity(val id: CommunityId, val name: String?, val roles: List<String>)

    /** Adds the action that grants the supervisor role to [user] or revokes it, after a confirmation. */
    private fun ContentScope.supervisorRoleAction(user: MultipleRoleUser) {
        val isSupervisor = user.data.roles.any { role -> role is Supervisor }
        val id = ServiceUserId(user.id.value)
        val publicCommunityId = runtime.publicCommunityId
        val texts = currentLocalization().pageService
        if (isSupervisor) {
            action(texts.userSupervisorRevoke()) {
                onClick {
                    confirm(
                        title = texts.userSupervisorRevokeTitle(name = user.data.name),
                        text = texts.userSupervisorRevokeText(),
                        action = texts.userSupervisorRevokeAction(),
                        isDanger = true,
                    ) { report(supervisorRole.revoke(id, publicCommunityId), texts.userSupervisorRevoked()) }
                }
            }
        } else {
            action(texts.userSupervisorGrant()) {
                onClick {
                    confirm(
                        title = texts.userSupervisorGrantTitle(name = user.data.name),
                        text = texts.userSupervisorGrantText(),
                        action = texts.userSupervisorGrantAction(),
                    ) { report(supervisorRole.grant(id, publicCommunityId), texts.userSupervisorGranted()) }
                }
            }
        }
    }

    private fun report(change: SupervisorRoleChange, success: String) {
        when (change) {
            is SupervisorRoleChange.Changed -> {
                toast(FeedbackKind.Success, success)
                show(change.user)
            }
            SupervisorRoleChange.NotFound -> toast(FeedbackKind.Error, currentLocalization().pageService.userNotFound())
            SupervisorRoleChange.NotAllowed -> toast(FeedbackKind.Error, currentLocalization().pageService.userSupervisorNotAllowed())
        }
    }
}

/** Returns this moment in the time zone of the server. */
private fun Instant.toServerDateTime(): LocalDateTime = LocalDateTime.ofInstant(this, ZoneId.systemDefault())
