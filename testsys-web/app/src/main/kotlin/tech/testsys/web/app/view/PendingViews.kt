package tech.testsys.web.app.view

import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.texts.currentLocalization

/**
 * Cabinet page whose content is not implemented yet: the Cabinet header with the [active] section, the head and browser
 * tab [title] and an empty state. The routes and access of the pages are final, so the header can lead to them; the
 * optional route parameter `section` selects the section of a role page that the menu leads to.
 *
 * @since %CURRENT_VERSION%
 */
abstract class PendingCabinetView(headers: CabinetHeaders, private val title: String, active: String?) :
    TestSysView(), HasDynamicTitle {
    init {
        val texts = currentLocalization().pageCabinet
        page(headers.cabinet(active)) {
            head(title)
            guide(texts.pendingGuide())
            row {
                block {
                    guide(texts.pendingBlockGuide(), title = texts.pendingBlockGuideTitle())
                    emptyState(texts.pendingEmpty())
                }
            }
        }
    }

    override fun getPageTitle(): String = title
}

/**
 * Cabinet of an Observer (testsys.web.page.observer), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("observer/:section?(contests)")
@RolesAllowed("OBSERVER")
class ObserverView(headers: CabinetHeaders) :
    PendingCabinetView(headers, title = currentLocalization().pageObserver.title(), active = CabinetHeaders.MAIN_SECTION)

/**
 * Cabinet of a Supervisor (testsys.web.page.supervisor), not implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("supervisor/:section?(users|communities)")
@RolesAllowed("SUPERVISOR")
class SupervisorView(headers: CabinetHeaders) :
    PendingCabinetView(headers, title = currentLocalization().pageSupervisor.title(), active = CabinetHeaders.MAIN_SECTION)
