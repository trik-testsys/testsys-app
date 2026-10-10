package tech.testsys.web.app.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.feedback.emptyState
import tech.testsys.web.components.texts.UiTexts

/**
 * Tab «Восстановление доступа» of the sign-in page (testsys.web.page.authentication); restoring access is not
 * implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("restore-access")
@PageTitle("Восстановление доступа")
@AnonymousAllowed
class RestoreAccessView(texts: UiTexts, headers: CabinetHeaders) : TestSysView(texts) {
    init {
        page(headers.guest(hasSignIn = false)) {
            guestForm(active = GuestTab.RestoreAccess) { emptyState("Восстановление доступа пока не реализовано") }
        }
    }
}
