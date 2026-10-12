package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.router.RouteParameters
import jakarta.annotation.security.RolesAllowed
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.web.app.service.AdminUserVo
import tech.testsys.web.app.service.CommunityVo
import tech.testsys.web.app.service.supervisor.SupervisorService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.data.table
import tech.testsys.web.components.texts.currentLocalization

/** Opens the community details of [communityId] in the Supervisor cabinet. */
internal fun openSupervisorCommunity(communityId: CommunityId) {
    UI.getCurrent().navigate(SupervisorCommunityView::class.java, communityParameters(communityId))
}

/** Opens the user details of [user] in the Supervisor cabinet. */
internal fun openSupervisorUser(user: AdminUserVo) {
    UI.getCurrent().navigate(SupervisorUserView::class.java, RouteParameters(USER_ID_PARAMETER, user.id.value.toString()))
}

/**
 * Supervisor cabinet with all non-supervisor users and all communities.
 *
 * @since %CURRENT_VERSION%
 */
@Route("supervisor/:section?(users|communities)")
@RolesAllowed("SUPERVISOR")
class SupervisorView(
    private val headers: CabinetHeaders,
    private val service: SupervisorService,
) : TestSysView(),
    BeforeEnterObserver,
    HasDynamicTitle {
    override fun getPageTitle(): String = currentLocalization().pageSupervisor.title()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val section = event.routeParameters.get(ADMIN_SECTION_PARAMETER).orElse(null)
        if (section == null) {
            event.forwardTo(SupervisorView::class.java, RouteParameters(ADMIN_SECTION_PARAMETER, "users"))
            return
        }
        val communities = service.viewCommunities()
        val localization = currentLocalization()
        val texts = localization.pageSupervisor
        page(headers.cabinet(active = CabinetHeaders.MENU_SECTION)) {
            head(texts.title()) {
                crumb(localization.pageCabinet.home(), SupervisorView::class.java)
                tabs(matchRouteParameters = true) {
                    tab(
                        localization.pageCabinet.sectionUsers(),
                        SupervisorView::class.java,
                        RouteParameters(ADMIN_SECTION_PARAMETER, "users"),
                    )
                    tab(
                        localization.pageCabinet.sectionCommunities(),
                        SupervisorView::class.java,
                        RouteParameters(ADMIN_SECTION_PARAMETER, "communities"),
                    )
                }
            }
            guide(texts.guide())
            if (section == "users") {
                row {
                    block(title = localization.pageCabinet.sectionUsers()) {
                        guide(texts.usersGuide())
                        adminUsersTable(service, communities.map { it.first }, community = null, openUser = ::openSupervisorUser)
                    }
                }
            } else {
                row {
                    block(title = localization.pageCabinet.sectionCommunities()) {
                        guide(texts.communitiesGuide())
                        table(
                            key = { (community, _): Pair<CommunityVo, Long> -> community.id },
                            fetch = { request -> pageOf(service.viewCommunities(), request) },
                        ) {
                            codeColumn(texts.id(), size = 4) { (community, _) -> community.id.value.toString() }
                            textColumn(texts.name(), size = 7) { (community, _) -> community.name }
                            textColumn(texts.description(), size = 9) { (community, _) -> community.description }
                            numberColumn(texts.communitiesUsers()) { (_, count) -> count }
                            empty(texts.communitiesEmpty())
                            onRowClick(isNavigation = true) { (community, _) -> openSupervisorCommunity(community.id) }
                        }
                    }
                }
            }
        }
    }
}
