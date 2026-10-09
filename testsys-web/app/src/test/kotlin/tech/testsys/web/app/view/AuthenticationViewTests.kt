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
import com.vaadin.flow.server.VaadinSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.domain.builder.api.developerData
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.CabinetPrincipal
import tech.testsys.web.app.security.CabinetSignIn
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class AuthenticationViewTests : MockSpringVaadinTests() {
    @ParameterizedTest
    @MethodSource("startPages")
    fun `should sign the user in and open the start page of their kind`(kind: UserKind, startPage: Class<out Component>) {
        val accessToken = fixtures.unique("token")
        val user = fixtures.userOf(kind, rawAccessToken = accessToken)
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInWith(accessToken)

        assertEquals(startPage, currentView)
        assertEquals(CabinetPrincipal.of(user), CabinetSignIn.principal())
    }

    @Test
    fun `should show that the access code is invalid and stay signed out if no user has it`() {
        UI.getCurrent().navigate(AuthenticationView::class.java)

        signInWith("unknown access code")

        assertEquals(AuthenticationView::class.java, currentView)
        assertEquals("Код-доступа недействителен", UI.getCurrent()._get<Span> { classes = "ts-toast__title" }.text)
        assertNull(CabinetSignIn.principal())
    }

    @Test
    fun `should compare the access code exactly`() {
        val accessToken = fixtures.unique("token")
        fixtures.userOf(UserKind.SUPERVISOR, rawAccessToken = accessToken)
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
        val accessToken = fixtures.unique("token")
        fixtures.multipleRoleUser(rawAccessToken = accessToken) { roles { developer { data = developerData {} } } }
        UI.getCurrent().navigate(DeveloperView::class.java)

        signInWith(accessToken)

        assertEquals(DeveloperView::class.java, currentView)
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

    private fun signInWith(accessToken: String) {
        UI.getCurrent()._find<PasswordField>().single().value = accessToken
        UI.getCurrent()._get<Button> { text = "Войти" }._click()
    }

    companion object {
        @JvmStatic
        fun startPages(): List<Arguments> = listOf(
            Arguments.of(UserKind.MULTIPLE_ROLE, MultiMainView::class.java),
            Arguments.of(UserKind.PARTICIPANT, ParticipantView::class.java),
            Arguments.of(UserKind.OBSERVER, ObserverView::class.java),
            Arguments.of(UserKind.SUPERVISOR, SupervisorView::class.java),
        )
    }
}
