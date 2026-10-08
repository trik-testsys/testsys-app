package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.RegistrationRequestRepository
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.RegistrationRole
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.RecordedMail
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind
import java.time.Instant

@SpringBootTest
class RegistrationViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var mail: RecordedMail

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var registrationRequests: RegistrationRequestRepository

    private lateinit var email: String

    @BeforeEach
    fun openRegistration() {
        email = "${fixtures.unique("new")}@example.com".lowercase()
        UI.getCurrent().navigate(RegistrationView::class.java)
    }

    @Test
    fun `should sign the registered user in and show their access code on the main page`() {
        requestCode(email)

        confirm(code = mail.confirmationCodes.getValue(email), name = "Новый Пользователь")

        assertEquals(MultiMainView::class.java, currentView)
        assertEquals(mail.accessTokens.getValue(email), UI.getCurrent()._get<TextField>().value)
        assertEquals(UserKind.MULTIPLE_ROLE, CabinetSignIn.principal()?.kind)
    }

    @Test
    fun `should register the user in the chosen role`() {
        requestCode(email)
        UI.getCurrent()._get<RadioButtonGroup<RegistrationRole>>().value = RegistrationRole.Manager

        confirm(code = mail.confirmationCodes.getValue(email), name = "Организатор")

        assertInstanceOf(Manager::class.java, multipleRoleUsers.findByEmail(email)?.data?.roles?.single())
    }

    @Test
    fun `should not show the access code again when the main page is opened again`() {
        requestCode(email)
        confirm(code = mail.confirmationCodes.getValue(email), name = "Новый Пользователь")
        UI.getCurrent().navigate(ProfileView::class.java)

        UI.getCurrent().navigate(MultiMainView::class.java)

        assertTrue(UI.getCurrent()._find<TextField>().isEmpty())
    }

    @Test
    fun `should show the reason and stay on the email step if the email is invalid`() {
        requestCode("no-at-sign")

        assertEquals("Почта указана неверно", lastToastTitle())
        assertEquals(1, UI.getCurrent()._find<TextField>().size)
    }

    @Test
    fun `should show the reason if the email is already bound to a user`() {
        fixtures.multipleRoleUser(email = email)

        requestCode(email)

        assertEquals("Почта уже привязана к Пользователю", lastToastTitle())
    }

    @Test
    fun `should show the reason and keep the confirmation step if the nickname is blank`() {
        requestCode(email)

        confirm(code = mail.confirmationCodes.getValue(email), name = " ")

        assertEquals("Псевдоним указан неверно", lastToastTitle())
        assertEquals(RegistrationView::class.java, currentView)
    }

    @Test
    fun `should show the reason if the confirmation code differs`() {
        requestCode(email)

        confirm(code = fixtures.otherCode(mail.confirmationCodes.getValue(email)), name = "Новый Пользователь")

        assertEquals("Код не совпадает с отправленным", lastToastTitle())
    }

    @Test
    fun `should offer to resend the code if the attempts are exhausted`() {
        requestCode(email)
        repeat(MAX_ATTEMPTS) { confirm(code = fixtures.otherCode(mail.confirmationCodes.getValue(email)), name = "Новый Пользователь") }

        confirm(code = mail.confirmationCodes.getValue(email), name = "Новый Пользователь")

        assertEquals("Попытки ввода кода исчерпаны", lastToastTitle())
        assertEquals("Отправьте код повторно.", UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text)
    }

    @Test
    fun `should offer to resend the code if it has expired`() {
        requestCode(email)
        registrationRequests.update(checkNotNull(registrationRequests.findByEmail(email)).withData { expiresAt = Instant.EPOCH })

        confirm(code = mail.confirmationCodes.getValue(email), name = "Новый Пользователь")

        assertEquals("Срок действия кода истёк", lastToastTitle())
        assertEquals("Отправьте код повторно.", UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text)
    }

    @Test
    fun `should offer to request a new code if the registration request no longer exists`() {
        requestCode(email)
        registrationRequests.removeById(checkNotNull(registrationRequests.findByEmail(email)).id)

        confirm(code = mail.confirmationCodes.getValue(email), name = "Новый Пользователь")

        assertEquals("Запрос регистрации не найден", lastToastTitle())
        assertEquals("Измените почту и получите код заново.", UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text)
    }

    @Test
    fun `should send the code again to the same email`() {
        requestCode(email)
        val code = mail.confirmationCodes.remove(email)

        UI.getCurrent()._get<Button> { text = "Отправить код повторно" }._click()

        assertEquals(code, mail.confirmationCodes[email])
        assertEquals("Код отправлен повторно", lastToastTitle())
    }

    @Test
    fun `should return to the email step with the entered email`() {
        requestCode(email)

        UI.getCurrent()._get<Button> { text = "Изменить почту" }._click()

        assertEquals(email, UI.getCurrent()._find<TextField>().single().value)
    }

    private fun requestCode(email: String) {
        UI.getCurrent()._find<TextField>().single().value = email
        UI.getCurrent()._get<Button> { text = "Получить код" }._click()
    }

    private fun confirm(code: String, name: String) {
        val (codeField, nameField) = UI.getCurrent()._find<TextField>()
        codeField.value = code
        nameField.value = name
        UI.getCurrent()._get<Button> { text = "Зарегистрироваться" }._click()
    }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
