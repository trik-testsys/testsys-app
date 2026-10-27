package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.session.SessionRegistry
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.AccessRestorationRequestRepository
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.RecordedMail
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
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

    @Autowired
    private lateinit var sessionRegistry: SessionRegistry

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
    fun `should set the new password and open signing in with a notice`() {
        requestCode(email)

        confirm(code = mail.confirmationCodes.getValue(email))

        assertEquals(user.id.value, userService.authenticateByPassword(email, NEW_PASSWORD).userId)
        assertEquals("Пароль изменён", lastToastTitle())
        assertEquals(AuthenticationView::class.java, currentView)
    }

    @Test
    fun `should show the password rules and keep the code step if the new password is too short`() {
        requestCode(email)

        confirm(code = mail.confirmationCodes.getValue(email), password = "short")

        assertEquals("Пароль не подходит по длине", lastToastTitle())
        assertEquals(1, UI.getCurrent()._find<Button> { text = "Восстановить доступ" }.size)
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

    @Test
    fun `should open the start page of the user while a supervisor is signed in as the user`() {
        signInImpersonated()

        UI.getCurrent().navigate(RestoreAccessView::class.java)

        assertEquals(MultiMainView::class.java, currentView)
    }

    @Test
    fun `should refuse an access restoration request while a supervisor is signed in as some user`() {
        signInImpersonated()

        assertThrows<IllegalStateException> { userService.requestAccessRestoration(email) }

        assertFalse(mail.confirmationCodes.containsKey(email))
        assertNull(requests.findByUser(user.id))
    }

    @Test
    fun `should refuse an access restoration confirmation while a supervisor is signed in as some user`() {
        requestCode(email)
        val attemptsLeft = checkNotNull(requests.findByUser(user.id)).data.attemptsLeft
        val userSession = fixtures.unique("session")
        sessionRegistry.registerNewSession(userSession, CabinetPrincipal.of(user))
        signInImpersonated()

        assertThrows<IllegalStateException> {
            userService.confirmAccessRestoration(email, confirmationCode = mail.confirmationCodes.getValue(email), password = NEW_PASSWORD)
        }

        assertEquals(user.id.value, userService.authenticateByPassword(email, fixtures.password).userId)
        assertEquals(attemptsLeft, checkNotNull(requests.findByUser(user.id)).data.attemptsLeft)
        assertFalse(checkNotNull(sessionRegistry.getSessionInformation(userSession)).isExpired)
    }

    /** Signs the supervisor in as [user]. */
    private fun signInImpersonated() {
        CabinetSignIn.signIn(CabinetPrincipal.of(user).copy(impersonatedBy = fixtures.supervisor().id.value))
    }

    private fun requestCode(email: String) {
        UI.getCurrent()._find<TextField>().single().value = email
        UI.getCurrent()._get<Button> { text = "Получить код" }._click()
    }

    private fun confirm(code: String, password: String = NEW_PASSWORD) {
        UI.getCurrent()._find<TextField>().single().value = code
        UI.getCurrent()._find<PasswordField>().single().value = password
        UI.getCurrent()._get<Button> { text = "Восстановить доступ" }._click()
    }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    private fun lastToastDescription(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val NEW_PASSWORD = "Restored password 1"
    }
}
