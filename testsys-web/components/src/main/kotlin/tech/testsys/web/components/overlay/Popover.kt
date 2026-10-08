@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.overlay

import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.popover.PopoverPosition
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.actions.ActionRole
import tech.testsys.web.components.actions.buildActionButton
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addThemeName
import tech.testsys.web.components.core.setAriaExpanded
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.layout.Placement

/**
 * Alignment of a popup relative to its trigger.
 *
 * @since %CURRENT_VERSION%
 */
enum class PopoverAlignment {
    /**
     * Aligns the left edges.
     *
     * @since %CURRENT_VERSION%
     */
    Start,

    /**
     * Aligns the right edges.
     *
     * @since %CURRENT_VERSION%
     */
    End,
}

/**
 * Handle of a popup and its own trigger; Vaadin restores focus to the trigger if it stayed in the popup.
 *
 * @property isOpen whether the popup is open.
 * @property isEnabled whether its trigger accepts interaction.
 * @since %CURRENT_VERSION%
 */
class PopoverHandle internal constructor(root: Div, private val popup: Popover, private val trigger: Button) : ElementHandle(root) {
    private var closed: () -> Unit = {}
    val isOpen: Boolean
        get() = popup.isOpened
    var isEnabled: Boolean
        get() = trigger.isEnabled
        set(value) {
            trigger.isEnabled = value
            if (!value) close()
        }

    init {
        popup.addOpenedChangeListener { event ->
            trigger.element.setAriaExpanded(event.isOpened)
            if (!event.isOpened) {
                closed()
            }
        }
        root.addDetachListener { popup.close() }
    }

    /**
     * Opens the popup if its trigger is enabled and visible.
     *
     * @since %CURRENT_VERSION%
     */
    fun open() {
        if (isEnabled && isVisible) popup.open()
    }

    /**
     * Closes the popup.
     *
     * @since %CURRENT_VERSION%
     */
    fun close() {
        popup.close()
    }

    /**
     * Replaces the closing [listener].
     *
     * @since %CURRENT_VERSION%
     */
    fun onClose(listener: () -> Unit) {
        closed = listener
    }
}

/**
 * Adds a popup with its own [label] trigger and [content], aligned by [alignment].
 *
 * @param configure the returned popup configuration.
 * @param content the popup body composition.
 * @since %CURRENT_VERSION%
 */
fun ContentScope.popover(
    label: String,
    alignment: PopoverAlignment = PopoverAlignment.Start,
    configure: PopoverHandle.() -> Unit = {},
    content: ContentScope.() -> Unit,
): PopoverHandle {
    val root = Div()
    val trigger = buildActionButton(ActionRole.Neutral, label).apply {
        element.setAriaHasPopup(AriaPopup.Dialog)
    }

    val popup = Popover().apply {
        target = trigger
        isModal = false
        isAutofocus = true
        isCloseOnEsc = true
        isCloseOnOutsideClick = true
        setAriaLabel(label)
        position = if (alignment == PopoverAlignment.Start) PopoverPosition.BOTTOM_START else PopoverPosition.BOTTOM_END
        addThemeName(CssTheme.Popover)
    }

    val body = Div().apply { addClassName(CssClass.Pop) }
    ContentScope(body, texts, Placement.Body).content()
    popup.add(body)
    root.add(trigger, popup)
    add(root)
    return PopoverHandle(root, popup, trigger).apply(configure)
}

/**
 * Adds a popup on [size] columns, or the remaining columns, using [label] for its trigger and [content] for its body.
 *
 * @param configure the returned popup configuration.
 * @param content the popup body composition.
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.popover(
    label: String,
    size: Int? = null,
    alignment: PopoverAlignment = PopoverAlignment.Start,
    configure: PopoverHandle.() -> Unit = {},
    content: ContentScope.() -> Unit,
): PopoverHandle = placeContent(size, Div()).popover(label, alignment, configure, content)
