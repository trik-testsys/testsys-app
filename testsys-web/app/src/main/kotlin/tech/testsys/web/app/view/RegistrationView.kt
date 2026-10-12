package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.RegistrationRole
import tech.testsys.infra.localization.PageCabinet.RoleNameRole
import tech.testsys.operation.error.ConfirmRegistrationError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.InvalidUserNameError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.RegistrationRequestNotExistsError
import tech.testsys.operation.error.RequestRegistrationError
import tech.testsys.web.app.security.CabinetSignIn
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
import tech.testsys.web.components.forms.radio
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Self-registration page (testsys.web.page.registration): the e-mail step and the confirmation step; after
 * registration the new user is signed in and sees their access code on the main page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("registration")
@AnonymousAllowed
class RegistrationView(
    private val headers: CabinetHeaders,
    private val userService: UserService,
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
                lateinit var role: ValueInput<RegistrationRole?>
                row { field(texts.email(), labelSize = 6, size = 18) { text(email) } }
                row { code = codeInput(confirmation.code(), labelSize = 6, size = 18) }
                row { name = textInput(texts.nickname(), labelSize = 6, size = 18) }
                row {
                    role = radio(texts.role(), labelSize = 6, size = 18, items = RegistrationRole.entries, itemLabel = ::roleLabel) {
                        value = RegistrationRole.Student
                    }
                }
                guide(texts.detailsStepBlockGuide(), title = texts.detailsStepBlockGuideTitle()) {
                    step(code, title = confirmation.code(), description = texts.codeGuide())
                    step(name, title = texts.nickname(), description = texts.nicknameGuide())
                    step(role, title = texts.role(), description = texts.roleGuide())
                }
                footer {
                    mainAction(localization.pageGuest.register()) {
                        onClick { confirm(requestId, code = code.value, name = name.value, role = role.value) }
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

    private fun confirm(requestId: RegistrationRequestId, code: String, name: String, role: RegistrationRole?) {
        // The radio group starts with a chosen role and cannot be cleared by the user.
        val chosenRole = checkNotNull(role) { "No role is chosen for registration request id=${requestId.value}" }
        val accessToken = registrationStep {
            userService.confirmRegistration(requestId, confirmationCode = code, name = name, role = chosenRole)
        } ?: return

        CabinetSignIn.signIn(userService.authenticate(accessToken))
        UI.getCurrent().navigate(MultiMainView::class.java).ifPresent { view -> view.showAccessToken(accessToken) }
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
                is RegistrationRequestNotExistsError -> texts.requestNotFound() to confirmation.requestNotFoundHint()
                ConfirmationCodeExpiredError -> confirmation.codeExpired() to confirmation.resendHint()
                ConfirmationAttemptsExhaustedError -> confirmation.attemptsExhausted() to confirmation.resendHint()
                InvalidConfirmationCodeError -> confirmation.codeMismatch() to null
                EmailAlreadyBoundError -> texts.emailBoundTitle() to texts.emailBoundDescription()
            }
            else -> null
        }
    }

    private fun roleLabel(role: RegistrationRole): String = currentLocalization().pageCabinet.roleName(
        role = when (role) {
            RegistrationRole.Student -> RoleNameRole.STUDENT
            RegistrationRole.Manager -> RoleNameRole.MANAGER
        },
    )
}
