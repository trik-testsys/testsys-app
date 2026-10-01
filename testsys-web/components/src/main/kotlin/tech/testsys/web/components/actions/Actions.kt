package tech.testsys.web.components.actions

import com.vaadin.flow.component.button.Button
import tech.testsys.web.components.core.ICON_SIZE
import tech.testsys.web.components.core.ICON_SIZE_SMALL
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.ContentScope

/**
 * Adds the main action of a block; by convention a block has one.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.mainAction(label: String, icon: IconName? = null, configure: ActionHandle.() -> Unit = {}): ActionHandle =
    addAction(ActionRole.Main, label, icon, configure)

/**
 * Adds a neutral action.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.action(label: String, icon: IconName? = null, configure: ActionHandle.() -> Unit = {}): ActionHandle =
    addAction(ActionRole.Neutral, label, icon, configure)

/**
 * Adds an action that deletes or cancels something.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.destructiveAction(label: String, icon: IconName? = null, configure: ActionHandle.() -> Unit = {}): ActionHandle =
    addAction(ActionRole.Destructive, label, icon, configure)

/**
 * Adds an inline text action.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.linkAction(label: String, icon: IconName? = null, configure: ActionHandle.() -> Unit = {}): ActionHandle =
    addAction(ActionRole.Link, label, icon, configure)

/**
 * Adds a neutral action shown as an icon only; [label] is read by screen readers and shown as a tooltip.
 *
 * @since %CURRENT_VERSION%
 */
fun ContentScope.iconAction(icon: IconName, label: String, configure: ActionHandle.() -> Unit = {}): ActionHandle {
    val handle = addAction(ActionRole.Neutral, label = null, icon, configure = {})
    val button = handle.button
    button.setAriaLabel(label)
    button.setTooltipText(label)
    button.element.setAttribute("data-ts-icon-only", true)
    return handle.apply(configure)
}

/** Role of an action; the look of the button follows it. */
internal enum class ActionRole(val attribute: String) {
    Main("main"),
    Neutral("neutral"),
    Destructive("destructive"),
    Link("link"),
    Danger("danger"),
}

/** Adds the filled red action of a dangerous confirmation; pages have no such role. */
internal fun ContentScope.dangerAction(label: String, configure: ActionHandle.() -> Unit = {}): ActionHandle =
    addAction(ActionRole.Danger, label, icon = null, configure)

private fun ContentScope.addAction(role: ActionRole, label: String?, icon: IconName?, configure: ActionHandle.() -> Unit): ActionHandle {
    val isSmall = placement.isCompact
    val iconComponent = icon?.let { name -> svgIcon(name, if (isSmall) ICON_SIZE_SMALL else ICON_SIZE) }
    val button = Button(label.orEmpty()).apply {
        element.setAttribute("data-ts-role", role.attribute)
        element.setAttribute("data-ts-size", if (isSmall) "sm" else "md")
        setIcon(iconComponent)
    }
    add(button)
    return ActionHandle(button, iconComponent).apply(configure)
}
