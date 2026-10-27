package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.operation.error.OperationException
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.PasswordSignIn
import tech.testsys.web.app.service.user.UserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Sign-in page (testsys.web.page.authentication): signs a user with non-fixed roles in by the e-mail address and the
 * password and a fixed-role user by the access code in a separate block, then opens the page saved before signing in,
 * or the start page of the user.
 *
 * @since %CURRENT_VERSION%
 */
@Route("login")
@AnonymousAllowed
class AuthenticationView(
    headers: CabinetHeaders,
    private val userService: UserService,
    private val passwordSignIn: PasswordSignIn,
) : TestSysView(), HasDynamicTitle {
    init {
        val texts = currentLocalization().pageGuest
        page(headers.guest(hasSignIn = false)) {
            guide(texts.signInGuide())
            guestForm(active = GuestTab.SignIn) {
                lateinit var email: ValueInput<String>
                lateinit var password: ValueInput<String>
                row { email = textInput(texts.signInEmail(), labelSize = 6, size = 18) }
                row { password = passwordInput(texts.signInPassword(), labelSize = 6, size = 18) }
                guide(texts.signInBlockGuide(), title = texts.tabSignIn()) {
                    step(email, title = texts.signInEmail(), description = texts.signInEmailGuide())
                    step(password, title = texts.signInPassword(), description = texts.signInPasswordGuide())
                }
                footer {
                    mainAction(texts.signInButton()) {
                        clickOnEnter()
                        onClick { signInByPassword(email.value, password.value) }
                    }
                }
            }
            guestBlock(title = texts.accessCodeTitle()) {
                lateinit var accessToken: ValueInput<String>
                row { accessToken = passwordInput(texts.signInAccessCode(), labelSize = 6, size = 18) }
                guide(texts.accessCodeBlockGuide(), title = texts.accessCodeTitle()) {
                    step(accessToken, title = texts.signInAccessCode(), description = texts.signInAccessCodeGuide())
                }
                footer { mainAction(texts.accessCodeButton()) { onClick { signInByAccessCode(accessToken.value) } } }
            }
        }
    }

    override fun getPageTitle(): String = GuestTab.SignIn.label

    private fun signInByPassword(email: String, password: String) {
        val texts = currentLocalization().pageGuest
        when (val result = passwordSignIn.signIn(email, password)) {
            is PasswordSignIn.Result.Success -> open(result.principal, result.returnAddress)
            PasswordSignIn.Result.InvalidCredentials -> toast(FeedbackKind.Error, texts.signInCredentialsInvalid())
            PasswordSignIn.Result.Blocked ->
                toast(FeedbackKind.Error, title = texts.signInBlockedTitle(), description = texts.signInBlockedDescription())
        }
    }

    private fun signInByAccessCode(accessToken: String) {
        // The only refusal of signing in by an access code is a code assigned to no fixed-role user.
        val principal = try {
            userService.authenticate(accessToken)
        } catch (_: OperationException) {
            toast(FeedbackKind.Error, currentLocalization().pageGuest.signInInvalid())
            return
        }

        open(principal, CabinetSignIn.signIn(principal))
    }

    private fun open(principal: CabinetPrincipal, returnAddress: String?) {
        if (returnAddress == null) {
            UI.getCurrent().navigate(startPageOf(principal.kind))
        } else {
            UI.getCurrent().navigate(returnAddress)
        }
    }
}
