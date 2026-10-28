@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components

import com.vaadin.flow.component.HtmlContainer
import com.vaadin.flow.component.html.Anchor
import com.vaadin.flow.component.html.Image
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementRole
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setRole

/**
 * Canonical brand assets served by the components module.
 *
 * @property EMBLEM the transparent monochrome emblem.
 * @property WORDMARK the transparent graphic product name.
 * @property ICON the light emblem on a brand-color square, displayed with the wordmark in headers.
 * @property FOOTER the transparent horizontal logo displayed in page footers.
 * @property FAVICON the canonical resource path of the rounded emblem with its background.
 * @property FAVICON_URL the versioned favicon URL used by browsers.
 * @since %CURRENT_VERSION%
 */
object TestSysBrand {
    const val EMBLEM: String = "testsys-ui/brand/emblem/svg/TestSys-mono-emblem-transparent.svg"
    const val WORDMARK: String = "testsys-ui/brand/wordmark/svg/TestSys-mono-wordmark-transparent.svg"
    const val ICON: String = "testsys-ui/brand/emblem/svg/TestSys-mono-emblem-inverse.svg"
    const val FAVICON: String = "testsys-ui/brand/emblem/svg/TestSys-mono-emblem.svg"
    const val FAVICON_URL: String = "$FAVICON?v=rounded-96"
    const val FOOTER: String =
        "testsys-ui/brand/horizontal-large-type/svg/TestSys-mono-horizontal-large-type-07-transparent-blue.svg"
}

internal fun buildBrand(name: String, href: String? = null): HtmlContainer {
    val container = if (href == null) Span().apply { element.setRole(ElementRole.Image) } else Anchor(href)
    container.addClassName(CssClass.Brand)
    container.element.setAttribute(HtmlAttribute.AriaLabel, name)
    container.add(
        Image(TestSysBrand.ICON, "").apply { addClassName(CssClass.BrandEmblem) },
        Image(TestSysBrand.WORDMARK, "").apply { addClassName(CssClass.BrandWordmark) },
    )
    return container
}
