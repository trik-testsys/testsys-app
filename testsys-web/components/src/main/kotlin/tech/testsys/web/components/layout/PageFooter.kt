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

/**
 * Sets the version of the system that every footer built on [ui] shows after the brand name; with `null` the footer
 * shows no version.
 *
 * @since %CURRENT_VERSION%
 */
fun setFooterVersion(ui: UI, version: String?) {
    ComponentUtil.setData(ui, FooterVersion::class.java, version?.let(::FooterVersion))
}

/** Key and value of the footer version in the data of a UI. */
private class FooterVersion(val value: String)

internal fun buildPageFooter(texts: UiLocalization, links: PageFooterScope = PageFooterScope()): Footer {
    val ui = UI.getCurrent()
    val name = Span(texts.messages.uiPage.brandName()).apply { addClassName(CssClass.FooterName) }
    val brand = Div(name).apply { addClassName(CssClass.FooterBrand) }
    ui?.let { ComponentUtil.getData(it, FooterVersion::class.java) }?.let { version ->
        brand.add(Span(version.value).apply { addClassName(CssClass.FooterVersion) })
    }
    val inner = Div().apply { addClassName(CssClass.FooterInner) }
    ui?.let { ComponentUtil.getData(it, CommonFooterLinks::class.java) }?.let { common -> links.apply(common.content) }
    links.build(texts.messages.uiPage.footerLinks())?.let { navigation -> inner.add(navigation) }
    inner.add(brand)
    return Footer(inner).apply { addClassName(CssClass.Footer) }
}
