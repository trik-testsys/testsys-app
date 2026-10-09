@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.dom.Element

@InternalComponentsApi
internal fun Element.focusClient(): PendingJavaScriptResult = executeJs("this.focus()")

@InternalComponentsApi
internal fun Element.clickClient(): PendingJavaScriptResult = executeJs("this.click()")

@InternalComponentsApi
internal fun Element.copyToClipboardClient(text: String): PendingJavaScriptResult =
    executeJs("return navigator.clipboard.writeText($0)", text)
