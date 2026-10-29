package tech.testsys.web.service.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.service.security.ServiceSignIn
import tech.testsys.web.service.security.ServiceSignInResult

/**
 * Service login on the guest card with one secret field, cleared after every submitted attempt.
 *
 * @since 1.0.0-beta.1
 */
@Route("login")
@PageTitle("Служебный вход")
@AnonymousAllowed
class ServiceLoginView(private val signIn: ServiceSignIn) : TestSysView() {
    init {
        guestPage("Служебный интерфейс") {
            card(title = "Служебный вход", description = "Введите код входа в служебный интерфейс.") {
                val code = passwordInput("Код")
                mainAction("Войти") {
                    clickOnEnter()
                    onClick { submit(code) }
                }
            }
        }
    }

    private fun submit(code: ValueInput<String>) {
        val result = try {
            signIn.signIn(code.value)
        } finally {
            code.value = ""
        }

        when (result) {
            ServiceSignInResult.Success -> UI.getCurrent().navigate(ServiceHomeView::class.java)
            ServiceSignInResult.InvalidCode -> toast(FeedbackKind.Error, "Неверный код")
            ServiceSignInResult.Blocked ->
                toast(FeedbackKind.Error, title = "Вход временно заблокирован", description = "Повторите попытку через 5 минут.")
        }
    }
}
