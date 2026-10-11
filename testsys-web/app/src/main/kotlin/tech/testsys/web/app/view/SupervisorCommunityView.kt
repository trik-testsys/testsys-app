package tech.testsys.web.app.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.app.service.supervisor.SupervisorService
import tech.testsys.web.components.texts.UiTexts

/**
 * Community details and applicable management actions for a supervisor.
 *
 * @since %CURRENT_VERSION%
 */
@Route("supervisor/communities/:communityId([0-9]+)")
@PageTitle("Сообщество")
@RolesAllowed("SUPERVISOR")
class SupervisorCommunityView(texts: UiTexts, headers: CabinetHeaders, service: SupervisorService) : CommunityDetailsView(
    texts,
    headers,
    service,
    SupervisorView::class.java,
    SupervisorView::class.java,
    "Кабинет Супервайзера",
    ::openSupervisorUser,
)
