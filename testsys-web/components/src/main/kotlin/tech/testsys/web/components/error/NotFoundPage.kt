@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.error

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.html.Main
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.dom.Element
import com.vaadin.flow.router.HasDynamicTitle
import tech.testsys.web.components.actions.ActionRole
import tech.testsys.web.components.actions.setActionRole
import tech.testsys.web.components.buildBrand
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.ElementSize
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setSize
import tech.testsys.web.components.layout.buildPageFooter
import tech.testsys.web.components.texts.UiTexts

/**
 * Branded missing-route screen available in every profile, with browser-history navigation only.
 *
 * @since %CURRENT_VERSION%
 */
open class NotFoundPage(private val texts: UiTexts) : Div(), HasDynamicTitle {
    private val back = Button(texts.notFound.back).apply {
        element.setActionRole(ActionRole.Neutral)
        element.setSize(ElementSize.Medium)
        isEnabled = false
        addClickListener { ui.orElseThrow().page.history.back() }
    }

    init {
        addClassName(CssClass.App)
        val brand = buildBrand(texts.brand)
        val bar = Div(brand).apply { addClassName(CssClass.HeaderBar) }
        val header = Header(bar).apply { addClassName(CssClass.Header) }
        val heading = H1(texts.notFound.title).apply { addClassName(CssClass.H1) }
        val description = Div().apply {
            text = texts.notFound.description
            addClassName(CssClass.EmptyDesc)
        }
        val actions = Div(back).apply { addClassName(CssClass.EmptyActions) }
        val content = Div(heading, description, actions).apply { addClassName(CssClass.Empty) }
        val block = Section(content).apply { addClassName(CssClass.Block) }
        add(header, Main(block).apply { addClassName(CssClass.Page) }, buildPageFooter(texts))
    }

    /**
     * Resets history navigation until the browser reports the completed navigation state; every missing-route
     * handler calls it from `setErrorParameter`, otherwise the back action stays disabled.
     *
     * @since %CURRENT_VERSION%
     */
    fun refreshHistoryNavigation() {
        back.isEnabled = false
        // Flow updates history asynchronously; use its navigation completion event before reading the new entry.
        element.readCompletedHistory().then(Boolean::class.java) { canGoBack -> back.isEnabled = canGoBack }
    }

    override fun getPageTitle(): String = texts.notFound.pageTitle
}

private fun Element.readCompletedHistory() = executeJs(
    """
    return new Promise(resolve => {
        const readHistory = () => resolve(window.navigation ? window.navigation.canGoBack : window.history.length > 1);
        if (window.Vaadin?.Flow?.navigation) {
            window.addEventListener('vaadin-navigated', readHistory, {once: true});
            // The flag is Flow-internal; read history anyway if the completion event never arrives.
            setTimeout(readHistory, 1000);
        } else {
            requestAnimationFrame(() => setTimeout(readHistory));
        }
    });
    """.trimIndent(),
)
