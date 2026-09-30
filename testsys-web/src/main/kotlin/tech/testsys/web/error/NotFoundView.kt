package tech.testsys.web.error

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.H1
import com.vaadin.flow.component.html.Header
import com.vaadin.flow.component.html.Main
import com.vaadin.flow.component.html.Section
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.ErrorParameter
import com.vaadin.flow.router.HasDynamicTitle
import com.vaadin.flow.router.HasErrorParameter
import com.vaadin.flow.router.NotFoundException
import com.vaadin.flow.server.HttpStatusCode
import tech.testsys.web.ui.UiTexts

/**
 * Branded missing-route screen available in every profile, with browser-history navigation only.
 *
 * @since %CURRENT_VERSION%
 */
class NotFoundView(private val texts: UiTexts) : Div(), HasErrorParameter<NotFoundException>, HasDynamicTitle {
    private val back = Button(texts.notFound.back).apply {
        element.setAttribute("data-ts-role", "neutral")
        element.setAttribute("data-ts-size", "md")
        isEnabled = false
        addClickListener { ui.orElseThrow().page.history.back() }
    }

    init {
        addClassName("ts-app")
        val brand = Span(texts.brand).apply { addClassName("ts-brand") }
        val bar = Div(brand).apply { addClassName("ts-header__bar") }
        val header = Header(bar).apply { addClassName("ts-header") }
        val heading = H1(texts.notFound.title).apply { addClassName("ts-h1") }
        val description = Div().apply {
            text = texts.notFound.description
            addClassName("ts-empty__desc")
        }
        val actions = Div(back).apply { addClassName("ts-empty__actions") }
        val content = Div(heading, description, actions).apply { addClassName("ts-empty") }
        val block = Section(content).apply { addClassName("ts-block") }
        add(header, Main(block).apply { addClassName("ts-page") })
    }

    override fun setErrorParameter(event: BeforeEnterEvent, parameter: ErrorParameter<NotFoundException>): Int {
        back.isEnabled = false
        // Flow updates history asynchronously; use its navigation completion event before reading the new entry.
        element.executeJs(
            """
            return new Promise(resolve => {
                const readHistory = () => resolve(window.navigation ? window.navigation.canGoBack : window.history.length > 1);
                if (window.Vaadin?.Flow?.navigation) {
                    window.addEventListener('vaadin-navigated', readHistory, {once: true});
                } else {
                    requestAnimationFrame(() => setTimeout(readHistory));
                }
            });
            """.trimIndent(),
        ).then(Boolean::class.java) { canGoBack -> back.isEnabled = canGoBack }
        return HttpStatusCode.NOT_FOUND.code
    }

    override fun getPageTitle(): String = texts.notFound.pageTitle
}
