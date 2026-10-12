package tech.testsys.web.app.view

import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.texts.currentLocalization

/**
 * Tab «Восстановление доступа» of the sign-in page (testsys.web.page.authentication); restoring access is not
 * implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("restore-access")
@AnonymousAllowed
class RestoreAccessView(headers: CabinetHeaders) : TestSysView(), HasDynamicTitle {
    init {
        val texts = currentLocalization().pageGuest
        page(headers.guest(hasSignIn = false)) {
            guide(texts.restoreAccessGuide())
            guestForm(active = GuestTab.RestoreAccess) {
                guide(texts.restoreAccessBlockGuide(), title = texts.tabRestoreAccess())
                emptyState(texts.restoreAccessEmpty())
            }
        }
    }

    override fun getPageTitle(): String = GuestTab.RestoreAccess.label
}
