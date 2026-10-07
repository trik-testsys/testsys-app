@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Main
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.navigation.header.buildHeader
import tech.testsys.web.components.texts.UiTexts
import tech.testsys.web.components.texts.bindTexts

/**
 * Replaces the content of [root] with `.ts-app`: the header, the page head if the body declares one and
 * `main.ts-page` built by [body], and the footer; [view] is the class of the page, whose tab the page head marks. Binds [texts]
 * to the current UI for dialogs opened later from its handlers.
 */
internal fun renderPage(root: Div, header: CabinetHeader, texts: UiTexts, view: Class<out Component>?, body: PageScope.() -> Unit) {
    bindTexts(checkNotNull(UI.getCurrent()) { "No current UI: a page is built inside a Vaadin request" }, texts)

    val main = Main().apply { addClassName(CssClass.Page) }
    root.removeAll()
    root.setClassName(CssClass.App)
    root.add(buildHeader(header, texts), main)

    val scope = PageScope(main, texts, view) { head -> root.addComponentAtIndex(1, head) }.apply(body)
    root.add(scope.buildFooter())
}
