package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
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
    fun `should forward a guest to the sign-in page`() {
        UI.getCurrent().navigate(MainView::class.java)

        assertEquals(AuthenticationView::class.java, currentView)
    }
}
