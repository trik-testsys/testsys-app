package tech.testsys.web.components.display

import com.vaadin.flow.component.html.Div
import tech.testsys.web.components.ElementHandle
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
    val samples = Div().apply { addClassName("ts-foundation-samples") }
    foundationTokens(category).forEach { name ->
        val example = Div(
            when (category) {
                FoundationCategory.Typography -> sampleText
                FoundationCategory.Palette -> ""
                FoundationCategory.Layout -> name
            },
        ).apply {
            addClassName("ts-foundation-sample")
            val property = when {
                category == FoundationCategory.Palette -> "background"
                name.startsWith("--font-") -> "font-family"
                name.startsWith("--fs-") -> "font-size"
                name.startsWith("--fw-") -> "font-weight"
                name.startsWith("--space-") -> "padding"
                name.startsWith("--radius-") -> "border-radius"
                else -> "box-shadow"
            }
            element.style.set(property, "var($name)")
        }
        samples.add(Div(Div(name).apply { addClassName("ts-mono") }, example).apply { addClassName("ts-foundation-row") })
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
    val source = checkNotNull(FoundationCategory::class.java.getResource("/META-INF/resources/testsys-ui/tokens/$file.css")).readText()
    val names = Regex("(--[a-zA-Z0-9-]+)\\s*:").findAll(source).map { match -> match.groupValues[1] }.distinct()
    return names.filter { name ->
        when (category) {
            FoundationCategory.Palette -> true
            FoundationCategory.Typography -> name.startsWith("--font-") || name.startsWith("--fs-") || name.startsWith("--fw-")
            FoundationCategory.Layout -> name.startsWith("--space-") || name.startsWith("--radius-") || name.startsWith("--shadow-")
        }
    }.toList()
}
