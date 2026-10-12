package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._click
import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10._value
import com.github.mvysny.kaributesting.v10.currentView
import com.github.mvysny.kaributesting.v10.expectView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Table
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Location
import com.vaadin.flow.router.NavigationTrigger
import com.vaadin.flow.router.RouteParameters
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.error.AccessDeniedView
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class SupervisorViewTests : MockSpringVaadinTests() {
    @Test
    fun `should deny the cabinet to a signed-in user without the supervisor role`() {
        signIn(fixtures.administrator())
        val ui = UI.getCurrent()

        val result = ui.internals.router.navigate(ui, Location("supervisor/users"), NavigationTrigger.PAGE_LOAD)

        assertEquals(403, result)
        expectView<AccessDeniedView>()
    }

    @Test
    fun `should filter users without the supervisor and open the communities tab`() {
        val supervisor = fixtures.supervisor()
        signIn(supervisor)
        val first = fixtures.community(name = "Первое сообщество")
        fixtures.community(name = "Второе сообщество")
        fixtures.observer(first, name = "Наблюдатель Анна")

        UI.getCurrent().navigate(SupervisorView::class.java, section("users"))
        field("Псевдоним")._value = "Наблюдатель Анна"
        UI.getCurrent()._get<Button> { text = "Применить" }._click()

        val users = tableText()
        assertTrue("Наблюдатель Анна" in users, users)
        field("Псевдоним")._value = supervisor.data.name
        UI.getCurrent()._get<Button> { text = "Применить" }._click()
        assertFalse(supervisor.data.name in tableText())

        UI.getCurrent().navigate(SupervisorView::class.java, section("communities"))

        assertEquals(SupervisorView::class.java, currentView)
        assertTrue(UI.getCurrent()._find<Table>().isNotEmpty())
    }

    @Test
    fun `should rebuild the users table when returning from communities in the same view`() {
        signIn(fixtures.userOf(UserKind.SUPERVISOR))
        val observer = fixtures.observer(fixtures.community(), name = "Наблюдатель Пётр")
        UI.getCurrent().navigate(SupervisorView::class.java, section("users"))
        val firstView = UI.getCurrent()._get<SupervisorView>()

        UI.getCurrent().navigate(SupervisorView::class.java, section("communities"))
        UI.getCurrent().navigate(SupervisorView::class.java, section("users"))

        assertSame(firstView, UI.getCurrent()._get<SupervisorView>())
        assertEquals(SupervisorView::class.java, currentView)
        field("Псевдоним")._value = observer.data.name
        UI.getCurrent()._get<Button> { text = "Применить" }._click()
        assertTrue(observer.data.name in tableText())
    }

    private fun section(name: String) = RouteParameters(ADMIN_SECTION_PARAMETER, name)

    private fun field(label: String): TextField = UI.getCurrent()._find<TextField>().single { it.ariaLabel.orElse(null) == label }

    private fun tableText(): String = UI.getCurrent()._find<Table>().single().element.textRecursively
}
