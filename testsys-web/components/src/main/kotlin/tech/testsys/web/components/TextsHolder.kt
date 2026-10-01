package tech.testsys.web.components

import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI

/** Makes [texts] the texts of the components opened on [ui] outside a page scope, such as dialogs. */
internal fun bindTexts(ui: UI, texts: UiTexts) {
    ComponentUtil.setData(ui, UiTexts::class.java, texts)
}

/** Texts bound to the current UI by the page built on it. */
internal fun currentTexts(): UiTexts {
    val ui = checkNotNull(UI.getCurrent()) { "No current UI: dialogs open from event handlers of a page" }
    return checkNotNull(ComponentUtil.getData(ui, UiTexts::class.java)) {
        "No texts bound to the current UI: build a TestSysView page before opening dialogs"
    }
}
