package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class PendingViewsTests : MockSpringVaadinTests() {
    @Test
    fun `should show no breadcrumbs on the start page of a fixed role`() {
        signIn(fixtures.userOf(UserKind.OBSERVER))

        UI.getCurrent().navigate(ObserverView::class.java)

        assertTrue(crumbs().isEmpty())
    }

    private fun crumbs(): Map<String, String> = UI.getCurrent()._find<Nav> { classes = "ts-crumbs" }
        .flatMap { nav -> nav._find<RouterLink>() }
        .associate { link -> link.text to link.href }
}
