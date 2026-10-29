package tech.testsys.web.app.view

import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.Route
import jakarta.annotation.security.RolesAllowed
import tech.testsys.operation.error.ChangePasswordError
import tech.testsys.operation.error.ConfirmEmailChangeError
import tech.testsys.operation.error.ConfirmationAttemptsExhaustedError
import tech.testsys.operation.error.ConfirmationCodeExpiredError
import tech.testsys.operation.error.EmailAlreadyBoundError
import tech.testsys.operation.error.EmailChangeRequestNotExistsError
import tech.testsys.operation.error.EmailUnchangedError
import tech.testsys.operation.error.InvalidConfirmationCodeError
import tech.testsys.operation.error.InvalidCurrentPasswordError
import tech.testsys.operation.error.InvalidEmailError
import tech.testsys.operation.error.InvalidPasswordError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.RequestEmailChangeError
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.ActionHandle
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.passwordInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.overlay.dialog
import tech.testsys.web.components.texts.currentLocalization

/**
 * Profile of a user with non-fixed roles (testsys.web.page.multi.profile): the nickname and the e-mail address in
 * read-only fields and, unless a supervisor is signed in as the user, the actions that open the dialogs of the e-mail
 * change, in two steps, the new address and then the code from the letter, and of the password change.
 *
 * @since 1.0.0-beta.1
 */
