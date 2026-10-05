@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.navigation.header

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.ElementEffect
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.ICON_SIZE_LARGE
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.getAttribute
import tech.testsys.web.components.core.set
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.texts.HeaderTexts

/**
 * Notifications whose list and read state are owned by the application. The first snapshot when the header is built
 * is the loaded baseline; later unseen unread keys are arrivals, while removed or read keys never replay within that header.
 *
 * @property items the signal containing the authoritative list.
 * @property onRead the handler called before opening an unread item; the application updates [items].
 * @property onReadAll the handler of the read-all action; the application updates [items].
 * @since %CURRENT_VERSION%
 */
class HeaderNotifications(
    val items: Signal<List<HeaderNotification>>,
    val onRead: (String) -> Unit,
    val onReadAll: () -> Unit,
)

/**
 * Notification displayed in the header.
 *
 * @property key the unique key of the notification.
 * @property label the primary text.
 * @property destination the related route or action.
 * @property isUnread whether the application considers the notification unread.
 * @property description the optional secondary text.
 * @since %CURRENT_VERSION%
 */
data class HeaderNotification(
    val key: String,
    val label: String,
    val destination: HeaderDestination,
    val isUnread: Boolean = true,
    val description: String? = null,
)

internal class HeaderNotificationsController(
    private val notifications: HeaderNotifications,
    private val texts: HeaderTexts,
    interactions: HeaderInteractions,
) {
    val indicator = Span().apply {
        addClassName(CssClass.BellDot)
        element.setAriaHidden(true)
    }
    private val trigger = NativeButton().apply {
        addClassNames(CssClass.Btn, CssClass.BtnGhost, CssClass.BtnIcon, CssClass.Bell)
        add(svgIcon(IconName.Bell, ICON_SIZE_LARGE), indicator)
    }
    val popup = interactions.popup(trigger, label = texts.notifications, theme = CssTheme.HeaderNotificationsPopup, autofocus = true)
    val readAll = NativeButton(texts.readAll).apply {
        addClassNames(CssClass.Btn, CssClass.BtnLink, CssClass.BtnSm, CssClass.HeaderReadAll)
        addClickListener { notifications.onReadAll() }
    }
    private var isAcceptingArrivals = false
    private val seen = notifications.items.peek().map { item -> item.key }.toMutableSet()
    private val list = Div().apply { addClassName(CssClass.HeaderNotificationsList) }
    private val empty = Div(texts.notificationsEmpty).apply { addClassName(CssClass.HeaderNotificationsEmpty) }
    val component = Div(trigger, popup)
    val arrival = HeaderNotificationArrival(component, texts, ::openNotification) { popup.open() }

    init {
        popup.add(Div(Span(texts.notifications), readAll).apply { addClassName(CssClass.HeaderPopupHead) }, list, empty)
        component.addAttachListener {
            seen.addAll(notifications.items.peek().map { item -> item.key })
            isAcceptingArrivals = true
            component.element.attachNotificationArrivals()
        }
        component.addDetachListener {
            isAcceptingArrivals = false
            arrival.close()
        }
        popup.addOpenedChangeListener { event -> if (event.isOpened) arrival.close() }
        render(notifications.items.peek())
        ElementEffect.bind(component.element, notifications.items) { _, values -> render(values) }
    }

    private fun render(values: List<HeaderNotification>) {
        require(values.map { value -> value.key }.distinct().size == values.size) { "Header notification keys must be unique" }
        val added = values.filter { item -> item.key !in seen && item.isUnread }
        seen.addAll(values.map { item -> item.key })
        arrival.refresh(values)
        val unread = values.count { value -> value.isUnread }
        indicator.isVisible = unread > 0
        trigger.element.setAttribute(HtmlAttribute.AriaLabel, texts.unreadCount(unread))
        readAll.isEnabled = unread > 0
        empty.isVisible = values.isEmpty()
        val existing = list.children.toList().associateBy { component -> component.element.getAttribute(HtmlAttribute.DataHeaderKey) }
        val keys = values.map { notification -> notification.key }.toSet()
        val removed = existing.filterKeys { key -> key !in keys }.values
        removed.forEach { row -> list.remove(row) }
        values.forEachIndexed { index, notification ->
            val row = existing[notification.key] ?: item(notification)
            updateItem(row, notification)
            if (index >= list.element.childCount || list.element.getChild(index) != row.element) {
                list.element.insertChild(index, row.element)
            }
        }
        if (added.isNotEmpty() && isAcceptingArrivals && component.isAttached) announce(added)
        if (removed.isNotEmpty()) {
            popup.element.focusAfterNotificationRemoval(trigger.element)
        }
    }

    private fun announce(values: List<HeaderNotification>) {
        component.element.pulseNotificationArrival(indicator.element)
        if (popup.isOpened) {
            values.forEach { notification ->
                val row = list.children.toList().find { item -> item.element.getAttribute(HtmlAttribute.DataHeaderKey) == notification.key }
                row?.let { item -> component.element.highlightNotificationArrival(item.element) }
            }
        } else {
            arrival.show(values)
        }
    }

    private fun openNotification(key: String) {
        val current = notifications.items.peek().find { value -> value.key == key }
        if (current != null) {
            if (current.isUnread) notifications.onRead(current.key)
            popup.close()
            current.destination.open()
        }
    }

    private fun item(notification: HeaderNotification): NativeButton = NativeButton().apply {
        element.setType(ElementType.Button)
        element.setAttribute(HtmlAttribute.DataHeaderKey, notification.key)
        addClickListener { openNotification(notification.key) }
    }

    private fun updateItem(row: Component, notification: HeaderNotification) {
        row.element.setText(notification.label)
        row.element.classList.add(CssClass.HeaderNotification)
        row.element.classList.set(CssClass.HeaderNotificationUnread, notification.isUnread)
        row.element.setAttribute(
            HtmlAttribute.AriaLabel,
            if (notification.isUnread) texts.unreadItem(notification.label) else notification.label,
        )
        notification.description?.let { description -> row.element.appendChild(Span(description).element) }
    }
}

private fun Element.attachNotificationArrivals() = executeJs("window.testsysHeaderArrivals.attach(this)")

private fun Element.focusAfterNotificationRemoval(trigger: Element) = executeJs(
    "requestAnimationFrame(() => { if (this.opened && document.activeElement === document.body) { " +
        "const next = this.querySelector('button:not([disabled]),a[href]'); (next ?? $0).focus(); } });",
    trigger,
)

private fun Element.pulseNotificationArrival(dot: Element) = executeJs("window.testsysHeaderArrivals.pulse(this, $0)", dot)

private fun Element.highlightNotificationArrival(row: Element) = executeJs("window.testsysHeaderArrivals.highlight(this, $0)", row)
