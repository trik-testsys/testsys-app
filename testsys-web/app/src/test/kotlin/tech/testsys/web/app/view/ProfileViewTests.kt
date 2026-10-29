package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.RouterLink
import com.vaadin.flow.server.VaadinSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.session.SessionRegistry
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.EmailChangeRequestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.RecordedMail
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.app.service.user.UserService
import java.time.Instant

@SpringBootTest
class ProfileViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var mail: RecordedMail

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @Autowired
    private lateinit var emailChangeRequests: EmailChangeRequestRepository

    @Autowired
    private lateinit var multipleRoleUserService: MultipleRoleUserService

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var sessionRegistry: SessionRegistry

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
    fun `should title the browser tab of the profile`() {
        open()

        assertEquals("Профиль", _get<ProfileView>().pageTitle)
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
        assertTrue(UI.getCurrent()._find<TextField>().none { field -> field.ariaLabel.orElse(null) == "Новая почта" })
    }

    @Test
    fun `should show no fields on the page besides the read-only nickname and e-mail address`() {
        open()

        val fields = UI.getCurrent()._find<TextField>().map { field -> field.ariaLabel.orElse(null) to field.isReadOnly }
        assertEquals(listOf("Псевдоним" to true, "Почта" to true), fields)
        assertTrue(UI.getCurrent()._find<PasswordField>().isEmpty())
    }

    @Test
    fun `should start the e-mail dialog from an empty new address on reopening`() {
        open()
        requestCode("${fixtures.unique("new")}@example.com")
        UI.getCurrent()._get<Button> { text = "Отменить" }._click()

        openEmailDialog()

        assertEquals("", textField("Новая почта").value)
        assertFalse(textField("Новая почта").isReadOnly)
        assertTrue(UI.getCurrent()._find<TextField>().none { field -> field.ariaLabel.orElse(null) == "Код из письма" })
    }

    @Test
    fun `should keep the spent attempt when the code does not match`() {
        val user = open()
        val newEmail = "${fixtures.unique("new")}@example.com".lowercase()
        requestCode(newEmail)

        confirm(fixtures.otherCode(mail.confirmationCodes.getValue(newEmail)))

        assertEquals("Неверный код", lastToastTitle())
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
    fun `should tell that the new e-mail address is invalid and keep the dialog open`() {
        open()

        requestCode("no-at-sign")

        assertEquals("Почта указана неверно", lastToastTitle())
        assertEquals("no-at-sign", textField("Новая почта").value)
        assertFalse(textField("Новая почта").isReadOnly)
        assertEquals(1, UI.getCurrent()._find<Button> { text = "Получить код" }.size)
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

        UI.getCurrent()._get<Button> { text = "Указать другую почту" }._click()

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

        assertEquals("Слишком много неверных попыток", lastToastTitle())
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

        assertEquals("Сначала получите код для новой почты", lastToastTitle())
        assertEquals("Измените почту и получите код заново.", lastToastDescription())
    }

    @Test
    fun `should not offer the e-mail change while a supervisor is signed in as the user`() {
        openImpersonated()

        assertTrue(UI.getCurrent()._find<Button> { text = "Изменить почту" }.isEmpty())
        assertTrue(UI.getCurrent()._find<TextField>().none { field -> field.ariaLabel.orElse(null) == "Новая почта" })
    }

    @Test
    fun `should describe the profile without the e-mail change in the page guide while a supervisor is signed in as the user`() {
        openImpersonated()

        assertEquals("Здесь можно просмотреть Профиль Пользователя.", pageGuide())
    }

    @Test
    fun `should offer the e-mail change in the page guide when the user signed in themselves`() {
        open()

        assertEquals("Здесь можно просмотреть свой Профиль, изменить привязанную почту и сменить Пароль.", pageGuide())
    }

    @Test
    fun `should change the password and end the other sessions of the user, keeping the current one`() {
        val user = open()
        val otherSession = fixtures.unique("session")
        sessionRegistry.registerNewSession(otherSession, CabinetPrincipal.of(user))

        changePassword(current = fixtures.password, new = NEW_PASSWORD)

        assertEquals("Пароль изменён", lastToastTitle())
        assertEquals(user.id.value, userService.authenticateByPassword(user.data.email, NEW_PASSWORD).userId)
        assertTrue(checkNotNull(sessionRegistry.getSessionInformation(otherSession)).isExpired)
        assertFalse(checkNotNull(sessionRegistry.getSessionInformation(VaadinSession.getCurrent().session.id)).isExpired)
        assertTrue(UI.getCurrent()._find<PasswordField>().isEmpty())
    }

    @Test
    fun `should tell that the current password is wrong and keep the password and the dialog`() {
        val user = open()

        changePassword(current = "Wrong password", new = NEW_PASSWORD)

        assertEquals("Текущий Пароль неверен", lastToastTitle())
        assertEquals(user.id.value, userService.authenticateByPassword(user.data.email, fixtures.password).userId)
        assertEquals(2, UI.getCurrent()._find<PasswordField>().size)
    }

    @Test
    fun `should start the password dialog from empty fields on reopening`() {
        open()
        changePassword(current = "Wrong password", new = NEW_PASSWORD)
        UI.getCurrent()._get<Button> { text = "Отменить" }._click()

        UI.getCurrent()._get<Button> { text = "Изменить пароль" }._click()

        assertEquals(listOf("", ""), UI.getCurrent()._find<PasswordField>().map { field -> field.value })
    }

    @Test
    fun `should show the password rules if the new password is too short`() {
        open()

        changePassword(current = fixtures.password, new = "short")

        assertEquals("Пароль не подходит", lastToastTitle())
    }

    @Test
    fun `should not offer the password change while a supervisor is signed in as the user`() {
        openImpersonated()

        assertTrue(UI.getCurrent()._find<Button> { text = "Изменить пароль" }.isEmpty())
        assertTrue(UI.getCurrent()._find<PasswordField>().isEmpty())
    }

    @Test
    fun `should refuse a password change while a supervisor is signed in as the user`() {
        val user = openImpersonated()

        assertThrows<IllegalStateException> { multipleRoleUserService.changePassword(fixtures.password, NEW_PASSWORD) }

        assertEquals(user.id.value, userService.authenticateByPassword(user.data.email, fixtures.password).userId)
    }

    @Test
    fun `should refuse an e-mail change request while a supervisor is signed in as the user`() {
        val user = openImpersonated()

        assertThrows<IllegalStateException> { multipleRoleUserService.requestEmailChange("${fixtures.unique("new")}@example.com") }

        assertNull(emailChangeRequests.findByUser(user.id))
    }

    @Test
    fun `should refuse an e-mail change confirmation while a supervisor is signed in as the user`() {
        openImpersonated()

        assertThrows<IllegalStateException> { multipleRoleUserService.confirmEmailChange("000000") }
    }

    /** Opens the profile of a new user as the supervisor signed in as them. */
    private fun openImpersonated(): MultipleRoleUser = fixtures.multipleRoleUser().also { user ->
        CabinetSignIn.signIn(CabinetPrincipal.of(user).copy(impersonatedBy = fixtures.supervisor().id.value))
        UI.getCurrent().navigate(ProfileView::class.java)
    }

    private fun open(): MultipleRoleUser = fixtures.multipleRoleUser().also { user ->
        signIn(user)
        UI.getCurrent().navigate(ProfileView::class.java)
    }

    private fun openEmailDialog() {
        UI.getCurrent()._get<Button> { text = "Изменить почту" }._click()
    }

    /** Opens the e-mail dialog and requests a code for [email]. */
    private fun requestCode(email: String) {
        openEmailDialog()
        textField("Новая почта").value = email
        UI.getCurrent()._get<Button> { text = "Получить код" }._click()
    }

    private fun confirm(code: String) {
        textField("Код из письма").value = code
        UI.getCurrent()._get<Button> { text = "Сменить почту" }._click()
    }

    /** Opens the password dialog and changes the password. */
    private fun changePassword(current: String, new: String) {
        UI.getCurrent()._get<Button> { text = "Изменить пароль" }._click()
        passwordField("Текущий Пароль").value = current
        passwordField("Новый Пароль").value = new
        UI.getCurrent()._get<Button> { text = "Сменить пароль" }._click()
    }

    private fun passwordField(label: String): PasswordField =
        UI.getCurrent()._find<PasswordField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun textField(label: String): TextField =
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun pageGuide(): String? = UI.getCurrent()._find<Div>()
        .single { div -> div.element.hasAttribute("data-guide-introduction") }
        .element.getAttribute("data-guide-description")

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    private fun lastToastDescription(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__desc" }.last().text

    private companion object {
        const val MAX_ATTEMPTS = 3
        const val NEW_PASSWORD = "Changed-password-1"
    }
}
