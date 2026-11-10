@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.actions

import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.copyToClipboardClient
import tech.testsys.web.components.layout.ContentScope

/**
 * Adds a neutral action named [label] that copies the text returned by [value] at the moment of the click to the clipboard
 * of the browser. [configure] may set its own click listener; copying still runs.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.copyAction(label: String, value: () -> String, configure: ActionHandle.() -> Unit = {}): ActionHandle = action(label) {
    button.addClickListener { _ -> if (!isLoading) button.element.copyToClipboardClient(value()) }
    configure()
}
