package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.operation.error.ConfirmRegistrationError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.InvalidPasswordError
import tech.testsys.operation.error.InvalidUserNameError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.RegistrationRequestNotExistsError
import tech.testsys.operation.error.RequestRegistrationError
import tech.testsys.web.app.security.PasswordSignIn
import tech.testsys.web.app.service.user.UserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.texts.currentLocalization

/**
 * Self-registration page (testsys.web.page.registration): the guest card with the e-mail step and the confirmation step
 * with the password; after registration the new user is signed in by the e-mail address and the password and opens the
 * main page. Links lead back to signing in.
 *
 * @since 1.0.0-beta.1
 */
@Route("registration")
@AnonymousAllowed
class RegistrationView(
    private val userService: UserService,
    private val passwordSignIn: PasswordSignIn,
) : TestSysView(), HasDynamicTitle {
    init {
        showEmailStep(email = "")
    }

    override fun getPageTitle(): String = currentLocalization().pageRegistration.title()

    private fun showEmailStep(email: String) {
        val localization = currentLocalization()
        val texts = localization.pageRegistration
        val guest = localization.pageGuest
        guestPage(guest.tagline()) {
            card(title = texts.title(), description = texts.emailStepDescription(), step = guest.step(current = 1, total = 2)) {
                val emailInput = textInput(texts.email()) { value = email }
                mainAction(localization.pageConfirmation.sendCode()) { onClick { requestCode(emailInput.value) } }
            }
            links {
                text(texts.hasAccount())
                linkAction(guest.signInButton()) { onClick { UI.getCurrent().navigate(AuthenticationView::class.java) } }
            }
        }
    }

    private fun showConfirmationStep(email: String, requestId: RegistrationRequestId) {
        val localization = currentLocalization()
        val texts = localization.pageRegistration
        val guest = localization.pageGuest
        val confirmation = localization.pageConfirmation
        guestPage(guest.tagline()) {
            card(
                title = texts.title(),
                description = texts.detailsStepDescription(email = email),
                step = guest.step(current = 2, total = 2),
            ) {
                val sendAgain: ContentScope.() -> Unit = { linkAction(confirmation.sendAgain()) { onClick { resendCode(email) } } }
                val code = codeInput(confirmation.code(), aside = sendAgain)
                val name = textInput(texts.nickname(), hint = texts.nicknameHint())
                val password = passwordInput(texts.password(), hint = localization.pagePassword.guide())
                mainAction(guest.register()) {
                    onClick { confirm(requestId, email = email, code = code.value, name = name.value, password = password.value) }
                }
            }
            links {
                linkAction(confirmation.changeEmail()) { onClick { showEmailStep(email) } }
                linkAction(guest.backToSignIn()) { onClick { UI.getCurrent().navigate(AuthenticationView::class.java) } }
            }
        }
    }

    private fun requestCode(email: String) {
        val requestId = registrationStep { userService.requestRegistration(email) } ?: return
        toast(FeedbackKind.Success, currentLocalization().pageConfirmation.codeSent())
        showConfirmationStep(email, requestId)
    }

    private fun resendCode(email: String) {
        registrationStep { userService.requestRegistration(email) } ?: return
        val texts = currentLocalization().pageConfirmation
        toast(FeedbackKind.Success, title = texts.codeResentTitle(), description = texts.codeResentDescription(email = email))
    }

    private fun confirm(requestId: RegistrationRequestId, email: String, code: String, name: String, password: String) {
        registrationStep {
            userService.confirmRegistration(requestId, confirmationCode = code, name = name, password = password)
        } ?: return

        if (passwordSignIn.signIn(email, password) is PasswordSignIn.Result.Success) {
            UI.getCurrent().navigate(MultiMainView::class.java)
        } else {
            UI.getCurrent().navigate(AuthenticationView::class.java)
        }
        toast(FeedbackKind.Success, currentLocalization().pageRegistration.completed())
    }

    /** Runs [step] and returns its result, or shows the reason of a refusal and returns `null`. */
    private fun <T : Any> registrationStep(step: () -> T): T? = try {
        step()
    } catch (failure: OperationException) {
        val (title, description) = refusalOf(failure.error) ?: throw failure
        toast(FeedbackKind.Error, title = title, description = description)
        null
    }

    private fun refusalOf(error: OperationError): Pair<String, String?>? {
        val localization = currentLocalization()
        val texts = localization.pageRegistration
        val confirmation = localization.pageConfirmation
        return when (error) {
            is RequestRegistrationError -> when (error) {
                InvalidEmailError -> confirmation.emailInvalidTitle() to confirmation.emailInvalidDescription()
                EmailAlreadyBoundError -> texts.emailBoundTitle() to texts.emailBoundDescription()
            }
            is ConfirmRegistrationError -> when (error) {
                InvalidUserNameError -> texts.nicknameInvalidTitle() to texts.nicknameInvalidDescription()
                InvalidPasswordError -> localization.pagePassword.invalidTitle() to localization.pagePassword.invalidDescription()
                is RegistrationRequestNotExistsError -> texts.requestNotFound() to confirmation.requestNotFoundHint()
                ConfirmationCodeExpiredError -> confirmation.codeExpired() to confirmation.resendHint()
                ConfirmationAttemptsExhaustedError -> confirmation.attemptsExhausted() to confirmation.resendHint()
                InvalidConfirmationCodeError -> confirmation.codeMismatch() to null
                EmailAlreadyBoundError -> texts.emailBoundTitle() to texts.emailBoundDescription()
            }
            else -> null
        }
    }
}
