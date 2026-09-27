package tech.testsys.web.ui.overlay

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.ComponentUtil
import com.vaadin.flow.component.contextmenu.ContextMenu
import com.vaadin.flow.component.contextmenu.MenuItem
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.TestSysDsl
import tech.testsys.web.ui.actions.action
import tech.testsys.web.ui.actions.iconAction
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.layout.ContentScope

/**
 * Scope of an action menu: its items in order. Destructive items come last, after a separator the menu adds itself.
 *
 * @since %CURRENT_VERSION%
 */
@TestSysDsl
class MenuScope internal constructor(private val menu: ContextMenu) {
    internal var itemCount: Int = 0
        private set
    private var hasDestructive = false

    /**
     * Adds an item named [label] that runs [onSelect]; an item that is not [isEnabled] is shown but cannot be chosen.
     *
     * @throws IllegalStateException if a destructive item is already added.
     * @since %CURRENT_VERSION%
     */
    fun item(label: String, isEnabled: Boolean = true, onSelect: () -> Unit) {
        check(!hasDestructive) { "Menu item '$label' follows a destructive item; destructive items come last" }
        add(label, onSelect).isEnabled = isEnabled
    }

    /**
     * Adds an item named [label] that deletes or cancels something and runs [onSelect].
     *
     * @since %CURRENT_VERSION%
     */
    fun destructiveItem(label: String, onSelect: () -> Unit) {
        if (!hasDestructive && itemCount > 0) menu.addSeparator()
        hasDestructive = true
        add(label, onSelect).addClassName("ts-menu__item--danger")
    }

    private fun add(label: String, onSelect: () -> Unit): MenuItem {
        itemCount++
        return menu.addItem(label) { onSelect() }.apply { addClassName("ts-menu__item") }
    }
}

/**
 * Adds a button that opens an action menu: "⋯" named for screen readers if [label] is `null`, a text button otherwise.
 *
 * @throws IllegalStateException if the menu has no items or an item follows a destructive one.
 * @since %CURRENT_VERSION%
 */
fun ContentScope.menu(label: String? = null, content: MenuScope.() -> Unit): ElementHandle {
    val trigger = if (label == null) iconAction(IconName.Ellipsis, texts.menu.actions) else action(label)
    attachMenu(trigger.button, content)
    return ElementHandle(trigger.button)
}

/** Attaches a menu filled by [content] to [trigger], opened by a click on it. */
internal fun attachMenu(trigger: Component, content: MenuScope.() -> Unit): ContextMenu {
    val menu = ContextMenu(trigger).apply {
        isOpenOnClick = true
        // A theme, not the class: .ts-menu of the design system lays out the popover and would apply to the host element.
        element.themeList.add("ts-menu")
    }
    val scope = MenuScope(menu).apply(content)
    check(scope.itemCount > 0) { "Menu must have at least one item" }
    trigger.element.setAttribute("aria-haspopup", "menu")
    ComponentUtil.setData(trigger, ContextMenu::class.java, menu)
    return menu
}

/** The menu that [trigger] opens. */
internal fun menuOf(trigger: Component): ContextMenu =
    checkNotNull(ComponentUtil.getData(trigger, ContextMenu::class.java)) { "Component <${trigger.element.tag}> opens no menu" }
