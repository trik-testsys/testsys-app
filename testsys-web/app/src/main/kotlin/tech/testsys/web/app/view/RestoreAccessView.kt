package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
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
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.service.user.UserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.linkAction
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.texts.currentLocalization

/**
 * Access restoration page of signing in (testsys.web.page.authentication): the guest card with the e-mail step and the
 * step of the code with the new password; after restoration signing in opens. A supervisor signed in as a user goes to
 * the start page of the user.
 *
 * @since %CURRENT_VERSION%
 */
@Route("restore-access")
@AnonymousAllowed
class RestoreAccessView(private val userService: UserService) : TestSysView(), HasDynamicTitle, BeforeEnterObserver {
    init {
        showEmailStep(email = "")
    }

    override fun getPageTitle(): String = currentLocalization().pageGuest.restoreAccessTitle()

    override fun beforeEnter(event: BeforeEnterEvent) {
        val principal = CabinetSignIn.principal() ?: return
        if (principal.impersonatedBy != null) event.forwardTo(startPageOf(principal.kind))
    }

    private fun showEmailStep(email: String) {
        val localization = currentLocalization()
        val texts = localization.pageGuest
        guestPage(texts.tagline()) {
            card(
                title = texts.restoreAccessTitle(),
                description = texts.restoreAccessEmailStepDescription(),
                step = texts.step(current = 1, total = 2),
            ) {
                val emailInput = textInput(texts.restoreAccessEmail()) { value = email }
                mainAction(localization.pageConfirmation.sendCode()) { onClick { requestCode(emailInput.value) } }
            }
            links { backToSignIn() }
        }
    }

    private fun showCodeStep(email: String) {
        val localization = currentLocalization()
        val texts = localization.pageGuest
        val confirmation = localization.pageConfirmation
        guestPage(texts.tagline()) {
            card(
                title = texts.restoreAccessTitle(),
                description = texts.restoreAccessCodeStepDescription(email = email),
                step = texts.step(current = 2, total = 2),
            ) {
                val sendAgain: ContentScope.() -> Unit = { linkAction(confirmation.sendAgain()) { onClick { requestCode(email) } } }
                val code = codeInput(confirmation.code(), aside = sendAgain)
                val password = passwordInput(texts.restoreAccessPassword(), hint = localization.pagePassword.guide())
                mainAction(texts.restoreAccessButton()) { onClick { confirm(email, code.value, password.value) } }
            }
            links {
                linkAction(confirmation.changeEmail()) { onClick { showEmailStep(email) } }
                backToSignIn()
            }
        }
    }

    private fun ContentScope.backToSignIn() {
        linkAction(currentLocalization().pageGuest.backToSignIn()) { onClick { UI.getCurrent().navigate(AuthenticationView::class.java) } }
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
