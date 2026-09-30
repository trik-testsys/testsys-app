package tech.testsys.web.ui.data

import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.react.ReactAdapterComponent
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.layout.ContentScope
import tech.testsys.web.ui.layout.Placement
import java.util.Base64

@Tag("testsys-sortable-list")
@JsModule("./testsys-ui/sortable-list.tsx")
internal class SortableListAdapter<T : Any>(
    private val texts: UiTexts,
    initial: List<T>,
    private val itemKey: (T) -> String,
    private val itemLabel: (T) -> String,
    private val content: ContentScope.(T) -> Unit,
) : ReactAdapterComponent() {
    var items: List<T> = emptyList()
        private set
    var isOrderBound: () -> Boolean = { false }
    var changed: (List<T>) -> Unit = {}
    private var version = 0L
    private var isEnabled = true
    private val known = mutableMapOf<String, Pair<T, Div>>()

    init {
        element.addEventListener("list-reorder") { event ->
            val keys = readFromJson(event.eventData.get("event.detail.keys"), Array<String>::class.java).toList()
            reorder(keys, event.eventData.get("event.detail.version").asLong())
        }.addEventData("event.detail.keys").addEventData("event.detail.version")
        addDetachListener {
            version++
            publish()
        }
        addAttachListener {
            version++
            publish()
        }
        present(initial)
    }

    fun present(values: List<T>) {
        val keys = values.map(itemKey)
        require(keys.all(String::isNotEmpty) && keys.distinct().size == keys.size) { "Sortable list keys must be nonempty and unique" }
        val removed = known.keys - keys.toSet()
        removed.forEach { key ->
            getContentElement(slot(key)).removeAllChildren()
            known.remove(key)
        }
        values.forEach { item ->
            val key = itemKey(item)
            if (known[key]?.first !== item) {
                val holder = Div()
                ContentScope(holder, texts, Placement.Body).content(item)
                getContentElement(slot(key)).removeAllChildren()
                getContentElement(slot(key)).appendChild(holder.element)
                known[key] = item to holder
            }
        }
        items = values.toList()
        version++
        publish()
    }

    fun enable(value: Boolean) {
        isEnabled = value
        version++
        publish()
    }

    fun reorder(keys: List<String>, receivedVersion: Long): Boolean {
        val currentKeys = items.map(itemKey)
        if (!isEnabled || receivedVersion != version || keys.size != currentKeys.size || keys.toSet() != currentKeys.toSet()) {
            publish()
            return false
        }
        if (keys == currentKeys) return true
        val byKey = items.associateBy(itemKey)
        val requested = keys.map(byKey::getValue)
        if (!isOrderBound()) items = requested
        version++
        publish()
        changed(requested.toList())
        return true
    }

    fun currentVersion(): Long = version

    private fun slot(key: String): String = "item-" + Base64.getUrlEncoder().withoutPadding().encodeToString(
        key.toByteArray(
            Charsets.UTF_8,
        ),
    )

    private fun publish() {
        setState(
            "list",
            mapOf(
                "positions" to items.associate { item ->
                    itemKey(item) to items.indices.map { index ->
                        texts.components.reorderPosition(
                            itemLabel(
                                item,
                            ),
                            index + 1,
                            items.size,
                        )
                    }
                },
                "version" to version,
                "disabled" to !isEnabled,
                "handleLabel" to texts.components.drag,
                "items" to items.map { item ->
                    mapOf(
                        "key" to itemKey(item),
                        "slot" to slot(
                            itemKey(
                                item,
                            ),
                        ),
                        "label" to itemLabel(item),
                        "announcement" to texts.components.reorderPosition(
                            itemLabel(item),
                            items.indexOf(item) + 1,
                            items.size,
                        ),
                    )
                },
            ),
        )
    }
}
