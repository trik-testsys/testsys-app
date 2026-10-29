@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Main
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.navigation.header.CabinetHeader
import tech.testsys.web.components.navigation.header.buildHeader
import tech.testsys.web.components.overlay.GuideClient
import tech.testsys.web.components.texts.currentUiLocalization

/**
 * Replaces the content of [root] with `.ts-app`: the header, the page head if the body declares one and
 * `main.ts-page` built by [body], and the footer; [view] is the class of the page, whose tab the page head marks. Texts
 * come from the localization bound to the current UI.
 */
internal fun renderPage(root: Div, header: CabinetHeader, view: Class<out Component>?, body: PageScope.() -> Unit) {
    val texts = currentUiLocalization()

    val main = Main().apply { addClassName(CssClass.Page) }
    root.removeAll()
    root.setClassName(CssClass.App)
    root.add(buildHeader(header, texts), main)

    val scope = PageScope(main, texts, view) { head -> root.addComponentAtIndex(1, head) }.apply(body)
    scope.finishGuide()
    root.add(scope.buildFooter().apply { add(GuideClient(texts.messages.uiPage)) })
}
