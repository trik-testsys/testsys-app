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
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.display.field
import tech.testsys.web.components.display.text
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Self-registration page (testsys.web.page.registration): the e-mail step and the confirmation step with the password;
 * after registration the new user is signed in by the e-mail address and the password and opens the main page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("registration")
@AnonymousAllowed
class RegistrationView(
    private val headers: CabinetHeaders,
    private val userService: UserService,
    private val passwordSignIn: PasswordSignIn,
) : TestSysView(), HasDynamicTitle {
    init {
        showEmailStep(email = "")
    }

    override fun getPageTitle(): String = GuestTab.Registration.label

    private fun showEmailStep(email: String) {
        val localization = currentLocalization()
        val texts = localization.pageRegistration
        page(headers.guest(hasSignIn = false)) {
            guide(texts.guide())
            guestForm(active = GuestTab.Registration) {
                lateinit var emailInput: ValueInput<String>
                row { emailInput = textInput(texts.email(), labelSize = 6, size = 18) { value = email } }
                guide(texts.emailStepBlockGuide(), title = texts.emailStepBlockGuideTitle()) {
                    step(emailInput, title = texts.email(), description = texts.emailGuide())
                }
                footer { mainAction(localization.pageConfirmation.sendCode()) { onClick { requestCode(emailInput.value) } } }
            }
        }
    }

    private fun showConfirmationStep(email: String, requestId: RegistrationRequestId) {
        val localization = currentLocalization()
        val texts = localization.pageRegistration
        val confirmation = localization.pageConfirmation
        page(headers.guest(hasSignIn = false)) {
            guide(texts.guide())
            guestForm(active = GuestTab.Registration) {
                lateinit var code: ValueInput<String>
                lateinit var name: ValueInput<String>
                lateinit var password: ValueInput<String>
                row { field(texts.email(), labelSize = 6, size = 18) { text(email) } }
                row { code = codeInput(confirmation.code(), labelSize = 6, size = 18) }
                row { name = textInput(texts.nickname(), labelSize = 6, size = 18) }
                row { password = passwordInput(texts.password(), labelSize = 6, size = 18) }
                guide(texts.detailsStepBlockGuide(), title = texts.detailsStepBlockGuideTitle()) {
                    step(code, title = confirmation.code(), description = texts.codeGuide())
                    step(name, title = texts.nickname(), description = texts.nicknameGuide())
                    step(password, title = texts.password(), description = localization.pagePassword.guide())
                }
                footer {
                    mainAction(localization.pageGuest.register()) {
                        onClick { confirm(requestId, email = email, code = code.value, name = name.value, password = password.value) }
                    }
                    action(confirmation.resendCode()) { onClick { resendCode(email) } }
                    action(confirmation.changeEmail()) { onClick { showEmailStep(email) } }
                }
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
