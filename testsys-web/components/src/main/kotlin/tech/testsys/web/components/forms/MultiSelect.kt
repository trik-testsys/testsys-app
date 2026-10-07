@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.checkbox.Checkbox
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.component.popover.Popover
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.value.ValueChangeMode
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.AriaPopup
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.CssTheme
import tech.testsys.web.components.core.ElementType
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.ICON_SIZE_TINY
import tech.testsys.web.components.core.IconName
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addClassNames
import tech.testsys.web.components.core.addThemeName
import tech.testsys.web.components.core.setAriaExpanded
import tech.testsys.web.components.core.setAriaHasPopup
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setClassName
import tech.testsys.web.components.core.setReadOnly
import tech.testsys.web.components.core.setType
import tech.testsys.web.components.core.svgIcon
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope
import tech.testsys.web.components.texts.UiTexts

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
    return addInput(label, labelSize, size, field, hint, configure)
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
        element.setType(ElementType.Button)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
        element.setAriaHasPopup(AriaPopup.Dialog)
        addClassName(CssClass.LookupText)
    }

    private val chips = Div().apply { addClassName(CssClass.LookupChips) }
    private val clear = NativeButton(texts.lookup.clear).apply {
        element.setType(ElementType.Button)
        addClassName(CssClass.MultiselectClear)
    }

    private val box = Div(trigger, chips, clear).apply { addClassNames(CssClass.Trigger, CssClass.Lookup, CssClass.LookupMany) }
    private val popup = Popover().apply {
        target = trigger
        isOpenOnClick = false
        isModal = true
        isAutofocus = true
        isCloseOnEsc = true
        isCloseOnOutsideClick = true
        setAriaLabel(label)
        addThemeName(CssTheme.Popover)
    }

    private val search = TextField().apply {
        placeholder = texts.lookup.search
        setAriaLabel(texts.lookup.search)
        valueChangeMode = ValueChangeMode.EAGER
        addClassName(CssClass.PopoverSearch)
    }

    private val options = Div().apply { addClassName(CssClass.Options) }
    private val count = Span()
    private var draft: Set<T>? = null

    init {
        clear.addClickListener { if (choosable()) choose(emptySet()) }
        trigger.addClickListener { if (popup.isOpened) close() else open() }
        search.addValueChangeListener { renderDraft() }
        require(maxChips > 0) { "MultiSelect max chips must be positive, got $maxChips" }
        require(items.distinct().size == items.size) { "MultiSelect '$label' items must be unique" }
        val foot = Div(count).apply { addClassName(CssClass.PopoverFoot) }
        foot.add(
            NativeButton(texts.lookup.reset).apply {
                element.setType(ElementType.Button)
                addClassNames(CssClass.Btn, CssClass.BtnSecondary)
                addClickListener {
                    draft = emptySet()
                    renderDraft()
                }
            },
            NativeButton(texts.lookup.apply).apply {
                addClassNames(CssClass.Btn, CssClass.BtnPrimary)
                addClickListener { applyDraft() }
            },
        )
        popup.add(search, options, foot)
        popup.addOpenedChangeListener { event ->
            trigger.element.setAriaExpanded(event.isOpened)
            box.setClassName(CssClass.TriggerOpen, event.isOpened)
            if (!event.isOpened) {
                draft = null
                if (isAttached) trigger.element.restoreTriggerFocus(popup.element)
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
        require(items.containsAll(value)) { "MultiSelect value items ${value - items.toSet()} are outside its options" }
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
        require(items.containsAll(newPresentationValue)) {
            "MultiSelect value items ${newPresentationValue - items.toSet()} are outside its options"
        }
        close()
        renderValue(newPresentationValue)
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setReadOnly(readOnly)
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
            val chip = Span(Span(itemLabel(item)).apply { addClassName(CssClass.ObscuredValue) }).apply { addClassName(CssClass.Chip) }
            if (choosable()) {
                val remove = NativeButton().apply {
                    add(svgIcon(IconName.X, ICON_SIZE_TINY))
                    addClassName(CssClass.ChipX)
                    element.setAttribute(HtmlAttribute.AriaLabel, texts.lookup.remove(itemLabel(item)))
                    addClickListener { if (choosable()) choose(value - item) }
                }
                chip.add(remove)
            }
            chips.add(chip)
        }
        if (display == MultiSelectDisplay.Count && selected.isNotEmpty()) {
            val counter = Span(texts.lookup.selectedCount(selected.size)).apply {
                addClassNames(CssClass.Counter, CssClass.CounterAccent, CssClass.ObscuredValue)
            }
            chips.add(counter)
        }
        if (display == MultiSelectDisplay.Chips && selected.size > maxChips) {
            val more = Span("+${selected.size - maxChips}").apply {
                addClassNames(CssClass.Chip, CssClass.ChipMore, CssClass.ObscuredValue)
                element.setAttribute(HtmlAttribute.AriaLabel, texts.components.more(selected.size - maxChips))
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
                addClassName(CssClass.Option)
                value = item in current
                addValueChangeListener { event -> if (event.isFromClient) toggle(item) }
            }
            itemMeta?.let { meta -> choice.element.setAttribute(HtmlAttribute.Title, meta(item)) }
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
): ValueInput<Set<T>> = placeLabelLessInput(
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

/** Returns focus to the trigger only if it stayed in [popup] or was lost, so a click elsewhere keeps its target. */
private fun Element.restoreTriggerFocus(popup: Element): PendingJavaScriptResult =
    executeJs("const active = document.activeElement; if (active === document.body || $0.contains(active)) this.focus();", popup)
