package tech.testsys.web.components.data

import com.vaadin.flow.component.html.Div
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.signals.Signal
import tech.testsys.web.components.ElementHandle
import tech.testsys.web.components.core.Bindable
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

/**
 * Handle of a keyed list reordered by pointer or keyboard; programmatic updates never invoke its change callback.
 *
 * @param T the type of the application items.
 * @property items the immutable current item order.
 * @property isEnabled whether reordering is enabled.
 * @since %CURRENT_VERSION%
 */
class SortableListHandle<T : Any> internal constructor(private val adapter: SortableListAdapter<T>, initial: List<T>) : ElementHandle(
    adapter,
) {
    private val order = Bindable(adapter.element, initial.toList(), adapter::present)
    private val enabled = Bindable(adapter.element, true, adapter::enable)
    init {
        adapter.isOrderBound = {
            order.isBound
        }
    }

    var items: List<T>
        get() = adapter.items.toList()
        set(value) {
            order.value = value.toList()
        }
    var isEnabled: Boolean
        get() = enabled.value
        set(value) {
            enabled.value = value
        }

    /**
     * Binds the item order to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindItems(signal: Signal<List<T>>): SignalBinding<List<T>> = order.bind(signal)

    /**
     * Binds the enabled state to [signal].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEnabled(signal: Signal<Boolean>): SignalBinding<Boolean> = enabled.bind(signal)

    /**
     * Replaces the callback of a valid user reorder; [listener] receives the resulting order.
     *
     * @since %CURRENT_VERSION%
     */
    fun onChange(listener: (List<T>) -> Unit) {
        adapter.changed = listener
    }
}

/**
 * Adds [items] identified by unique [itemKey] and labelled by [itemLabel]; [content] renders their persistent Flow content.
 *
 * @param T the type of application items.
 * @param content the renderer whose components survive reordering.
 * @param configure the interactions and signal bindings of the returned handle.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> ContentScope.sortableList(
    items: List<T>,
    itemKey: (T) -> String,
    itemLabel: (T) -> String,
    content: ContentScope.(T) -> Unit,
    configure: SortableListHandle<T>.() -> Unit = {},
): SortableListHandle<T> {
    val adapter = SortableListAdapter(texts, items, itemKey, itemLabel, content, gridColumns)
    add(adapter)
    return SortableListHandle(adapter, items).apply(configure)
}

/**
 * Adds a sortable list on [size] columns, or the remaining columns.
 *
 * @param T the type of application items.
 * @param content the renderer of the persistent item content.
 * @param configure the returned handle configuration.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.sortableList(
    items: List<T>,
    itemKey: (T) -> String,
    itemLabel: (T) -> String,
    size: Int? = null,
    content: ContentScope.(T) -> Unit,
    configure: SortableListHandle<T>.() -> Unit = {},
): SortableListHandle<T> = placeContent(size, Div()).sortableList(
    items,
    itemKey,
    itemLabel,
    content,
    configure,
)
