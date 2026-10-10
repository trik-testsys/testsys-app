package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class MainViewTests : MockSpringVaadinTests() {
    @Test
    fun `should forward a signed-in user to their start page`() {
        UI.getCurrent().navigate(AuthenticationView::class.java)
        signIn(fixtures.userOf(UserKind.PARTICIPANT))

        UI.getCurrent().navigate(MainView::class.java)

        assertEquals(ParticipantView::class.java, currentView)
    }

    @Test
    fun `should open the sign-in page by the sign-in action`() {
        UI.getCurrent().navigate(MainView::class.java)

        UI.getCurrent()._get<Button> { text = "Войти" }._click()

        assertEquals(AuthenticationView::class.java, currentView)
    }

    @Test
    fun `should open the registration page by the registration action`() {
        UI.getCurrent().navigate(MainView::class.java)

        UI.getCurrent()._get<Button> { text = "Зарегистрироваться" }._click()

        assertEquals(RegistrationView::class.java, currentView)
    }
}
