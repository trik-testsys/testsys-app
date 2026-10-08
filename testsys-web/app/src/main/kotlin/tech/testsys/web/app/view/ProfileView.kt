package tech.testsys.web.app.view

import com.vaadin.flow.router.PageTitle
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
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.components.TestSysView
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.actions.mainAction
import tech.testsys.web.components.feedback.FeedbackKind
import tech.testsys.web.components.feedback.toast
import tech.testsys.web.components.forms.ValueInput
import tech.testsys.web.components.forms.codeInput
import tech.testsys.web.components.forms.textInput
import tech.testsys.web.components.texts.UiTexts

private const val RESEND_HINT = "Отправьте код повторно."

/**
 * Profile of a user with non-fixed roles (testsys.web.page.multi.profile): the nickname and the e-mail address, and the
 * change of the e-mail address in two steps, the new address and then the code from the letter.
 *
 * @since %CURRENT_VERSION%
 */
@Route("profile")
@PageTitle("Профиль")
@RolesAllowed("MULTIPLE_ROLE")
class ProfileView(texts: UiTexts, private val headers: CabinetHeaders, private val multipleRoleUserService: MultipleRoleUserService) :
    TestSysView(texts) {
    init {
        show(newEmail = null)
    }

    /** Rebuilds the page; with [newEmail] the e-mail change waits for the code sent to it. */
    private fun show(newEmail: String?) {
        val profile = multipleRoleUserService.viewProfile()
        page(headers.cabinet(active = null)) {
            head("Профиль") { crumb("Главная", MultiMainView::class.java) }
            row {
                block(size = HALF_COLUMNS, title = "Профиль") {
                    row { textInput("Псевдоним", labelSize = 6, size = 18) { value = profile.name } }
                    row { textInput("Почта", labelSize = 6, size = 18) { value = profile.email } }
                }.isEditable = false
                block(title = "Смена почты") {
                    if (newEmail == null) {
                        lateinit var email: ValueInput<String>
                        row { email = textInput("Новая почта", labelSize = 6, size = 18) }
                        footer {
                            mainAction("Получить код") {
                                clickOnEnter()
                                onClick { requestCode(email.value) }
                            }
                        }
                    } else {
                        lateinit var code: ValueInput<String>
                        row {
                            textInput("Новая почта", labelSize = 6, size = 18) {
                                value = newEmail
                                isEditable = false
                            }
                        }
                        row { code = codeInput("Код из письма", labelSize = 6, size = 18) }
                        footer {
                            mainAction("Сменить почту") {
                                clickOnEnter()
                                onClick { confirm(code.value) }
                            }
                            action("Отправить код повторно") { onClick { resendCode(newEmail) } }
                            action("Изменить почту") { onClick { show(newEmail = null) } }
                        }
                    }
                }
            }
        }
    }

    private fun requestCode(email: String) {
        emailChangeStep { multipleRoleUserService.requestEmailChange(email) } ?: return
        // The operation stores the address trimmed and in lower case, so the page shows it the same way.
        show(newEmail = email.trim().lowercase())
    }

    private fun resendCode(email: String) {
        emailChangeStep { multipleRoleUserService.requestEmailChange(email) } ?: return
        toast(FeedbackKind.Success, title = "Код отправлен повторно", description = "Письмо с кодом отправлено на $email.")
    }

    private fun confirm(code: String) {
        val user = emailChangeStep { multipleRoleUserService.confirmEmailChange(code) } ?: return
        toast(FeedbackKind.Success, title = "Почта изменена", description = "Новая почта — ${user.email}.")
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

    private fun refusalOf(error: OperationError): Pair<String, String?>? = when (error) {
        is RequestEmailChangeError -> when (error) {
            InvalidEmailError -> "Почта указана неверно" to "Нужна почта до 255 символов с одним символом @ между непустыми частями."
            EmailUnchangedError -> "Это ваша текущая почта" to "Укажите другую почту."
            EmailAlreadyBoundError -> emailAlreadyBound()
        }
        is ConfirmEmailChangeError -> when (error) {
            EmailChangeRequestNotExistsError -> "Запрос смены почты не найден" to "Измените почту и получите код заново."
            ConfirmationCodeExpiredError -> "Срок действия кода истёк" to RESEND_HINT
            ConfirmationAttemptsExhaustedError -> "Попытки ввода кода исчерпаны" to RESEND_HINT
            InvalidConfirmationCodeError -> "Код не совпадает с отправленным" to null
            EmailAlreadyBoundError -> emailAlreadyBound()
        }
        else -> null
    }

    private fun emailAlreadyBound(): Pair<String, String?> = "Почта уже привязана к другому Пользователю" to "Укажите другую почту."

    private companion object {
        /** Columns of the profile block; the e-mail change takes the rest of the row beside it. */
        const val HALF_COLUMNS = 12
    }
}
