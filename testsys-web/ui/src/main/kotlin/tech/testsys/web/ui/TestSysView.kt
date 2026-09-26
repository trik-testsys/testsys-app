package tech.testsys.web.ui

import com.vaadin.flow.component.Composite
import com.vaadin.flow.component.html.Div
import tech.testsys.web.ui.layout.PageScope
import tech.testsys.web.ui.layout.renderPage
import tech.testsys.web.ui.navigation.CabinetHeader

/**
 * Base class of a Cabinet page built with the design system DSL.
 *
 * @since %CURRENT_VERSION%
 */
abstract class TestSysView(protected val texts: UiTexts) : Composite<Div>() {
    private var isPageBuilt = false

    /** Builds the page once: [header] and the rows and blocks built by [body]. */
    protected fun page(header: CabinetHeader, body: PageScope.() -> Unit) {
        check(!isPageBuilt) { "TestSysView.page() is already built for ${this::class.simpleName}; call it once" }
        isPageBuilt = true
        renderPage(content, header, texts, body)
    }
}
