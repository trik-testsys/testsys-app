@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssProperty
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setToken
import tech.testsys.web.components.layout.BlockScope

/**
 * Categories of illustrations of the canonical visual foundations.
 *
 * @since %CURRENT_VERSION%
 */
enum class FoundationCategory { Palette, Typography, Layout }

/**
 * Fills a block with canonical foundation samples; [sampleText] is application-provided typography content.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockScope.foundationSamples(
    category: FoundationCategory,
    sampleText: String = "",
    configure: ElementHandle.() -> Unit = {},
): ElementHandle {
    val samples = Div().apply { addClassName(CssClass.FoundationSamples) }
    foundationTokens(category).forEach { name ->
        val example = Div(
            when (category) {
                FoundationCategory.Typography -> sampleText
                FoundationCategory.Palette -> ""
                FoundationCategory.Layout -> name
            },
        ).apply {
            addClassName(CssClass.FoundationSample)
            val property = when {
                category == FoundationCategory.Palette -> CssProperty.Background
                name.startsWith("--font-") -> CssProperty.FontFamily
                name.startsWith("--fs-") -> CssProperty.FontSize
                name.startsWith("--fw-") -> CssProperty.FontWeight
                name.startsWith("--space-") -> CssProperty.Padding
                name.startsWith("--radius-") -> CssProperty.BorderRadius
                else -> CssProperty.BoxShadow
            }
            element.style.setToken(property, name)
        }
        samples.add(Div(Div(name).apply { addClassName(CssClass.Mono) }, example).apply { addClassName(CssClass.FoundationRow) })
    }
    placeWhole(samples, "foundation samples")
    return ElementHandle(samples).apply(configure)
}

private fun foundationTokens(category: FoundationCategory): List<String> {
    val file = when (category) {
        FoundationCategory.Palette -> "colors"
        FoundationCategory.Typography -> "typography"
        FoundationCategory.Layout -> "spacing"
    }
    val path = "/META-INF/resources/testsys-ui/tokens/$file.css"
    val source = checkNotNull(FoundationCategory::class.java.getResource(path)) { "Token file $path of $category is missing" }.readText()
    val names = Regex("(--[a-zA-Z0-9-]+)\\s*:").findAll(source).map { match -> match.groupValues[1] }.distinct()
    return names.filter { name ->
        when (category) {
            FoundationCategory.Palette -> true
            FoundationCategory.Typography -> name.startsWith("--font-") || name.startsWith("--fs-") || name.startsWith("--fw-")
            FoundationCategory.Layout -> name.startsWith("--space-") || name.startsWith("--radius-") || name.startsWith("--shadow-")
        }
    }.toList()
}
