@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.router.RouteParameters
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.TestSysDsl
import tech.testsys.web.components.actions.action
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.layout.ContentScope

/** Item collected by a [MenuScope]: an [Action] or a [Link]. */
internal sealed interface MenuEntry {
    val label: String
    val isEnabled: Boolean
    val isDestructive: Boolean

    /** Item that runs [onSelect]. */
    class Action(
        override val label: String,
        override val isEnabled: Boolean,
        override val isDestructive: Boolean,
        val onSelect: () -> Unit,
    ) : MenuEntry

    /** Item that opens the route [target] with [parameters]. */
    class Link(override val label: String, val target: Class<out Component>, val parameters: RouteParameters) : MenuEntry {
        override val isEnabled: Boolean get() = true
        override val isDestructive: Boolean get() = false
    }
}

/**
 * Scope of an action menu or a header user menu: its items in order. Destructive items come last; the menu separates
 * them from ordinary items itself.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class MenuScope internal constructor() {
    private val collected = mutableListOf<MenuEntry>()

    /** The items added so far, in order. */
    internal val entries: List<MenuEntry> get() = collected

    /**
     * Adds an item named [label] that runs [onSelect]; an item that is not [isEnabled] is shown but cannot be chosen.
     *
     * @throws IllegalStateException if a destructive item is already added.
     * @since %CURRENT_VERSION%
     */
    fun item(label: String, isEnabled: Boolean = true, onSelect: () -> Unit) {
        checkOrdinary(label)
        collected += MenuEntry.Action(label, isEnabled, isDestructive = false, onSelect)
    }

    /**
     * Adds an item named [label] that opens the route [target] with [parameters].
     *
     * @throws IllegalStateException if a destructive item is already added.
     * @since %CURRENT_VERSION%
     */
    fun link(label: String, target: Class<out Component>, parameters: RouteParameters = RouteParameters.empty()) {
        checkOrdinary(label)
        collected += MenuEntry.Link(label, target, parameters)
    }

    /**
     * Adds an item named [label] that deletes or cancels something and runs [onSelect]; an item that is not
     * [isEnabled] is shown but cannot be chosen.
     *
     * @since %CURRENT_VERSION%
     */
    fun destructiveItem(label: String, isEnabled: Boolean = true, onSelect: () -> Unit) {
        collected += MenuEntry.Action(label, isEnabled, isDestructive = true, onSelect)
    }

    private fun checkOrdinary(label: String) {
        check(collected.none { entry -> entry.isDestructive }) {
            "Menu item '$label' follows a destructive item; destructive items come last"
        }
    }
}

/** Collects the items of [content], which must add at least one. */
internal fun menuEntries(content: MenuScope.() -> Unit): List<MenuEntry> {
    val entries = MenuScope().apply(content).entries
    check(entries.isNotEmpty()) { "Menu must have at least one item" }
    return entries
}

/** Whether a separator precedes the item at [index]: only the first destructive item after ordinary ones has it. */
internal fun List<MenuEntry>.hasSeparatorBefore(index: Int): Boolean =
    index > 0 && this[index].isDestructive && !this[index - 1].isDestructive

/**
 * Adds a "⋯" button named for screen readers that opens an action menu filled by [content].
 *
 * @throws IllegalStateException if the menu has no items or an item follows a destructive one.
 * @since %CURRENT_VERSION%
 */
fun ContentScope.menu(content: MenuScope.() -> Unit): ElementHandle = iconMenu(texts.messages.uiOverlay.menuActions(), content)

/** Adds the compact ellipsis trigger with the complete accessible name [ariaLabel]. */
internal fun ContentScope.iconMenu(ariaLabel: String, content: MenuScope.() -> Unit): ElementHandle {
    val trigger = action(ariaLabel, IconName.Ellipsis, iconOnly = true)
    attachMenu(trigger.button, content)
    return ElementHandle(trigger.button)
}

/** Attaches a menu filled by [content] to [trigger], opened by a click on it. */
internal fun attachMenu(trigger: Component, content: MenuScope.() -> Unit): ContextMenu {
    val menu = ContextMenu(trigger).apply {
        isOpenOnClick = true
        // A theme, not the class: .ts-menu of the design system lays out the popover and would apply to the host element.
        element.themeList.add(CssTheme.Menu)
    }

    val entries = menuEntries(content)
    entries.forEachIndexed { index, entry ->
        if (entries.hasSeparatorBefore(index)) menu.addSeparator()
        menu.addItem(entry.label) {
            when (entry) {
                is MenuEntry.Action -> entry.onSelect()
                is MenuEntry.Link -> UI.getCurrent().navigate(entry.target, entry.parameters)
            }
        }.apply {
            isEnabled = entry.isEnabled
            addClassName(CssClass.MenuItem)
            if (entry.isDestructive) addClassName(CssClass.MenuItemDanger)
        }
    }

    trigger.element.setAriaHasPopup(AriaPopup.Menu)
    ComponentUtil.setData(trigger, ContextMenu::class.java, menu)
    return menu
}

/** The menu that [trigger] opens. */
internal fun menuOf(trigger: Component): ContextMenu =
    checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java)) { "Component <${trigger.element.tag}> opens no menu" }
