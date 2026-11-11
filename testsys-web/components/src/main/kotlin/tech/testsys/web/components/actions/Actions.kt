@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.actions

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.ElementSize
import tech.testsys.web.components.core.ICON_SIZE
import tech.testsys.web.components.core.ICON_SIZE_SMALL
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.setIconOnly
import tech.testsys.web.components.core.setSize
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.GuestCardScope

/** Role of an action; the look of the button follows it. */
internal enum class ActionRole(val attribute: String) {
    Main("main"),
    Neutral("neutral"),
    Destructive("destructive"),
    Link("link"),
    Danger("danger"),
}

/**
 * Adds the main action of a block; by convention a block has one. With [iconOnly] the button shows only [icon], and
 * [label] becomes its accessible name and tooltip.
 *
 * @throws IllegalArgumentException if [iconOnly] is set without [icon] or with a blank [label].
 * @since 1.0.0-beta.1
 */
fun ContentScope.mainAction(
    label: String,
    icon: IconName? = null,
    iconOnly: Boolean = false,
    configure: ActionHandle.() -> Unit = {},
): ActionHandle = addAction(ActionRole.Main, label, icon, iconOnly, configure)

/**
 * Adds the main action of a guest card, as wide as the card, under its fields.
 *
 * @since 1.0.0-beta.1
 */
fun GuestCardScope.mainAction(label: String, configure: ActionHandle.() -> Unit = {}): ActionHandle =
    body.mainAction(label, configure = configure)

/**
 * Adds a neutral action. With [iconOnly] the button shows only [icon], and [label] becomes its accessible name and
 * tooltip.
 *
 * @throws IllegalArgumentException if [iconOnly] is set without [icon] or with a blank [label].
 * @since 1.0.0-beta.1
 */
fun ContentScope.action(
    label: String,
    icon: IconName? = null,
    iconOnly: Boolean = false,
    configure: ActionHandle.() -> Unit = {},
): ActionHandle = addAction(ActionRole.Neutral, label, icon, iconOnly, configure)

/**
 * Adds an action that deletes or cancels something. With [iconOnly] the button shows only [icon], and [label] becomes
 * its accessible name and tooltip.
 *
 * @throws IllegalArgumentException if [iconOnly] is set without [icon] or with a blank [label].
 * @since 1.0.0-beta.1
 */
fun ContentScope.destructiveAction(
    label: String,
    icon: IconName? = null,
    iconOnly: Boolean = false,
    configure: ActionHandle.() -> Unit = {},
): ActionHandle = addAction(ActionRole.Destructive, label, icon, iconOnly, configure)

/**
 * Adds an inline text action. With [iconOnly] the button shows only [icon], and [label] becomes its accessible name
 * and tooltip.
 *
 * @throws IllegalArgumentException if [iconOnly] is set without [icon] or with a blank [label].
 * @since 1.0.0-beta.1
 */
fun ContentScope.linkAction(
    label: String,
    icon: IconName? = null,
    iconOnly: Boolean = false,
    configure: ActionHandle.() -> Unit = {},
): ActionHandle = addAction(ActionRole.Link, label, icon, iconOnly, configure)

/** Adds the filled red action of a dangerous confirmation; pages have no such role. */
internal fun ContentScope.dangerAction(label: String, configure: ActionHandle.() -> Unit = {}) =
    addAction(ActionRole.Danger, label, icon = null, iconOnly = false, configure)

private fun ContentScope.addAction(
    role: ActionRole,
    label: String,
    icon: IconName?,
    iconOnly: Boolean,
    configure: ActionHandle.() -> Unit,
): ActionHandle {
    if (iconOnly) {
        requireNotNull(icon) { "Icon-only action '$label' must have an icon" }
        require(label.isNotBlank()) { "Icon-only action accessible name must not be blank" }
    }

    val button = buildActionButton(role, label.takeUnless { iconOnly }, icon)

    if (iconOnly) {
        button.setAriaLabel(label)
        button.setTooltipText(label)
        button.element.setIconOnly(true)
    }

    add(button)

    return ActionHandle(button, button.icon).apply(configure)
}

/** Builds a button whose semantic role and size follow this scope's placement. */
internal fun ContentScope.buildActionButton(role: ActionRole, label: String?, icon: IconName? = null): Button {
    val isSmall = placement.isCompact
    val iconComponent = icon?.let { name -> svgIcon(name, if (isSmall) ICON_SIZE_SMALL else ICON_SIZE) }
    val button = Button(label.orEmpty()).apply {
        element.setActionRole(role)
        element.setSize(if (isSmall) ElementSize.Small else ElementSize.Medium)
        setIcon(iconComponent)
    }

    return button
}

@InternalComponentsApi
internal fun Element.setActionRole(value: ActionRole): Element = setAttribute("data-ts-role", value.attribute)
