@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.checkbox.Switch
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.layout.BlockRowScope

/**
 * Adds a boolean switch on [size] columns beside [labelSize] label columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.switchInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    configure: ValueInput<Boolean>.() -> Unit = {},
): ValueInput<Boolean> {
    val field = Switch()
    return addInput(label, labelSize, size, field, hint, configure)
}
