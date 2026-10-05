@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Image
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysBrand
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Canonical brand images whose proportions and geometry belong to the components core.
 *
 * @since %CURRENT_VERSION%
 */
enum class BrandAsset(internal val path: String) {
    Header(TestSysBrand.HEADER),
    Footer(TestSysBrand.FOOTER),
    Emblem(TestSysBrand.EMBLEM),
    Wordmark(TestSysBrand.WORDMARK),
    Favicon(TestSysBrand.FAVICON),
}

/**
 * Adds the canonical [asset] with an application-provided accessible [label].
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.brandImage(asset: BrandAsset, label: String, configure: ElementHandle.() -> Unit = {}): ElementHandle {
    val image = buildBrandImage(asset, label)
    add(image)
    return ElementHandle(image).apply(configure)
}

/**
 * Adds a canonical brand image on [size] columns, or the remainder.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.brandImage(
    asset: BrandAsset,
    label: String,
    size: Int? = null,
    configure: ElementHandle.() -> Unit = {},
): ElementHandle = ElementHandle(place(size, buildBrandImage(asset, label))).apply(configure)

private fun buildBrandImage(asset: BrandAsset, label: String): Image {
    require(label.isNotBlank()) { "Brand image accessible name must not be blank" }
    return Image(asset.path, label).apply { addClassName(CssClass.BrandImage) }
}
