package tech.testsys.web.app.view

import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.feedback.emptyState

/**
 * Tab «Восстановление доступа» of the sign-in page (testsys.web.page.authentication); restoring access is not
 * implemented yet.
 *
 * @since %CURRENT_VERSION%
 */
@Route("restore-access")
@PageTitle("Восстановление доступа")
@AnonymousAllowed
class RestoreAccessView(headers: CabinetHeaders) : TestSysView() {
    init {
        page(headers.guest(hasSignIn = false)) {
            guide("Восстановление доступа ещё не реализовано. Для входа используйте свой Код-доступа.")
            guestForm(active = GuestTab.RestoreAccess) {
                guide(
                    "Восстановление доступа пока недоступно. Используйте вкладку входа, если у вас есть Код-доступа.",
                    title = "Восстановление доступа",
                )
                emptyState("Восстановление доступа пока не реализовано")
            }
        }
    }
}
