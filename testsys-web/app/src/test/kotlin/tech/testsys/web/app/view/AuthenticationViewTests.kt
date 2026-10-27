package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.server.VaadinSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.session.SessionRegistry
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.operation.config.PublicCommunityOwner
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class AuthenticationViewTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var sessionRegistry: SessionRegistry

    @ParameterizedTest
    @MethodSource("fixedRoleStartPages")
    fun `should sign a fixed-role user in by the access code and open the start page of their kind`(
        kind: UserKind,
        startPage: Class<out Component>,
    ) {
        val user = assertInstanceOf(SingleRoleUser::class.java, fixtures.userOf(kind))
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInWith(fixtures.accessTokenOf(user))

        assertEquals(startPage, currentView)
        assertEquals(CabinetPrincipal.of(user), CabinetSignIn.principal())
    }

    @Test
    fun `should sign a user with non-fixed roles in by the e-mail address and the password`() {
        val user = fixtures.multipleRoleUser()
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInByPassword(" ${user.data.email.uppercase()} ", fixtures.password)

        assertEquals(MultiMainView::class.java, currentView)
        assertEquals(CabinetPrincipal.of(user), CabinetSignIn.principal())
    }

    @Test
    fun `should register the session of the signed-in user`() {
        val user = fixtures.multipleRoleUser()
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInByPassword(user.data.email, fixtures.password)

        assertEquals(
            listOf(VaadinSession.getCurrent().session.id),
            sessionRegistry.getAllSessions(CabinetPrincipal.of(user), false).map { session -> session.sessionId },
        )
    }

    @Test
    fun `should show that the e-mail address or the password is wrong and stay signed out`() {
        val user = fixtures.multipleRoleUser()
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInByPassword(user.data.email, " ${fixtures.password}")

        assertEquals(AuthenticationView::class.java, currentView)
        assertEquals("Почта или Пароль неверны", lastToastTitle())
        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should show the same refusal for an unbound e-mail address`() {
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInByPassword("${fixtures.unique("unbound")}@example.com", fixtures.password)

        assertEquals("Почта или Пароль неверны", lastToastTitle())
        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should close signing in by the e-mail address after five failures even with the right password`() {
        val user = fixtures.multipleRoleUser()
        UI.getCurrent().navigate(AuthenticationView::class.java)
        repeat(5) { signInByPassword(user.data.email, "Wrong password") }

        signInByPassword(user.data.email, fixtures.password)

        assertEquals("Вход временно закрыт", lastToastTitle())
        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should refuse the technical owner`() {
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInByPassword(PublicCommunityOwner.EMAIL, fixtures.password)

        assertEquals("Почта или Пароль неверны", lastToastTitle())
        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should show that the access code is invalid and stay signed out if no user has it`() {
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInWith("unknown access code")

        assertEquals(AuthenticationView::class.java, currentView)
        assertEquals("Код-доступа недействителен", lastToastTitle())
        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should compare the access code exactly`() {
        val accessToken = fixtures.accessTokenOf(fixtures.supervisor())
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInWith(" $accessToken ")

        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should send a guest from a protected page to sign in`() {
        UI.getCurrent().navigate(DeveloperView::class.java)

        assertEquals(AuthenticationView::class.java, currentView)
    }

    @Test
    fun `should open the page saved before signing in`() {
        val user = fixtures.multipleRoleUser { roles { developer { data = developerData {} } } }
        UI.getCurrent().navigate(DeveloperView::class.java)

        signInByPassword(user.data.email, fixtures.password)

        assertEquals(DeveloperView::class.java, currentView)
    }

    @Test
    fun `should open the start page instead of a saved page closed to the user's kind`() {
        val accessToken = fixtures.unique("token")
        fixtures.userOf(UserKind.OBSERVER, rawAccessToken = accessToken)
        UI.getCurrent().navigate(MultiMainView::class.java)

        signInWith(accessToken)

        assertEquals(ObserverView::class.java, currentView)
    }

    @Test
    fun `should change the session id when signing in`() {
        val accessToken = fixtures.unique("token")
        fixtures.userOf(UserKind.OBSERVER, rawAccessToken = accessToken)
        UI.getCurrent().navigate(AuthenticationView::class.java)
        val guestSessionId = VaadinSession.getCurrent().session.id

        signInWith(accessToken)

        assertNotEquals(guestSessionId, VaadinSession.getCurrent().session.id)
    }

    @Test
    fun `should open the registration page by its tab`() {
        UI.getCurrent().navigate(AuthenticationView::class.java)

        UI.getCurrent()._get<NativeButton> { text = "Регистрация" }._click()

        assertEquals(RegistrationView::class.java, currentView)
    }

    @Test
    fun `should open the access restoring page by its tab`() {
        UI.getCurrent().navigate(AuthenticationView::class.java)

        UI.getCurrent()._get<NativeButton> { text = "Восстановление доступа" }._click()

        assertEquals(RestoreAccessView::class.java, currentView)
    }

    @Test
    fun `should return to signing in by its tab`() {
        UI.getCurrent().navigate(RegistrationView::class.java)

        UI.getCurrent()._get<NativeButton> { text = "Вход" }._click()

        assertEquals(AuthenticationView::class.java, currentView)
    }

    @Test
    fun `should select the tab of the open page`() {
        UI.getCurrent().navigate(RegistrationView::class.java)

        assertEquals("true", UI.getCurrent()._get<NativeButton> { text = "Регистрация" }.element.getAttribute("aria-pressed"))
    }

    @ParameterizedTest
    @MethodSource("guestTitles")
    fun `should title the browser tab of a guest form page by its tab`(view: Class<out Component>, title: String) {
        UI.getCurrent().navigate(view)

        assertEquals(title, assertInstanceOf(HasDynamicTitle::class.java, UI.getCurrent().currentView).pageTitle)
    }

    private fun signInWith(accessToken: String) {
        passwordField("Код-доступа").value = accessToken
        UI.getCurrent()._get<Button> { text = "Войти по коду" }._click()
    }

    private fun signInByPassword(email: String, password: String) {
        UI.getCurrent()._find<TextField>().single { field -> field.ariaLabel.orElse(null) == "Почта" }.value = email
        passwordField("Пароль").value = password
        UI.getCurrent()._get<Button> { text = "Войти" }._click()
    }

    private fun passwordField(label: String): PasswordField =
        UI.getCurrent()._find<PasswordField>().single { field -> field.ariaLabel.orElse(null) == label }

    private fun lastToastTitle(): String = UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text

    companion object {
        @JvmStatic
        fun fixedRoleStartPages(): List<Arguments> = listOf(
            Arguments.of(UserKind.PARTICIPANT, ParticipantView::class.java),
            Arguments.of(UserKind.OBSERVER, ObserverView::class.java),
            Arguments.of(UserKind.SUPERVISOR, SupervisorView::class.java),
        )

        @JvmStatic
        fun guestTitles(): List<Arguments> = listOf(
            Arguments.of(AuthenticationView::class.java, "Вход"),
            Arguments.of(RegistrationView::class.java, "Регистрация"),
            Arguments.of(RestoreAccessView::class.java, "Восстановление доступа"),
        )
    }
}
