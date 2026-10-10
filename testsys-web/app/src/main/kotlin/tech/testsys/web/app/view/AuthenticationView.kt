package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.service.user.UserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.texts.UiTexts

/**
 * Sign-in page (testsys.web.page.authentication): signs the user in by the access code and opens the page saved before
 * signing in, or the start page of the user.
 *
 * @since %CURRENT_VERSION%
 */
@Route("login")
@PageTitle("Вход")
@AnonymousAllowed
class AuthenticationView(texts: UiTexts, headers: CabinetHeaders, private val userService: UserService) : TestSysView(texts) {
    init {
        page(headers.guest(hasSignIn = false)) {
            guestForm(active = GuestTab.SignIn) {
                lateinit var accessToken: ValueInput<String>
                row { accessToken = passwordInput("Код-доступа", labelSize = 6, size = 18) }
                footer {
                    mainAction("Войти") {
                        clickOnEnter()
                        onClick { signIn(accessToken.value) }
                    }
                }
            }
        }
    }

    private fun signIn(accessToken: String) {
        // The only refusal of signing in is an access code assigned to nobody.
        val principal = try {
            userService.authenticate(accessToken)
        } catch (_: OperationException) {
            toast(FeedbackKind.Error, "Код-доступа недействителен")
            return
        }

        val returnAddress = CabinetSignIn.signIn(principal)
        if (returnAddress == null) {
            UI.getCurrent().navigate(startPageOf(principal.kind))
        } else {
            UI.getCurrent().navigate(returnAddress)
        }
    }
}
