package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.operation.error.AccessRestorationRequestNotExistsError
import tech.testsys.operation.error.ConfirmAccessRestorationError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidPasswordError
import tech.testsys.operation.error.OperationException
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
 * Tab «Восстановление доступа» of the sign-in page (testsys.web.page.authentication): the e-mail step and the step
 * of the code with the new password; after restoration the tab «Вход» opens.
 *
 * @since %CURRENT_VERSION%
 */
@Route("restore-access")
@AnonymousAllowed
class RestoreAccessView(
    private val headers: CabinetHeaders,
    private val userService: UserService,
) : TestSysView(), HasDynamicTitle {
    init {
        showEmailStep(email = "")
    }

    override fun getPageTitle(): String = GuestTab.RestoreAccess.label

    private fun showEmailStep(email: String) {
        val localization = currentLocalization()
        val texts = localization.pageGuest
        page(headers.guest(hasSignIn = false)) {
            guide(texts.restoreAccessGuide())
            guestForm(active = GuestTab.RestoreAccess) {
                lateinit var emailInput: ValueInput<String>
                row { emailInput = textInput(texts.restoreAccessEmail(), labelSize = 6, size = 18) { value = email } }
                guide(texts.restoreAccessBlockGuide(), title = texts.restoreAccessBlockGuideTitle()) {
                    step(emailInput, title = texts.restoreAccessEmail(), description = texts.restoreAccessEmailGuide())
                }
                footer { mainAction(localization.pageConfirmation.sendCode()) { onClick { requestCode(emailInput.value) } } }
            }
        }
    }

    private fun showCodeStep(email: String) {
        val localization = currentLocalization()
        val texts = localization.pageGuest
        val confirmation = localization.pageConfirmation
        page(headers.guest(hasSignIn = false)) {
            guide(texts.restoreAccessGuide())
            guestForm(active = GuestTab.RestoreAccess) {
                lateinit var code: ValueInput<String>
                lateinit var password: ValueInput<String>
                row { field(texts.restoreAccessEmail(), labelSize = 6, size = 18) { text(email) } }
                row { code = codeInput(confirmation.code(), labelSize = 6, size = 18) }
                row { password = passwordInput(texts.restoreAccessPassword(), labelSize = 6, size = 18) }
                guide(texts.restoreAccessCodeStepBlockGuide(), title = texts.restoreAccessCodeStepBlockGuideTitle()) {
                    step(code, title = confirmation.code(), description = texts.restoreAccessCodeGuide())
                    step(password, title = texts.restoreAccessPassword(), description = localization.pagePassword.guide())
                }
                footer {
                    mainAction(texts.restoreAccessButton()) { onClick { confirm(email, code.value, password.value) } }
                    action(confirmation.resendCode()) { onClick { requestCode(email) } }
                    action(confirmation.changeEmail()) { onClick { showEmailStep(email) } }
                }
            }
        }
    }

    /** Requests a code for [email] and shows the code step; the toast does not disclose whether [email] is bound. */
    private fun requestCode(email: String) {
        userService.requestAccessRestoration(email)
        val texts = currentLocalization().pageGuest
        toast(
            FeedbackKind.Success,
            title = texts.restoreAccessRequestedTitle(),
            description = texts.restoreAccessRequestedDescription(email = email),
        )
        showCodeStep(email)
    }

    private fun confirm(email: String, code: String, password: String) {
        try {
            userService.confirmAccessRestoration(email, confirmationCode = code, password = password)
        } catch (failure: OperationException) {
            val error = failure.error as? ConfirmAccessRestorationError ?: throw failure
            val (title, description) = refusalOf(error)
            toast(FeedbackKind.Error, title = title, description = description)
            return
        }
        UI.getCurrent().navigate(AuthenticationView::class.java)
        val texts = currentLocalization().pageGuest
        toast(FeedbackKind.Success, title = texts.restoreAccessCompletedTitle(), description = texts.restoreAccessCompletedDescription())
    }

    private fun refusalOf(error: ConfirmAccessRestorationError): Pair<String, String?> {
        val localization = currentLocalization()
        val confirmation = localization.pageConfirmation
        return when (error) {
            AccessRestorationRequestNotExistsError ->
                localization.pageGuest.restoreAccessRequestNotFound() to confirmation.requestNotFoundHint()
            ConfirmationCodeExpiredError -> confirmation.codeExpired() to confirmation.resendHint()
            ConfirmationAttemptsExhaustedError -> confirmation.attemptsExhausted() to confirmation.resendHint()
            InvalidConfirmationCodeError -> confirmation.codeMismatch() to null
            InvalidPasswordError -> localization.pagePassword.invalidTitle() to localization.pagePassword.invalidDescription()
        }
    }
}
