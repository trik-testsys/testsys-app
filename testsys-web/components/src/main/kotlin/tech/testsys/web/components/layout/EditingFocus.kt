@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.layout

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setEditingBody

/** Finds eligible inputs in the current DOM after the edit state has reached the client. */
internal fun focusFirstEditableInput(body: Component, fallback: Component) {
    body.element.setEditingBody(true)
    // The body may be detached or hidden; queue on the visible cancel action so fallback never waits for it.
    fallback.element.focusEditableInputInBlock()
}

private fun Element.focusEditableInputInBlock(): PendingJavaScriptResult = executeJs("window.testsysEditingFocus.focusFirst(this)")
