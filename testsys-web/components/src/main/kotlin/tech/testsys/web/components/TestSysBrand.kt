package tech.testsys.web.components

import com.vaadin.flow.component.HtmlContainer
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span

/**
 * Canonical brand assets served by the components module.
 *
 * @property EMBLEM the transparent emblem displayed alongside the wordmark.
 * @property WORDMARK the transparent graphic product name.
 * @property FAVICON the emblem with its background used as the application favicon.
 * @since %CURRENT_VERSION%
 */
object TestSysBrand {
    const val EMBLEM: String = "design-system/assets/brand/emblem/svg/TestSys-emblem-transparent.svg"
    const val WORDMARK: String = "design-system/assets/brand/wordmark/svg/TestSys-wordmark-transparent.svg"
    const val FAVICON: String = "design-system/assets/brand/emblem/svg/TestSys-emblem.svg"
}

internal fun buildBrand(name: String, href: String? = null): HtmlContainer {
    val container = if (href == null) Span().apply { element.setAttribute("role", "img") } else Anchor(href)
    container.addClassName("ts-brand")
    container.element.setAttribute("aria-label", name)
    container.add(
        Image(TestSysBrand.EMBLEM, "").apply { addClassName("ts-brand__emblem") },
        Image(TestSysBrand.WORDMARK, "").apply { addClassName("ts-brand__wordmark") },
    )
    return container
}
