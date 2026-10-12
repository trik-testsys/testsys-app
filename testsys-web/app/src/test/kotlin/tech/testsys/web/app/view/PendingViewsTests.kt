package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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

    @ParameterizedTest
    @MethodSource("titles")
    fun `should title the head and the browser tab of a pending Cabinet`(kind: UserKind, view: Class<out Component>, title: String) {
        signIn(fixtures.userOf(kind))

        UI.getCurrent().navigate(view)

        assertEquals(title, assertInstanceOf(HasDynamicTitle::class.java, UI.getCurrent().currentView).pageTitle)
        assertEquals(title, UI.getCurrent()._get<H1>().text)
    }

    private fun crumbs(): Map<String, String> = UI.getCurrent()._find<Nav> { classes = "ts-crumbs" }
        .flatMap { nav -> nav._find<RouterLink>() }
        .associate { link -> link.text to link.href }

    companion object {
        @JvmStatic
        fun titles(): List<Arguments> = listOf(
            Arguments.of(UserKind.OBSERVER, ObserverView::class.java, "Кабинет Наблюдателя"),
            Arguments.of(UserKind.SUPERVISOR, SupervisorView::class.java, "Кабинет Супервайзера"),
        )
    }
}
