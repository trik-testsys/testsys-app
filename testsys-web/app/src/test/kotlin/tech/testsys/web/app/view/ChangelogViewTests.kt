package tech.testsys.web.app.view

import com.github.mvysny.kaributesting.v10._find
import com.github.mvysny.kaributesting.v10._get
import com.github.mvysny.kaributesting.v10.currentView
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.H3
import com.vaadin.flow.router.RouterLink
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.security.UserKind

@SpringBootTest
class ChangelogViewTests : MockSpringVaadinTests() {
    @ParameterizedTest
    @EnumSource(UserKind::class)
    fun `should show the versions and link the changelog in the footer for a signed-in user`(kind: UserKind) {
        signIn(fixtures.userOf(kind))

        UI.getCurrent().navigate(ChangelogView::class.java)

        assertEquals(ChangelogView::class.java, currentView)
        assertTrue("1.0.0-beta.1" in _find<H3> { classes = "ts-block__title" }.map { title -> title.text })
        assertEquals("changelog", _get<RouterLink> { classes = "ts-footer__link" }.href)
    }

    @Test
    fun `should send a guest to sign in without the changelog link in the footer`() {
        UI.getCurrent().navigate(ChangelogView::class.java)

        assertEquals(AuthenticationView::class.java, currentView)
        assertEquals(emptyList<RouterLink>(), _find<RouterLink> { classes = "ts-footer__link" })
    }
}
