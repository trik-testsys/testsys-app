package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.EmailChangeRequestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.RecordedMail
import java.time.Instant

@SpringBootTest
class ProfileViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var mail: RecordedMail

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var emailChangeRequests: EmailChangeRequestRepository

    @Test
    fun `should announce the first email confirmation code delivery`() {
        open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()

        requestCode(newEmail)

        assertEquals("Код отправлен", lastToastTitle())
    }

    @Test
    fun `should show the nickname and the e-mail address in read-only fields under the main page crumb`() {
        val user = open()

        assertEquals(user.data.name, textField("Псевдоним").value)
        assertEquals(user.data.email, textField("Почта").value)
        assertTrue(textField("Псевдоним").isReadOnly && textField("Почта").isReadOnly)
        val crumbs = UI.getCurrent()._get<Nav> { classes = "ts-crumbs" }._find<RouterLink>().associate { link -> link.text to link.href }
        assertEquals(mapOf("Главная" to "home"), crumbs)
    }

    @Test
    fun `should change the e-mail address by the code sent to the new address`() {
        val user = open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()

        requestCode(newEmail.uppercase())
        confirm(mail.confirmationCodes.getValue(newEmail))

        assertEquals(newEmail, multipleRoleUsers.findById(user.id)?.data?.email)
        assertEquals("Почта изменена", lastToastTitle())
        assertEquals(newEmail, textField("Почта").value)
    }

    @Test
    fun `should keep the spent attempt when the code does not match`() {
        val user = open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()
        requestCode(newEmail)

        confirm(fixtures.otherCode(mail.confirmationCodes.getValue(newEmail)))

        assertEquals("Код не совпадает с отправленным", lastToastTitle())
        assertEquals(MAX_ATTEMPTS - 1, emailChangeRequests.findByUser(user.id)?.data?.attemptsLeft)
        assertEquals(user.data.email, multipleRoleUsers.findById(user.id)?.data?.email)
    }

    @Test
    fun `should tell that the new e-mail address is the current one`() {
        val user = open()

        requestCode(user.data.email)

        assertEquals("Это ваша текущая почта", lastToastTitle())
    }

    @Test
    fun `should tell that the new e-mail address is invalid`() {
        open()

        requestCode("no-at-sign")

        assertEquals("Почта указана неверно", lastToastTitle())
    }

    @Test
    fun `should tell that the new e-mail address is bound to another user`() {
        open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()
        fixtures.multipleRoleUser(email = newEmail)

        requestCode(newEmail)

        assertEquals("Почта уже привязана к другому Пользователю", lastToastTitle())
    }

    @Test
    fun `should send the code again to the same new e-mail address`() {
        open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()
        requestCode(newEmail)
        val code = mail.confirmationCodes.remove(newEmail)

        UI.getCurrent()._get<Button> { text = "Отправить код повторно" }._click()

        assertEquals(code, mail.confirmationCodes[newEmail])
        assertEquals("Код отправлен повторно", lastToastTitle())
    }

    @Test
    fun `should return to the editable new e-mail address field`() {
        open()
        requestCode("${fixtures.unique("new")}@example.com")

        UI.getCurrent()._get<Button> { text = "Изменить почту" }._click()

        assertFalse(textField("Новая почта").isReadOnly)
        assertEquals(1, UI.getCurrent()._find<Button> { text = "Получить код" }.size)
        assertTrue(UI.getCurrent()._find<TextField>().none { field -> field.ariaLabel.orElse(null) == "Код из письма" })
    }

    @Test
    fun `should offer to resend the code if the attempts are exhausted`() {
        open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()
        requestCode(newEmail)
        repeat(MAX_ATTEMPTS) { confirm(fixtures.otherCode(mail.confirmationCodes.getValue(newEmail))) }

        confirm(mail.confirmationCodes.getValue(newEmail))

        assertEquals("Попытки ввода кода исчерпаны", lastToastTitle())
        assertEquals("Отправьте код повторно.", lastToastDescription())
    }

    @Test
    fun `should offer to resend the code if it has expired`() {
        val user = open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()
        requestCode(newEmail)
        emailChangeRequests.update(checkNotNull(emailChangeRequests.findByUser(user.id)).withData { expiresAt = Instant.EPOCH })

        confirm(mail.confirmationCodes.getValue(newEmail))

        assertEquals("Срок действия кода истёк", lastToastTitle())
        assertEquals("Отправьте код повторно.", lastToastDescription())
    }

    @Test
    fun `should offer to request a new code if the e-mail change request no longer exists`() {
        val user = open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()
        requestCode(newEmail)
        emailChangeRequests.removeById(checkNotNull(emailChangeRequests.findByUser(user.id)).id)

        confirm(mail.confirmationCodes.getValue(newEmail))

        assertEquals("Запрос смены почты не найден", lastToastTitle())
        assertEquals("Измените почту и получите код заново.", lastToastDescription())
    }

    private fun open(): MultipleRoleUser = fixtures.multipleRoleUser().also { user ->
        signIn(user)
        UI.getCurrent().navigate(ProfileView::class.java)
    }

    private fun requestCode(email: String) {
        textField("Новая почта").value = email
        UI.getCurrent()._get<Button> { text = "Получить код" }._click()
    }

    private fun confirm(code: String) {
        textField("Код из письма").value = code
        UI.getCurrent()._get<Button> { text = "Сменить почту" }._click()
    }

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    private fun lastToastDescription(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
