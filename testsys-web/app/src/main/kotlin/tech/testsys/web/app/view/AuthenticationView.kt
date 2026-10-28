package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.app.security.AccessCodeSignIn
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.PasswordSignIn
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.segmentedControl
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Sign-in page (testsys.web.page.authentication): signs a user with non-fixed roles in by the e-mail address and the
 * password and a fixed-role user by the access code, chosen by a switch in the same block, then opens the page saved
 * before signing in, or the start page of the user.
 *
 * @since %CURRENT_VERSION%
 */
@Route("login")
@AnonymousAllowed
class AuthenticationView(
    headers: CabinetHeaders,
    private val passwordSignIn: PasswordSignIn,
    private val accessCodeSignIn: AccessCodeSignIn,
) : TestSysView(), HasDynamicTitle {
    init {
        val texts = currentLocalization().pageGuest
        page(headers.guest(hasSignIn = false)) {
            guide(texts.signInGuide())
            guestForm(active = GuestTab.SignIn) {
                lateinit var method: ValueInput<SignInMethod?>
                lateinit var email: ValueInput<String>
                lateinit var password: ValueInput<String>
                lateinit var accessToken: ValueInput<String>
                row {
                    method = segmentedControl(
                        texts.signInMethod(),
                        labelSize = 6,
                        size = 18,
                        items = SignInMethod.entries,
                        itemLabel = { item -> item.label },
                    ) { value = SignInMethod.Password }
                }
                row { email = textInput(texts.signInEmail(), labelSize = 6, size = 18) }
                row { password = passwordInput(texts.signInPassword(), labelSize = 6, size = 18) }
                row { accessToken = passwordInput(texts.signInAccessCode(), labelSize = 6, size = 18) { isVisible = false } }
                method.addValueChangeListener { event ->
                    val isPassword = event.value == SignInMethod.Password
                    email.isVisible = isPassword
                    password.isVisible = isPassword
                    accessToken.isVisible = !isPassword
                }
                guide(texts.signInBlockGuide(), title = texts.tabSignIn()) {
                    step(method, title = texts.signInMethod(), description = texts.signInMethodGuide())
                    step(email, title = texts.signInEmail(), description = texts.signInEmailGuide())
                    step(password, title = texts.signInPassword(), description = texts.signInPasswordGuide())
                    step(accessToken, title = texts.signInAccessCode(), description = texts.signInAccessCodeGuide())
                }
                footer {
                    mainAction(texts.signInButton()) {
                        clickOnEnter()
                        onClick {
                            when (method.value) {
                                SignInMethod.AccessCode -> signInByAccessCode(accessToken.value)
                                else -> signInByPassword(email.value, password.value)
                            }
                        }
                    }
                }
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
        val texts = currentLocalization().pageGuest
        when (val result = accessCodeSignIn.signIn(accessToken)) {
            is AccessCodeSignIn.Result.Success -> open(result.principal, result.returnAddress)
            AccessCodeSignIn.Result.InvalidAccessCode -> toast(FeedbackKind.Error, texts.signInInvalid())
            AccessCodeSignIn.Result.Blocked ->
                toast(FeedbackKind.Error, title = texts.signInBlockedTitle(), description = texts.signInBlockedDescription())
        }
    }

    private fun open(principal: CabinetPrincipal, returnAddress: String?) {
        if (returnAddress == null) {
            UI.getCurrent().navigate(startPageOf(principal.kind))
        } else {
            UI.getCurrent().navigate(returnAddress)
        }
    }
}

/** Way of signing in chosen on the sign-in page. */
internal enum class SignInMethod {
    /** By the e-mail address and the password. */
    Password,

    /** By the access code. */
    AccessCode,
    ;

    val label: String
        get() = with(currentLocalization().pageGuest) {
            when (this@SignInMethod) {
                Password -> signInMethodPassword()
                AccessCode -> signInMethodAccessCode()
            }
        }
}
