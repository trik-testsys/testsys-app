package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.web.app.security.AccessCodeSignIn
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.PasswordSignIn
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.segmentedControl
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.texts.currentLocalization

/**
 * Sign-in page (testsys.web.page.authentication): signs a user with non-fixed roles in by the e-mail address and the
 * password and a fixed-role user by the access code, chosen by a switch on the guest card, then opens the page saved
 * before signing in, or the start page of the user. Links lead to access restoration and registration.
 *
 * @since %CURRENT_VERSION%
 */
@Route("login")
@AnonymousAllowed
class AuthenticationView(
    private val passwordSignIn: PasswordSignIn,
    private val accessCodeSignIn: AccessCodeSignIn,
) : TestSysView(), HasDynamicTitle {
    init {
        val texts = currentLocalization().pageGuest
        guestPage(texts.tagline()) {
            card(title = texts.signInTitle(), description = texts.signInDescription()) {
                val method =
                    segmentedControl(texts.signInMethod(), items = SignInMethod.entries, itemLabel = { item -> item.label }) {
                        value = SignInMethod.Password
                    }
                val email = textInput(texts.signInEmail())
                val forgotPassword: ContentScope.() -> Unit = {
                    linkAction(texts.signInForgotPassword()) { onClick { UI.getCurrent().navigate(RestoreAccessView::class.java) } }
                }
                val password = passwordInput(texts.signInPassword(), aside = forgotPassword)
                val accessToken = passwordInput(texts.signInAccessCode()) { isVisible = false }
                method.addValueChangeListener { event ->
                    val isPassword = event.value == SignInMethod.Password
                    email.isVisible = isPassword
                    password.isVisible = isPassword
                    accessToken.isVisible = !isPassword
                }
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
            links {
                text(texts.signInNoAccount())
                linkAction(texts.register()) { onClick { UI.getCurrent().navigate(RegistrationView::class.java) } }
            }
        }
    }

    override fun getPageTitle(): String = currentLocalization().pageGuest.signInTitle()

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
