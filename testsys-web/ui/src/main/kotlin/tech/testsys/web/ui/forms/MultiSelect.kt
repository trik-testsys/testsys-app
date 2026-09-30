package tech.testsys.web.ui.forms

import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.value.ValueChangeMode
import tech.testsys.web.ui.UiTexts
import tech.testsys.web.ui.core.ICON_SIZE_TINY
import tech.testsys.web.ui.core.IconName
import tech.testsys.web.ui.core.svgIcon
import tech.testsys.web.ui.layout.BlockRowScope
import tech.testsys.web.ui.layout.ContentScope

/**
 * Adds a local multi-selection of [items] shown by [itemLabel]; popup changes become the value only on Apply.
 * The control takes [size] columns beside [labelSize] label columns.
 *
 * @param T the type of the selectable items.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.multiSelect(
    label: String,
    labelSize: Int,
    size: Int,
    items: List<T>,
    itemLabel: (T) -> String,
    hint: String? = null,
    itemMeta: ((T) -> String)? = null,
    display: MultiSelectDisplay = MultiSelectDisplay.Chips,
    maxChips: Int = 3,
    configure: ValueInput<Set<T>>.() -> Unit = {},
): ValueInput<Set<T>> {
    val field = MultiSelectField(texts, label, items, itemLabel, itemMeta, display, maxChips)
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<Set<T>>> ->
        field.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return placeInput(label, labelSize, size, field, hint, subscribe, configure)
}

internal class MultiSelectField<T : Any>(
    private val texts: UiTexts,
    label: String,
    private val items: List<T>,
    private val itemLabel: (T) -> String,
    private val itemMeta: ((T) -> String)? = null,
    private val display: MultiSelectDisplay = MultiSelectDisplay.Chips,
    private val maxChips: Int = 3,
) : CustomField<Set<T>>(emptySet(), true), HasValidator<Set<T>> {
    private val trigger: NativeButton = NativeButton(texts.lookup.open).apply {
        element.setAttribute("type", "button")
        element.setAttribute("aria-label", label)
        element.setAttribute("aria-haspopup", "dialog")
        addClassName("ts-lookup__text")
    }

    private val chips = Div().apply { addClassName("ts-lookup__chips") }
    private val clear = NativeButton(texts.lookup.clear).apply {
        element.setAttribute("type", "button")
        addClassName("ts-multiselect__clear")
    }

    private val box = Div(trigger, chips, clear).apply { addClassNames("ts-trigger", "ts-lookup", "ts-lookup--many") }
    private val popup = Popover().apply {
        target = trigger
        isOpenOnClick = false
        isModal = true
        isAutofocus = true
        isCloseOnEsc = true
        isCloseOnOutsideClick = true
        setAriaLabel(label)
        addThemeName("ts-popover")
    }

    private val search = TextField().apply {
        placeholder = texts.lookup.search
        setAriaLabel(texts.lookup.search)
        valueChangeMode = ValueChangeMode.EAGER
        addClassName("ts-popover__search")
    }

    private val options = Div().apply { addClassName("ts-options") }
    private val count = Span()
    private var draft: Set<T>? = null

    init {
        clear.addClickListener { if (choosable()) choose(emptySet()) }
        trigger.addClickListener { if (popup.isOpened) close() else open() }
        search.addValueChangeListener { renderDraft() }
        require(maxChips > 0) { "MultiSelect max chips must be positive, got $maxChips" }
        require(items.distinct().size == items.size) { "MultiSelect '$label' items must be unique" }
        val foot = Div(count).apply { addClassName("ts-popover__foot") }
        foot.add(
            NativeButton(texts.lookup.reset).apply {
                element.setAttribute("type", "button")
                addClassNames("ts-btn", "ts-btn--secondary")
                addClickListener {
                    draft = emptySet()
                    renderDraft()
                }
            },
            NativeButton(texts.lookup.apply).apply {
                addClassNames("ts-btn", "ts-btn--primary")
                addClickListener { applyDraft() }
            },
        )
        popup.add(search, options, foot)
        popup.addOpenedChangeListener { event ->
            trigger.element.setAttribute("aria-expanded", event.isOpened.toString())
            box.setClassName("ts-trigger--open", event.isOpened)
            if (!event.isOpened) {
                draft = null
                if (isAttached) trigger.focus()
            }
        }
        add(box, popup)
        addDetachListener { close() }
        renderValue(emptySet())
    }

    fun open() {
        if (!choosable() || popup.isOpened) return
        draft = value.toSet()
        search.clear()
        renderDraft()
        popup.open()
        search.focus()
    }

    fun close() {
        draft = null
        popup.close()
    }

    fun toggle(item: T) {
        val current = draft ?: return
        if (!choosable()) return
        require(item in items) { "MultiSelect draft item '$item' is outside its items" }
        draft = if (item in current) current - item else current + item
        renderDraft(item)
    }

    fun applyDraft() {
        val current = draft ?: return
        if (!choosable()) return
        choose(current)
    }

    override fun setValue(value: Set<T>) {
        require(items.containsAll(value)) { "MultiSelect value contains items outside its options" }
        close()
        super.setValue(value.toSet())
    }

    override fun valueEquals(value1: Set<T>?, value2: Set<T>?): Boolean {
        if (value1 === value2) return true
        if (value1 == null || value2 == null || value1.size != value2.size) return false
        val instances = value1.associateBy { item -> item }
        return value2.all { item -> instances[item] === item }
    }

    override fun generateModelValue(): Set<T> = value

    override fun setPresentationValue(newPresentationValue: Set<T>) {
        require(items.containsAll(newPresentationValue)) { "MultiSelect value contains items outside its options" }
        close()
        renderValue(newPresentationValue)
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setAttribute("readonly", readOnly)
        close()
        renderValue(value)
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        close()
        renderValue(value)
    }

    private fun choosable(): Boolean = isEnabled && !isReadOnly

    private fun choose(selected: Set<T>) {
        close()
        setModelValue(selected.toSet(), true)
        renderValue(value)
        trigger.focus()
    }

    private fun renderValue(selected: Set<T>) {
        chips.removeAll()
        val visible = items.filter { item -> item in selected }.take(if (display == MultiSelectDisplay.Chips) maxChips else 0)
        visible.forEach { item ->
            val chip = Span(itemLabel(item)).apply { addClassName("ts-chip") }
            if (choosable()) {
                val remove = NativeButton().apply {
                    add(svgIcon(IconName.X, ICON_SIZE_TINY))
                    addClassName("ts-chip__x")
                    element.setAttribute("aria-label", texts.lookup.remove(itemLabel(item)))
                    addClickListener { if (choosable()) choose(value - item) }
                }
                chip.add(remove)
            }
            chips.add(chip)
        }
        if (display == MultiSelectDisplay.Count && selected.isNotEmpty()) {
            val counter = Span(texts.lookup.selectedCount(selected.size)).apply { addClassNames("ts-counter", "ts-counter--accent") }
            chips.add(counter)
        }
        if (display == MultiSelectDisplay.Chips && selected.size > maxChips) {
            val more = Span("+${selected.size - maxChips}").apply {
                addClassNames("ts-chip", "ts-chip--more")
                element.setAttribute("aria-label", texts.lookup.selectedCount(selected.size - maxChips))
            }
            chips.add(more)
        }
        trigger.isEnabled = choosable()
        clear.isVisible = selected.isNotEmpty() && choosable()
    }

    private fun renderDraft(focused: T? = null, focusAll: Boolean = false) {
        val current = draft ?: return
        val visible = items.filter { item -> itemLabel(item).contains(search.value.trim(), ignoreCase = true) }
        options.removeAll()
        val all = Checkbox(texts.components.selectAll).apply {
            value = visible.isNotEmpty() && current.containsAll(visible)
            isIndeterminate = visible.any { item -> item in current } && !value
            addValueChangeListener { event ->
                if (event.isFromClient) {
                    draft = if (event.value) current + visible else current - visible.toSet()
                    renderDraft(focusAll = true)
                }
            }
        }
        options.add(all)
        if (focusAll) all.focus()
        visible.forEach { item ->
            val choice = Checkbox(itemLabel(item)).apply {
                addClassName("ts-option")
                value = item in current
                addValueChangeListener { event -> if (event.isFromClient) toggle(item) }
            }
            itemMeta?.let { meta -> choice.element.setAttribute("title", meta(item)) }
            options.add(choice)
            if (item == focused) choice.focus()
        }
        if (visible.isEmpty()) options.add(Span(texts.lookup.empty))
        count.text = texts.lookup.selectedCount(current.size)
    }
}

/**
 * Presentation of accepted multi-selection values.
 *
 * @since %CURRENT_VERSION%
 */
enum class MultiSelectDisplay { Chips, Count }

/**
 * Adds a compact local multi-selection filter that ignores the block edit mode.
 *
 * @param T the type of the options.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> ContentScope.multiSelect(
    label: String,
    items: List<T>,
    itemLabel: (T) -> String,
    display: MultiSelectDisplay = MultiSelectDisplay.Chips,
    maxChips: Int = 3,
    configure: ValueInput<Set<T>>.() -> Unit = {},
): ValueInput<Set<T>> = placeComposite(
    label,
    MultiSelectField(
        texts,
        label,
        items,
        itemLabel,
        display = display,
        maxChips = maxChips,
    ),
    configure,
)
