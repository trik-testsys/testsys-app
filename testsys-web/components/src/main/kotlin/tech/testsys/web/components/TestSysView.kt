package tech.testsys.web.components

import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.layout.PageScope
import tech.testsys.web.components.layout.renderPage
import tech.testsys.web.components.navigation.CabinetHeader

/**
 * Base class of a Cabinet page built with the design system DSL.
 *
 * @since %CURRENT_VERSION%
 */
abstract class TestSysView(protected val texts: UiTexts) : Composite<Div>() {
    private var isBuilding = false

    /**
     * Builds or rebuilds the page: [header] and the rows and blocks built by [body]. Vaadin reuses the view instance
     * when navigating again to the same route class (e.g. between page tabs of one object with different route
     * parameters), so a page with a route parameter (`@Route("…/:id")`) calls this from `beforeEnter` of
     * a `BeforeEnterObserver`; the new body replaces the old one.
     *
     * @throws IllegalStateException if called again from inside a [body] that is still being built.
     */
    protected fun page(header: CabinetHeader, body: PageScope.() -> Unit) {
        check(!isBuilding) { "TestSysView.page() called again from inside its own body for ${this::class.simpleName}" }
        isBuilding = true
        try {
            renderPage(content, header, texts, this::class.java, body)
        } finally {
            isBuilding = false
        }
    }
}
