@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.actions

import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.copyToClipboardClient
import tech.testsys.web.components.layout.ContentScope

/**
 * Adds a neutral action named [label] that copies the text returned by [value] at the moment of the click to the clipboard
 * of the browser.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.copyAction(label: String, value: () -> String, configure: ActionHandle.() -> Unit = {}): ActionHandle = action(label) {
    onClick { button.element.copyToClipboardClient(value()) }
    configure()
}
