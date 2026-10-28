package tech.testsys.web.components

import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.layout.GuestPageScope
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.layout.renderGuestPage
import tech.testsys.web.components.layout.renderPage
import tech.testsys.web.components.navigation.header.CabinetHeader

/**
 * Base class of a Cabinet or guest page built with the design system DSL; built-in texts come from the localization bound to
 * the current UI.
 *
 * @since %CURRENT_VERSION%
 */
abstract class TestSysView : Composite<Div>() {
    private var isBuilding = false

    /**
     * Builds or rebuilds the page: [header], the rows and blocks built by [body], and the automatic footer. Vaadin reuses
     * the view instance
     * when navigating again to the same route class (e.g. between page tabs of one object with different route
     * parameters), so a page with a route parameter (`@Route("…/:id")`) calls this from `beforeEnter` of
     * a `BeforeEnterObserver`; the new body replaces the old one.
     *
     * @throws IllegalStateException if called again from inside a [body] that is still being built.
     */
    protected fun page(header: CabinetHeader, body: PageScope.() -> Unit) {
        build("page") { renderPage(content, header, this::class.java, body) }
    }

    /**
     * Builds or rebuilds a guest page, for example sign-in: the brand with the [tagline], the card and the links built
     * by [body], and the automatic footer, without a header, a page head or guides. A repeated call replaces the page.
     *
     * @throws IllegalStateException if called from inside a body that is still being built.
     */
    protected fun guestPage(tagline: String, body: GuestPageScope.() -> Unit) {
        build("guestPage") { renderGuestPage(content, tagline, body) }
    }

    private fun build(entry: String, render: () -> Unit) {
        check(!isBuilding) { "TestSysView.$entry() called again from inside its own body for ${this::class.simpleName}" }

        isBuilding = true
        try {
            render()
        } finally {
            isBuilding = false
        }
    }
}
