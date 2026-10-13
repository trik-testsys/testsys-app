package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.AccessRestorationRequestRepository
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.RecordedMail
import tech.testsys.web.app.service.user.UserService
import java.time.Instant

@SpringBootTest
class RestoreAccessViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var mail: RecordedMail

    @Autowired
    private lateinit var requests: AccessRestorationRequestRepository

    @Autowired
    private lateinit var userService: UserService

    private lateinit var user: MultipleRoleUser

    private val email: String
        get() = user.data.email

    @BeforeEach
    fun openRestoreAccess() {
        user = fixtures.multipleRoleUser()
        UI.getCurrent().navigate(RestoreAccessView::class.java)
    }

    @Test
    fun `should send the code to a bound email and show the code step with the neutral notice`() {
        requestCode(email)

        assertEquals("Запрос принят", lastToastTitle())
        assertEquals("Если почта $email привязана к Пользователю, на неё придёт письмо с кодом.", lastToastDescription())
        assertEquals(1, UI.getCurrent()._find<Button> { text = "Восстановить доступ" }.size)
        assertTrue(mail.confirmationCodes.containsKey(email))
    }

    @Test
    fun `should show the same notice and the code step for an unbound email without sending a letter`() {
        val unbound = "${fixtures.unique("unbound")}@example.com"

        requestCode(unbound)

        assertEquals("Запрос принят", lastToastTitle())
        assertEquals(1, UI.getCurrent()._find<Button> { text = "Восстановить доступ" }.size)
        assertFalse(mail.confirmationCodes.containsKey(unbound))
    }

    @Test
    fun `should show the new access code that signs the user in`() {
        requestCode(email)

        confirm(code = mail.confirmationCodes.getValue(email))

        val accessToken = accessTokenFields().single().value
        assertEquals(user.id.value, userService.authenticate(accessToken).userId)
        assertEquals("Доступ восстановлен", lastToastTitle())
        assertEquals(RestoreAccessView::class.java, currentView)
    }

    @Test
    fun `should show the reason if the confirmation code differs`() {
        requestCode(email)

        confirm(code = fixtures.otherCode(mail.confirmationCodes.getValue(email)))

        assertEquals("Код не совпадает с отправленным", lastToastTitle())
        assertEquals(1, UI.getCurrent()._find<Button> { text = "Восстановить доступ" }.size)
    }

    @Test
    fun `should offer to resend the code if the attempts are exhausted`() {
        requestCode(email)
        repeat(MAX_ATTEMPTS) { confirm(code = fixtures.otherCode(mail.confirmationCodes.getValue(email))) }

        confirm(code = mail.confirmationCodes.getValue(email))

        assertEquals("Попытки ввода кода исчерпаны", lastToastTitle())
        assertEquals("Отправьте код повторно.", lastToastDescription())
    }

    @Test
    fun `should offer to resend the code if it has expired`() {
        requestCode(email)
        requests.update(checkNotNull(requests.findByUser(user.id)).withData { expiresAt = Instant.EPOCH })

        confirm(code = mail.confirmationCodes.getValue(email))

        assertEquals("Срок действия кода истёк", lastToastTitle())
        assertEquals("Отправьте код повторно.", lastToastDescription())
    }

    @Test
    fun `should offer to request a new code if there is no access restoration request`() {
        val unbound = "${fixtures.unique("unbound")}@example.com"
        requestCode(unbound)

        confirm(code = "12345678")

        assertEquals("Запрос восстановления не найден", lastToastTitle())
        assertEquals("Измените почту и получите код заново.", lastToastDescription())
    }

    @Test
    fun `should send the same active code again to the same email`() {
        requestCode(email)
        val code = mail.confirmationCodes.remove(email)

        UI.getCurrent()._get<Button> { text = "Отправить код повторно" }._click()

        assertEquals(code, mail.confirmationCodes[email])
        assertEquals("Запрос принят", lastToastTitle())
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

    private fun confirm(code: String) {
        UI.getCurrent()._find<TextField>().single().value = code
        UI.getCurrent()._get<Button> { text = "Восстановить доступ" }._click()
    }

    private fun accessTokenFields(): List<TextField> =
        UI.getCurrent()._find<TextField>().filter { field -> field.ariaLabel.orElse(null) == "Код-доступа" }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    private fun lastToastDescription(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
