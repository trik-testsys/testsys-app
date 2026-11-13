@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.component.html.Nav
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.RouteParameters
import com.vaadin.flow.router.RouterLink
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.texts.UiLocalization
import java.time.Clock
import java.time.Year

/**
 * Scope of the page footer: links in display order, opened in the current tab.
 *
 * @since 1.0.0-beta.1
 */
@TestSysDsl
class PageFooterScope internal constructor() {
    private val links = mutableListOf<Component>()

    /**
     * Adds a link named [label] that opens [target] with [parameters].
     *
     * @since 1.0.0-beta.1
     */
    fun link(label: String, target: Class<out Component>, parameters: RouteParameters = RouteParameters.empty()) {
        links += RouterLink(label, target, parameters).apply { addClassName(CssClass.FooterLink) }
    }

    /**
     * Adds a link named [label] that opens [href].
     *
     * @since 1.0.0-beta.1
     */
    fun link(label: String, href: String) {
        links += Anchor(href, label).apply { addClassName(CssClass.FooterLink) }
    }

    internal fun build(ariaLabel: String): Nav? {
        if (links.isEmpty()) return null
        return Nav().apply {
            addClassName(CssClass.FooterLinks)
            element.setAttribute(HtmlAttribute.AriaLabel, ariaLabel)
            links.forEach { link -> add(link) }
        }
    }
}

/**
 * Sets the links that every footer built on [ui] adds after the links of its page: of a page, a guest page and an
 * error screen. [links] runs at each footer build, so the set can depend on the state at that moment, such as whether
 * the user is signed in; a repeated call replaces the set.
 *
 * @since %CURRENT_VERSION%
 */
fun setCommonFooterLinks(ui: UI, links: PageFooterScope.() -> Unit) {
    ComponentUtil.setData(ui, CommonFooterLinks::class.java, CommonFooterLinks(links))
}

/** Key and value of the common footer links in the data of a UI. */
private class CommonFooterLinks(val content: PageFooterScope.() -> Unit)

internal fun buildPageFooter(
    texts: UiLocalization,
    links: PageFooterScope = PageFooterScope(),
    clock: Clock = Clock.systemDefaultZone(),
): Footer {
    val name = Span(texts.messages.uiPage.brandName()).apply { addClassName(CssClass.FooterName) }
    val year = Span(texts.messages.uiPage.footerYear(year = Year.now(clock).value)).apply { addClassName(CssClass.FooterYear) }
    val brand = Div(name, year).apply { addClassName(CssClass.FooterBrand) }
    val inner = Div(brand).apply { addClassName(CssClass.FooterInner) }
    val common = UI.getCurrent()?.let { ui -> ComponentUtil.getData(ui, CommonFooterLinks::class.java) }
    common?.let { commonLinks -> links.apply(commonLinks.content) }
    links.build(texts.messages.uiPage.footerLinks())?.let { navigation -> inner.add(navigation) }
    return Footer(inner).apply { addClassName(CssClass.Footer) }
}
