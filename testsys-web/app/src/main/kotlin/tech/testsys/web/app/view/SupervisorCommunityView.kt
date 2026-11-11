package tech.testsys.web.app.view

import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.app.service.supervisor.SupervisorService
import tech.testsys.web.components.texts.currentLocalization

/**
 * Community details and applicable management actions for a supervisor.
 *
 * @since 1.0.0-beta.1
 */
@Route("supervisor/communities/:communityId([0-9]+)")
@RolesAllowed("SUPERVISOR")
class SupervisorCommunityView(headers: CabinetHeaders, service: SupervisorService) : CommunityDetailsView(
    headers,
    service,
    SupervisorView::class.java,
    SupervisorView::class.java,
    currentLocalization().pageSupervisor.title(),
    ::openSupervisorUser,
)
