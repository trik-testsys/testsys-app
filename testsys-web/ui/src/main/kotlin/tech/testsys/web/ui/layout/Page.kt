package tech.testsys.web.ui.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Main
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.bindTexts
import tech.testsys.web.ui.navigation.CabinetHeader
import tech.testsys.web.ui.navigation.buildHeader

/**
 * Replaces the content of [root] with `.ts-app`: the header, the page head if the body declares one and
 * `main.ts-page` built by [body]; [view] is the class of the page, whose tab the page head marks. Binds [texts]
 * to the current UI for dialogs opened later from its handlers.
 */
internal fun renderPage(root: Div, header: CabinetHeader, texts: UiTexts, view: Class<out Component>?, body: PageScope.() -> Unit) {
    bindTexts(checkNotNull(UI.getCurrent()) { "No current UI: a page is built inside a Vaadin request" }, texts)
    val main = Main().apply { addClassName("ts-page") }
    root.removeAll()
    root.setClassName("ts-app")
    root.add(buildHeader(header, texts), main)
    PageScope(main, texts, view) { head -> root.addComponentAtIndex(1, head) }.body()
}
