package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.UiTexts
import java.time.Clock
import java.time.Year

/**
 * Scope of the page footer: links in display order, opened in the current tab.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageFooterScope internal constructor() {
    private val links = mutableListOf<Component>()

    /**
     * Adds a link named [label] that opens [target] with [parameters].
     *
     * @since %CURRENT_VERSION%
     */
    fun link(label: String, target: Class<out Component>, parameters: RouteParameters = RouteParameters.empty()) {
        links += RouterLink(label, target, parameters).apply { addClassName("ts-footer__link") }
    }

    /**
     * Adds a link named [label] that opens [href].
     *
     * @since %CURRENT_VERSION%
     */
    fun link(label: String, href: String) {
        links += Anchor(href, label).apply { addClassName("ts-footer__link") }
    }

    internal fun build(ariaLabel: String): Nav? {
        if (links.isEmpty()) return null
        return Nav().apply {
            addClassName("ts-footer__links")
            element.setAttribute("aria-label", ariaLabel)
            links.forEach { link -> add(link) }
        }
    }
}

internal fun buildPageFooter(
    texts: UiTexts,
    links: PageFooterScope = PageFooterScope(),
    clock: Clock = Clock.systemDefaultZone(),
): Footer {
    val logo = Image(TestSysBrand.FOOTER, texts.brand).apply { addClassName("ts-footer__logo") }
    val year = Span(texts.footer.year(Year.now(clock).value)).apply { addClassName("ts-footer__year") }
    val brand = Div(logo, year).apply { addClassName("ts-footer__brand") }
    val inner = Div(brand).apply { addClassName("ts-footer__inner") }
    links.build(texts.footer.links)?.let { navigation -> inner.add(navigation) }
    return Footer(inner).apply { addClassName("ts-footer") }
}
