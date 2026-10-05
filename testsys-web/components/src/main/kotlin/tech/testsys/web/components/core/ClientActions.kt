@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.dom.Element

@InternalComponentsApi
internal fun Element.focusClient(): PendingJavaScriptResult = executeJs("this.focus()")

@InternalComponentsApi
internal fun Element.clickClient(): PendingJavaScriptResult = executeJs("this.click()")
