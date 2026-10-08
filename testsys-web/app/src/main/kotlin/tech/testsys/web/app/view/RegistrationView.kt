package tech.testsys.web.app.view

import com.vaadin.flow.component.UI
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.auth.AnonymousAllowed
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.RegistrationRole
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
import tech.testsys.web.components.texts.UiTexts

private const val RESEND_HINT = "Отправьте код повторно."

/**
 * Self-registration page (testsys.web.page.registration): the e-mail step and the confirmation step; after
 * registration the new user is signed in and sees their access code on the main page.
 *
 * @since %CURRENT_VERSION%
 */
@Route("registration")
@PageTitle("Регистрация")
@AnonymousAllowed
class RegistrationView(
    texts: UiTexts,
    private val headers: CabinetHeaders,
    private val userService: UserService,
) : TestSysView(texts) {
    init {
        showEmailStep(email = "")
    }

    private fun showEmailStep(email: String) {
        page(headers.guest(hasSignIn = false)) {
            guestForm(active = GuestTab.Registration) {
                lateinit var emailInput: ValueInput<String>
                row { emailInput = textInput("Почта", labelSize = 6, size = 18) { value = email } }
                footer { mainAction("Получить код") { onClick { requestCode(emailInput.value) } } }
            }
        }
    }

    private fun showConfirmationStep(email: String, requestId: RegistrationRequestId) {
        page(headers.guest(hasSignIn = false)) {
            guestForm(active = GuestTab.Registration) {
                lateinit var code: ValueInput<String>
                lateinit var name: ValueInput<String>
                lateinit var role: ValueInput<RegistrationRole?>
                row { field("Почта", labelSize = 6, size = 18) { text(email) } }
                row { code = codeInput("Код из письма", labelSize = 6, size = 18) }
                row { name = textInput("Псевдоним", labelSize = 6, size = 18) }
                row {
                    role = radio("Роль", labelSize = 6, size = 18, items = RegistrationRole.entries, itemLabel = ::roleLabel) {
                        value = RegistrationRole.Student
                    }
                }
                footer {
                    mainAction("Зарегистрироваться") {
                        onClick { confirm(requestId, code = code.value, name = name.value, role = role.value) }
                    }
                    action("Отправить код повторно") { onClick { resendCode(email) } }
                    action("Изменить почту") { onClick { showEmailStep(email) } }
                }
            }
        }
    }

    private fun requestCode(email: String) {
        val requestId = registrationStep { userService.requestRegistration(email) } ?: return
        showConfirmationStep(email, requestId)
    }

    private fun resendCode(email: String) {
        registrationStep { userService.requestRegistration(email) } ?: return
        toast(FeedbackKind.Success, title = "Код отправлен повторно", description = "Письмо с кодом отправлено на $email.")
    }

    private fun confirm(requestId: RegistrationRequestId, code: String, name: String, role: RegistrationRole?) {
        // The radio group starts with a chosen role and cannot be cleared by the user.
        val chosenRole = checkNotNull(role) { "No role is chosen for registration request id=${requestId.value}" }
        val accessToken = registrationStep {
            userService.confirmRegistration(requestId, confirmationCode = code, name = name, role = chosenRole)
        } ?: return

        CabinetSignIn.signIn(userService.authenticate(accessToken))
        UI.getCurrent().navigate(MultiMainView::class.java).ifPresent { view -> view.showAccessToken(accessToken) }
    }

    /** Runs [step] and returns its result, or shows the reason of a refusal and returns `null`. */
    private fun <T : Any> registrationStep(step: () -> T): T? = try {
        step()
    } catch (failure: OperationException) {
        val (title, description) = refusalOf(failure.error) ?: throw failure
        toast(FeedbackKind.Error, title = title, description = description)
        null
    }

    private fun refusalOf(error: OperationError): Pair<String, String?>? = when (error) {
        is RequestRegistrationError -> when (error) {
            InvalidEmailError -> "Почта указана неверно" to "Нужна почта до 255 символов с одним символом @ между непустыми частями."
            EmailAlreadyBoundError -> emailAlreadyBound()
        }
        is ConfirmRegistrationError -> when (error) {
            InvalidUserNameError -> "Псевдоним указан неверно" to "Псевдоним не может быть пустым или длиннее 512 символов."
            is RegistrationRequestNotExistsError -> "Запрос регистрации не найден" to "Измените почту и получите код заново."
            ConfirmationCodeExpiredError -> "Срок действия кода истёк" to RESEND_HINT
            ConfirmationAttemptsExhaustedError -> "Попытки ввода кода исчерпаны" to RESEND_HINT
            InvalidConfirmationCodeError -> "Код не совпадает с отправленным" to null
            EmailAlreadyBoundError -> emailAlreadyBound()
        }
        else -> null
    }

    private fun emailAlreadyBound(): Pair<String, String?> = "Почта уже привязана к Пользователю" to "Войдите по Коду-доступа."

    private fun roleLabel(role: RegistrationRole): String = when (role) {
        RegistrationRole.Student -> "Ученик"
        RegistrationRole.Manager -> "Организатор"
    }
}
