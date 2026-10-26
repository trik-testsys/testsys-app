package tech.testsys.web.app.view

import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.operation.error.ConfirmEmailChangeError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.EmailChangeRequestNotExistsError
import tech.testsys.operation.error.EmailUnchangedError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.RequestEmailChangeError
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.currentLocalization

/**
 * Profile of a user with non-fixed roles (testsys.web.page.multi.profile): the nickname and the e-mail address, and,
 * unless a supervisor is signed in as the user, the change of the e-mail address in two steps, the new address and then
 * the code from the letter.
 *
 * @since %CURRENT_VERSION%
 */
@Route("profile")
@RolesAllowed("MULTIPLE_ROLE")
class ProfileView(private val headers: CabinetHeaders, private val multipleRoleUserService: MultipleRoleUserService) :
    TestSysView(), HasDynamicTitle {
    init {
        show(newEmail = null)
    }

    override fun getPageTitle(): String = currentLocalization().pageProfile.title()

    /** Rebuilds the page; with [newEmail] the e-mail change waits for the code sent to it. */
    private fun show(newEmail: String?) {
        val profile = multipleRoleUserService.viewProfile()
        val localization = currentLocalization()
        val texts = localization.pageProfile
        val confirmation = localization.pageConfirmation
        // A supervisor signed in as the user does not change their e-mail address (testsys.user.single.supervisor.impersonate).
        val isImpersonated = CabinetSignIn.principal()?.impersonatedBy != null
        page(headers.cabinet(active = null)) {
            head(texts.title()) { crumb(localization.pageCabinet.home(), MultiMainView::class.java) }
            guide(if (isImpersonated) texts.impersonationGuide() else texts.guide())
            row {
                block(size = HALF_COLUMNS, title = texts.title()) {
                    guide(texts.profileBlockGuide())
                    row { textInput(texts.nickname(), labelSize = 6, size = 18) { value = profile.name } }
                    row { textInput(texts.email(), labelSize = 6, size = 18) { value = profile.email } }
                }.isEditable = false
                if (!isImpersonated) {
                    block(title = texts.changeTitle()) {
                        guide(texts.changeBlockGuide())
                        if (newEmail == null) {
                            lateinit var email: ValueInput<String>
                            row { email = textInput(texts.changeNewEmail(), labelSize = 6, size = 18) }
                            footer {
                                mainAction(confirmation.sendCode()) {
                                    clickOnEnter()
                                    onClick { requestCode(email.value) }
                                }
                            }
                        } else {
                            lateinit var code: ValueInput<String>
                            row {
                                textInput(texts.changeNewEmail(), labelSize = 6, size = 18) {
                                    value = newEmail
                                    isEditable = false
                                }
                            }
                            row { code = codeInput(confirmation.code(), labelSize = 6, size = 18) }
                            footer {
                                mainAction(texts.changeButton()) {
                                    clickOnEnter()
                                    onClick { confirm(code.value) }
                                }
                                action(confirmation.resendCode()) { onClick { resendCode(newEmail) } }
                                action(confirmation.changeEmail()) { onClick { show(newEmail = null) } }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestCode(email: String) {
        emailChangeStep { multipleRoleUserService.requestEmailChange(email) } ?: return
        toast(FeedbackKind.Success, currentLocalization().pageConfirmation.codeSent())
        // The operation stores the address trimmed and in lower case, so the page shows it the same way.
        show(newEmail = email.trim().lowercase())
    }

    private fun resendCode(email: String) {
        emailChangeStep { multipleRoleUserService.requestEmailChange(email) } ?: return
        val texts = currentLocalization().pageConfirmation
        toast(FeedbackKind.Success, title = texts.codeResentTitle(), description = texts.codeResentDescription(email = email))
    }

    private fun confirm(code: String) {
        val user = emailChangeStep { multipleRoleUserService.confirmEmailChange(code) } ?: return
        val texts = currentLocalization().pageProfile
        toast(FeedbackKind.Success, title = texts.changedTitle(), description = texts.changedDescription(email = user.email))
        show(newEmail = null)
    }

    /** Runs [step] and returns its result, or shows the reason of a refusal and returns `null`. */
    private fun <T : Any> emailChangeStep(step: () -> T): T? = try {
        step()
    } catch (failure: OperationException) {
        val (title, description) = refusalOf(failure.error) ?: throw failure
        toast(FeedbackKind.Error, title = title, description = description)
        null
    }

    private fun refusalOf(error: OperationError): Pair<String, String?>? {
        val localization = currentLocalization()
        val texts = localization.pageProfile
        val confirmation = localization.pageConfirmation
        return when (error) {
            is RequestEmailChangeError -> when (error) {
                InvalidEmailError -> confirmation.emailInvalidTitle() to confirmation.emailInvalidDescription()
                EmailUnchangedError -> texts.sameEmail() to texts.anotherEmail()
                EmailAlreadyBoundError -> texts.emailBound() to texts.anotherEmail()
            }
            is ConfirmEmailChangeError -> when (error) {
                EmailChangeRequestNotExistsError -> texts.requestNotFound() to confirmation.requestNotFoundHint()
                ConfirmationCodeExpiredError -> confirmation.codeExpired() to confirmation.resendHint()
                ConfirmationAttemptsExhaustedError -> confirmation.attemptsExhausted() to confirmation.resendHint()
                InvalidConfirmationCodeError -> confirmation.codeMismatch() to null
                EmailAlreadyBoundError -> texts.emailBound() to texts.anotherEmail()
            }
            else -> null
        }
    }

    private companion object {
        /** Columns of the profile block; the e-mail change takes the rest of the row beside it. */
        const val HALF_COLUMNS = 12
    }
}
