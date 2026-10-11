@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Footer
import com.vaadin.flow.component.html.Main
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.navigation.PageHeadScope
import tech.testsys.web.components.navigation.placePageGuideButton
import tech.testsys.web.components.overlay.GuideStep
import tech.testsys.web.components.overlay.guideButton
import tech.testsys.web.components.texts.UiTexts

/**
 * Scope of a page: the head, rows and the links of its footer.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class PageScope internal constructor(
    private val main: Main,
    private val texts: UiTexts,
    private val view: Class<out Component>?,
    private val placeHead: (Component) -> Unit,
) {
    private var footerLinks: PageFooterScope? = null
    private var isHeadAllowed = true
    private var guideDescription: String? = null
    private var pageHead: Div? = null
    private var pageTitle = texts.brand

    /**
     * Adds the page introduction before block explanations in display order, with an information button in its head.
     *
     * @throws IllegalArgumentException if [description] is blank.
     * @throws IllegalStateException if the guide is already configured.
     * @since %CURRENT_VERSION%
     */
    fun guide(description: String) {
        check(guideDescription == null) { "Page guide is already configured; call guide() once" }
        require(description.isNotBlank()) { "Page guide description must not be blank" }
        guideDescription = description
    }

    internal fun finishGuide() {
        guideDescription?.let { description ->
            val head = pageHead ?: PageHeadScope(texts, view).build(pageTitle).also(placeHead)
            GuideStep(head, title = pageTitle, description = description).install()
            head.element.setAttribute(HtmlAttribute.DataGuideIntroduction, "")
            placePageGuideButton(head, guideButton(texts.guide.page, isPage = true))
        }
    }

    /**
     * Adds the page head under the Cabinet header: breadcrumbs that end with [title], the title with badges, notes and
     * actions, and the tabs of the sections of one object.
     *
     * @throws IllegalStateException if called after a row or footer configuration, or repeated.
     * @since %CURRENT_VERSION%
     */
    fun head(title: String, content: PageHeadScope.() -> Unit = {}) {
        check(isHeadAllowed) { "head() must be the first call of the page body and made once" }
        isHeadAllowed = false
        pageTitle = title
        pageHead = PageHeadScope(texts, view).apply(content).build(title).also(placeHead)
    }

    /**
     * Configures the links of the automatic page footer, which stays after the page body.
     *
     * @throws IllegalStateException if the footer links have already been configured.
     * @since %CURRENT_VERSION%
     */
    fun footer(content: PageFooterScope.() -> Unit) {
        check(footerLinks == null) { "Page footer already has links; call footer() once" }
        isHeadAllowed = false
        footerLinks = PageFooterScope().apply(content)
    }

    /**
     * Adds a row of the 24-column page grid below the previous rows; its blocks lie side by side.
     *
     * @since %CURRENT_VERSION%
     */
    fun row(content: PageRowScope.() -> Unit) {
        isHeadAllowed = false
        val row = Div().apply { addClassName(CssClass.Row) }
        main.add(row)
        PageRowScope(row, texts).content()
    }

    internal fun buildFooter(): Footer = buildPageFooter(texts, footerLinks ?: PageFooterScope())
}
