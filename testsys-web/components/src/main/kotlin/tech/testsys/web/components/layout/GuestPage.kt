@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Text
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Main
import com.vaadin.flow.component.html.Paragraph
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.html.Span
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.buildBrand
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.texts.UiLocalization
import tech.testsys.web.components.texts.currentUiLocalization

/**
 * Scope of a guest page: one card in the middle and a line of links under it.
 *
 * @since 1.0.0-beta.1
 */
@TestSysDsl
class GuestPageScope internal constructor(private val main: Main, private val texts: UiLocalization) {
    private var hasCard = false
    private var hasLinks = false

    /**
     * Adds the card titled [title] with one explaining [description], the [step] label of a multi-step scenario before
     * it, if any, and the fields and the main action built by [content]. The card stays above the links.
     *
     * @throws IllegalArgumentException if [title] or [description] is blank.
     * @throws IllegalStateException if the page already has a card.
     * @since 1.0.0-beta.1
     */
    fun card(title: String, description: String, step: String? = null, content: GuestCardScope.() -> Unit) {
        require(title.isNotBlank()) { "Guest card title must not be blank" }
        require(description.isNotBlank()) { "Guest card '$title' needs a description" }
        check(!hasCard) { "Guest page already has a card, cannot add '$title'" }
        hasCard = true

        val explanation = Paragraph().apply { addClassName(CssClass.GuestCardDesc) }
        step?.let { label -> explanation.add(Span(label).apply { addClassName(CssClass.GuestCardStep) }) }
        explanation.add(Text(description))

        val body = Div().apply { addClassName(CssClass.GuestCardBody) }
        val card = Section(H1(title).apply { addClassName(CssClass.GuestCardTitle) }, explanation, body)
        card.addClassName(CssClass.GuestCard)
        main.addComponentAtIndex(BRAND_PARTS, card)
        GuestCardScope(ContentScope(body, texts, Placement.Body)).content()
    }

    /**
     * Adds the line under the card built by [content], usually a short text and link actions; consecutive link
     * actions are separated by a middle dot.
     *
     * @throws IllegalStateException if the page already has links.
     * @since 1.0.0-beta.1
     */
    fun links(content: ContentScope.() -> Unit) {
        check(!hasLinks) { "Guest page already has links" }
        hasLinks = true

        val line = Div().apply { addClassName(CssClass.GuestLinks) }
        main.add(line)
        ContentScope(line, texts, Placement.Body).content()
        separateLinks(line)
    }

    private fun separateLinks(line: Div) {
        line.children.toList().zipWithNext()
            .filter { (previous, next) -> previous is Button && next is Button }
            .forEach { (_, next) -> line.addComponentAtIndex(line.indexOf(next), middleDot()) }
    }

    private fun middleDot(): Component = Span("·").apply {
        addClassName(CssClass.GuestLinksSeparator)
        element.setAriaHidden(true)
    }
}

/**
 * Scope of the card of a guest page: fields with the label above, a segmented choice and the wide main action, one
 * under another.
 *
 * @since 1.0.0-beta.1
 */
@TestSysDsl
class GuestCardScope internal constructor(internal val body: ContentScope)

/** Number of the leading children of a guest page `main`: the brand and the tagline. */
private const val BRAND_PARTS = 2

/**
 * Replaces the content of [root] with the guest `.ts-app`: `main` with the brand, the [tagline] and the content built by
 * [body], and the footer; there is no header, page head or guide.
 */
internal fun renderGuestPage(root: Div, tagline: String, body: GuestPageScope.() -> Unit) {
    val texts = currentUiLocalization()
    val brand = buildBrand(texts.messages.uiPage.brandName()).apply { addClassName(CssClass.GuestBrand) }
    val main = Main(brand, Div(tagline).apply { addClassName(CssClass.GuestTagline) }).apply { addClassName(CssClass.Guest) }

    root.removeAll()
    root.setClassName(CssClass.App)
    root.addClassNames(CssClass.AppGuest)
    root.add(main)
    GuestPageScope(main, texts).body()
    root.add(buildPageFooter(texts))
}
