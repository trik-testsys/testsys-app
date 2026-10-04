package tech.testsys.web.components

import com.vaadin.flow.component.HtmlContainer
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span

/**
 * Canonical brand assets served by the components module.
 *
 * @property EMBLEM the transparent monochrome emblem.
 * @property WORDMARK the transparent graphic product name.
 * @property HEADER the horizontal logo displayed in headers and on the missing-route page.
 * @property FOOTER the transparent horizontal logo displayed in page footers.
 * @property FAVICON the canonical resource path of the rounded emblem with its background.
 * @property FAVICON_URL the versioned favicon URL used by browsers.
 * @since %CURRENT_VERSION%
 */
object TestSysBrand {
    const val EMBLEM: String = "testsys-ui/brand/emblem/svg/TestSys-mono-emblem-transparent.svg"
    const val WORDMARK: String = "testsys-ui/brand/wordmark/svg/TestSys-mono-wordmark-transparent.svg"
    const val HEADER: String =
        "testsys-ui/brand/horizontal-large-type/png/TestSys-mono-horizontal-large-type-05-split-cream.png"
    const val FAVICON: String = "testsys-ui/brand/emblem/svg/TestSys-mono-emblem.svg"
    const val FAVICON_URL: String = FAVICON + "?v=rounded-96"
    const val FOOTER: String =
        "testsys-ui/brand/horizontal-large-type/svg/TestSys-mono-horizontal-large-type-07-transparent-blue.svg"
}

internal fun buildBrand(name: String, href: String? = null): HtmlContainer {
    val container = if (href == null) Span().apply { element.setAttribute("role", "img") } else Anchor(href)
    container.addClassName("ts-brand")
    container.element.setAttribute("aria-label", name)
    container.add(Image(TestSysBrand.HEADER, "").apply { addClassName("ts-brand__logo") })
    return container
}
