package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
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
import tech.testsys.web.components.texts.currentLocalization

/**
 * Sign-in page (testsys.web.page.authentication): signs the user in by the access code and opens the page saved before
 * signing in, or the start page of the user.
 *
 * @since %CURRENT_VERSION%
 */
@Route("login")
@AnonymousAllowed
class AuthenticationView(headers: CabinetHeaders, private val userService: UserService) : TestSysView(), HasDynamicTitle {
    init {
        val texts = currentLocalization().pageGuest
        page(headers.guest(hasSignIn = false)) {
            guide(texts.signInGuide())
            guestForm(active = GuestTab.SignIn) {
                lateinit var accessToken: ValueInput<String>
                row { accessToken = passwordInput(texts.signInAccessCode(), labelSize = 6, size = 18) }
                guide(texts.signInBlockGuide(), title = texts.tabSignIn()) {
                    step(accessToken, title = texts.signInAccessCode(), description = texts.signInAccessCodeGuide())
                }
                footer {
                    mainAction(texts.signInButton()) {
                        clickOnEnter()
                        onClick { signIn(accessToken.value) }
                    }
                }
            }
        }
    }

    override fun getPageTitle(): String = GuestTab.SignIn.label

    private fun signIn(accessToken: String) {
        // The only refusal of signing in is an access code assigned to nobody.
        val principal = try {
            userService.authenticate(accessToken)
        } catch (_: OperationException) {
            toast(FeedbackKind.Error, currentLocalization().pageGuest.signInInvalid())
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