@Route("profile")
@RolesAllowed("MULTIPLE_ROLE")
class ProfileView(private val headers: CabinetHeaders, private val multipleRoleUserService: MultipleRoleUserService) :
    TestSysView(), HasDynamicTitle {
    init {
        show()
    }

    override fun getPageTitle(): String = currentLocalization().pageProfile.title()

    private fun show() {
        val profile = multipleRoleUserService.viewProfile()
        val localization = currentLocalization()
        val texts = localization.pageProfile
        // A supervisor signed in as the user changes neither their e-mail address nor their password
        // (testsys.user.single.supervisor.impersonate).
        val isImpersonated = CabinetSignIn.principal()?.impersonatedBy != null
        page(headers.cabinet(active = null)) {
            head(texts.title()) { crumb(localization.pageCabinet.home(), MultiMainView::class.java) }
            guide(if (isImpersonated) texts.impersonationGuide() else texts.guide())
            row {
                block(size = PROFILE_COLUMNS, title = texts.title()) {
                    guide(texts.profileBlockGuide())
                    if (!isImpersonated) {
                        val changingEmail = emailDialog()
                        val changingPassword = passwordDialog()
                        actions {
                            action(texts.changeOpen()) { onClick { changingEmail() } }
                            action(texts.passwordOpen()) { onClick { changingPassword() } }
                        }
                    }
                    row { textInput(texts.nickname(), labelSize = 4, size = 20) { value = profile.name } }
                    row { textInput(texts.email(), labelSize = 4, size = 20) { value = profile.email } }
                }.isEditable = false
            }
        }
    }

    /**
     * Builds the dialog of the e-mail change and returns its opening, which starts from an empty new address: the first
     * step requests a code for the address, the second confirms the change with the code and rebuilds the page.
     */
    private fun emailDialog(): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageProfile
        val confirmation = localization.pageConfirmation
        lateinit var email: ValueInput<String>
        lateinit var code: ValueInput<String>
        lateinit var codeStepActions: List<ActionHandle>
        lateinit var sendCode: ActionHandle

        fun showStep(newEmail: String?) {
            val isCodeStep = newEmail != null
            newEmail?.let { address -> email.value = address }
            email.isEditable = !isCodeStep
            code.value = ""
            code.isVisible = isCodeStep
            sendCode.isVisible = !isCodeStep
            codeStepActions.forEach { action -> action.isVisible = isCodeStep }
        }

        val changing = dialog(title = texts.changeTitle(), subtitle = texts.changeBlockGuide()) {
            row { email = textInput(texts.changeNewEmail(), labelSize = 6, size = 18) }
            row { code = codeInput(confirmation.code(), labelSize = 6, size = 18) }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                sendCode = mainAction(confirmation.sendCode()) {
                    clickOnEnter()
                    onClick { requestCode(email.value)?.let(::showStep) }
                }
                codeStepActions = listOf(
                    action(confirmation.resendCode()) { onClick { resendCode(email.value) } },
                    action(texts.changeOtherEmail()) { onClick { showStep(newEmail = null) } },
                    mainAction(texts.changeButton()) {
                        clickOnEnter()
                        onClick {
                            if (confirmEmailChange(code.value)) {
                                dialog.close()
                                show()
                            }
                        }
                    },
                )
            }
        }
        return {
            email.value = ""
            showStep(newEmail = null)
            changing.open()
        }
    }

    /** Builds the dialog of the password change and returns its opening, which starts from empty fields. */
    private fun passwordDialog(): () -> Unit {
        val localization = currentLocalization()
        val texts = localization.pageProfile
        lateinit var currentPassword: ValueInput<String>
        lateinit var newPassword: ValueInput<String>
        val changing = dialog(title = texts.passwordTitle(), subtitle = texts.passwordBlockGuide()) {
            row { currentPassword = passwordInput(texts.passwordCurrent(), labelSize = 6, size = 18) }
            row {
                newPassword = passwordInput(texts.passwordNew(), labelSize = 6, size = 18, hint = localization.pagePassword.guide())
            }
            footer { dialog ->
                action(localization.uiOverlay.dialogCancel()) { onClick { dialog.close() } }
                mainAction(texts.passwordButton()) {
                    clickOnEnter()
                    onClick { if (changePassword(currentPassword = currentPassword.value, newPassword = newPassword.value)) dialog.close() }
                }
            }
        }
        return {
            currentPassword.value = ""
            newPassword.value = ""
            changing.open()
        }
    }

    /** Changes the password and returns whether it changed; a refusal is shown in a toast. */
    private fun changePassword(currentPassword: String, newPassword: String): Boolean {
        profileStep { multipleRoleUserService.changePassword(currentPassword = currentPassword, newPassword = newPassword) } ?: return false
        val texts = currentLocalization().pageProfile
        toast(FeedbackKind.Success, title = texts.passwordChangedTitle(), description = texts.passwordChangedDescription())
        return true
    }

    /** Requests a code for [email] and returns the address it was sent to, or `null` on a refusal. */
    private fun requestCode(email: String): String? {
        profileStep { multipleRoleUserService.requestEmailChange(email) } ?: return null
        toast(FeedbackKind.Success, currentLocalization().pageConfirmation.codeSent())
        // The operation stores the address trimmed and in lower case, so the dialog shows it the same way.
        return email.trim().lowercase()
    }

    private fun resendCode(email: String) {
        profileStep { multipleRoleUserService.requestEmailChange(email) } ?: return
        val texts = currentLocalization().pageConfirmation
        toast(FeedbackKind.Success, title = texts.codeResentTitle(), description = texts.codeResentDescription(email = email))
    }

    /** Confirms the e-mail change with [code] and returns whether it changed; a refusal is shown in a toast. */
    private fun confirmEmailChange(code: String): Boolean {
        val user = profileStep { multipleRoleUserService.confirmEmailChange(code) } ?: return false
        val texts = currentLocalization().pageProfile
        toast(FeedbackKind.Success, title = texts.changedTitle(), description = texts.changedDescription(email = user.email))
        return true
    }

    /** Runs [step] and returns its result, or shows the reason of a refusal and returns `null`. */
    private fun <T : Any> profileStep(step: () -> T): T? = try {
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
            is ChangePasswordError -> when (error) {
                InvalidPasswordError -> localization.pagePassword.invalidTitle() to localization.pagePassword.invalidDescription()
                InvalidCurrentPasswordError -> texts.passwordCurrentInvalid() to null
            }
            else -> null
        }
    }

    private companion object {
        /** Columns of the profile block. */
        const val PROFILE_COLUMNS = 12
    }
}
