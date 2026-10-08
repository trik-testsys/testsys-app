package tech.testsys.web.app.error

import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.expectView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class AccessDeniedViewTests : MockSpringVaadinTests() {
    @Test
    fun `should show the forbidden screen with 403 if a signed-in user of another kind opens the page`() {
        signIn(fixtures.userOf(UserKind.PARTICIPANT))
        val ui = UI.getCurrent()

        val result = ui.internals.router.navigate(ui, Location("developer"), NavigationTrigger.PAGE_LOAD)

        assertEquals(403, result)
        expectView<AccessDeniedView>()
        assertEquals("Нет доступа", _get<H1>().text)
    }
}
