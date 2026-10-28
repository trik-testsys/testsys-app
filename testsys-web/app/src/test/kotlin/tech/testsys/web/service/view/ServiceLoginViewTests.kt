package tech.testsys.web.service.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.server.VaadinSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import tech.testsys.web.service.SERVICE_TEST_SECRET
import tech.testsys.web.service.ServiceMockVaadinTests
import tech.testsys.web.service.security.ServicePrincipal

class ServiceLoginViewTests : ServiceMockVaadinTests() {
    @Test
    fun `should authenticate rotate the session id clear the field and open the home page`() {
        UI.getCurrent().navigate(ServiceLoginView::class.java)
        val field = UI.getCurrent()._get<PasswordField>()
        val sessionId = VaadinSession.getCurrent().session.id
        field.value = SERVICE_TEST_SECRET

        UI.getCurrent()._get<Button> { text = "Войти" }._click()

        assertEquals(ServiceHomeView::class.java, currentView)
        assertEquals(ServicePrincipal, SecurityContextHolder.getContext().authentication?.principal)
        assertNull(SecurityContextHolder.getContext().authentication?.credentials)
        assertNotEquals(sessionId, VaadinSession.getCurrent().session.id)
        assertEquals("", field.value)
    }

    @Test
    fun `should keep a guest on login and clear the rejected code`() {
        UI.getCurrent().navigate(ServiceLoginView::class.java)
        val field = UI.getCurrent()._get<PasswordField>()
        field.value = "invalid"

        UI.getCurrent()._get<Button> { text = "Войти" }._click()

        assertEquals(ServiceLoginView::class.java, currentView)
        assertEquals("", field.value)
    }

    @Test
    fun `should redirect a guest away from the service home`() {
        UI.getCurrent().navigate(ServiceHomeView::class.java)

        assertEquals(ServiceLoginView::class.java, currentView)
    }

    @Test
    fun `should show the block on the fifth failure and reject a correct code from the same address`() {
        UI.getCurrent().navigate(ServiceLoginView::class.java)
        val field = UI.getCurrent()._get<PasswordField>()
        repeat(5) {
            field.value = "invalid"
            UI.getCurrent()._get<Button> { text = "Войти" }._click()
        }
        field.value = SERVICE_TEST_SECRET

        UI.getCurrent()._get<Button> { text = "Войти" }._click()

        assertEquals(ServiceLoginView::class.java, currentView)
        assertEquals("", field.value)
        assertEquals(
            "Вход временно заблокирован",
            UI.getCurrent()._find<Span> { classes = "ts-toast__title" }.last().text,
        )
    }
}
