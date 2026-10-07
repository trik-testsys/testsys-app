@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.core

import com.vaadin.flow.dom.DomEventListener
import com.vaadin.flow.dom.DomListenerRegistration
import com.vaadin.flow.dom.Element
import com.vaadin.flow.dom.PropertyChangeListener
import tools.jackson.databind.JsonNode

internal const val TABLE_SORT_KEY_FILTER: String = "event.key === 'Enter' || event.key === ' '"
internal const val TABLE_ROW_CLICK_FILTER: String =
    "!event.target.closest('[data-ts-input], vaadin-checkbox, vaadin-button, vaadin-text-field, button, a, input, label, " +
        "select, textarea, [role=button], .ts-field__value--inline')"
internal const val LOOKUP_CONTROL_SELECTOR: String = "vaadin-button, .ts-chip__x"
internal const val LOOKUP_CLICK_EXPRESSION: String = "!event.target.closest('$LOOKUP_CONTROL_SELECTOR')"

@InternalComponentsApi
internal enum class DomEvent(internal val value: String) {
    Click("click"),
    HeaderArrivalExpired("header-arrival-expired"),
    HeaderInput("header-input"),
    HeaderKey("header-key"),
    HeaderMenuClose("header-menu-close"),
    HeaderMenuInput("header-menu-input"),
    HeaderQuery("header-query"),
    Input("input"),
    KeyDown("keydown"),
    MouseEnter("mouseenter"),
    RangePick("range-pick"),
    TransferRemove("testsys-transfer-remove"),
}

@InternalComponentsApi
internal enum class DomEventData(internal val value: String) {
    DetailEnd("event.detail.end"),
    DetailIdentity("event.detail.identity"),
    DetailKey("event.detail.key"),
    DetailKeys("event.detail.keys"),
    DetailQuery("event.detail.query"),
    DetailRevision("event.detail.revision"),
    DetailStart("event.detail.start"),
    DetailVersion("event.detail.version"),
}

@InternalComponentsApi
internal enum class DomEventFilter(internal val value: String) {
    CardClick("!event.target.closest('button')"),
    CardKey("event.target === element && (event.key === 'Enter' || event.key === ' ')"),
    TableSortKey(TABLE_SORT_KEY_FILTER),
    TableRowClick(TABLE_ROW_CLICK_FILTER),
    LookupClick(LOOKUP_CLICK_EXPRESSION),
}

@InternalComponentsApi
internal fun Element.addEventListener(event: DomEvent, listener: DomEventListener): DomListenerRegistration =
    addEventListener(event.value, listener)

@InternalComponentsApi
internal fun DomListenerRegistration.addEventData(data: DomEventData): DomListenerRegistration = addEventData(data.value)

@InternalComponentsApi
internal fun DomListenerRegistration.setFilter(filter: DomEventFilter): DomListenerRegistration = setFilter(filter.value)

@InternalComponentsApi
internal fun Element.addPropertyChangeListener(
    property: DomProperty,
    event: DomEvent,
    listener: PropertyChangeListener,
): DomListenerRegistration = addPropertyChangeListener(property.value, event.value, listener)

@InternalComponentsApi
internal fun JsonNode.get(data: DomEventData): JsonNode = get(data.value)
