package tech.testsys.web.components.forms

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.NativeButton
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.data.binder.HasValidator
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

private const val MAX_SEGMENTED_CHOICES = 4
private const val DEFAULT_EDITOR_LINES = 10
private const val EDITOR_LINE_PIXELS = 21
private const val EDITOR_VERTICAL_PADDING = 28

/**
 * Adds one radio choice from [items] shown by [itemLabel] on [size] columns, beside [labelSize] label columns.
 *
 * @param T the type of the choices.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.radio(
    label: String,
    labelSize: Int,
    size: Int,
    items: List<T>,
    itemLabel: (T) -> String,
    hint: String? = null,
    itemMeta: ((T) -> String)? = null,
    configure: ValueInput<T?>.() -> Unit = {},
): ValueInput<T?> = choiceInput(
    label,
    labelSize,
    size,
    ChoiceField(label, items, itemLabel, itemMeta, isSegmented = false),
    hint,
    configure,
)

/**
 * Adds a isSegmented choice from [items] shown by [itemLabel] on [size] columns beside [labelSize] label columns.
 *
 * @param T the type of the choices.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> BlockRowScope.segmentedControl(
    label: String,
    labelSize: Int,
    size: Int,
    items: List<T>,
    itemLabel: (T) -> String,
    hint: String? = null,
    itemMeta: ((T) -> String)? = null,
    configure: ValueInput<T?>.() -> Unit = {},
): ValueInput<T?> = choiceInput(
    label,
    labelSize,
    size,
    ChoiceField(label, items, itemLabel, itemMeta, isSegmented = true),
    hint,
    configure,
)

/**
 * Adds a boolean switch on [size] columns beside [labelSize] label columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.switchInput(
    label: String,
    labelSize: Int,
    size: Int,
    hint: String? = null,
    configure: ValueInput<Boolean>.() -> Unit = {},
): ValueInput<Boolean> {
    val field = SwitchField(label)
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<Boolean>> ->
        field.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return placeInput(label, labelSize, size, field, hint, subscribe, configure)
}

/**
 * Adds a plain code editor with line numbers and [minLines] visible lines beside [labelSize] label columns.
 *
 * @since %CURRENT_VERSION%
 */
fun BlockRowScope.codeEditor(
    label: String,
    labelSize: Int,
    size: Int,
    minLines: Int = DEFAULT_EDITOR_LINES,
    hint: String? = null,
    configure: ValueInput<String>.() -> Unit = {},
): ValueInput<String> {
    require(minLines > 0) { "Code editor minimum lines must be positive, got $minLines" }
    val field = CodeEditorField(label, minLines)
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<String>> ->
        field.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return placeInput(label, labelSize, size, field, hint, subscribe, configure)
}

private fun <T : Any> BlockRowScope.choiceInput(
    label: String,
    labelSize: Int,
    size: Int,
    field: ChoiceField<T>,
    hint: String?,
    configure: ValueInput<T?>.() -> Unit,
): ValueInput<T?> {
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T?>> ->
        field.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return placeInput(label, labelSize, size, field, hint, subscribe, configure)
}

internal class ChoiceField<T : Any>(
    label: String,
    private val items: List<T>,
    itemLabel: (T) -> String,
    itemMeta: ((T) -> String)?,
    private val isSegmented: Boolean,
) : CustomField<T?>(null, true), HasValidator<T?> {
    private val group = Div().apply {
        addClassName(if (isSegmented) "ts-seg" else "ts-radio-group")
        element.setAttribute("role", "radiogroup")
        element.setAttribute("aria-label", label)
    }

    private val buttons: List<NativeButton>

    init {
        require(
            !isSegmented || items.size in 2..MAX_SEGMENTED_CHOICES,
        ) { "Segmented controls require two to four choices, got ${items.size}" }
        require(items.distinct().size == items.size) { "Choice items must be unique" }
        buttons = items.map { item ->
            NativeButton().apply {
                element.setAttribute("type", "button")
                element.setAttribute("role", "radio")
                if (isSegmented) {
                    addClassName("ts-seg__item")
                    text = itemLabel(item)
                } else {
                    addClassName("ts-choice")
                    add(
                        Span().apply {
                            addClassName("ts-radio")
                            add(Span().apply { addClassName("ts-radio__dot") })
                        },
                        Span(itemLabel(item)),
                    )
                }
                itemMeta?.let { meta -> add(Span(meta(item)).apply { addClassName("ts-option__meta") }) }
                addClickListener {
                    if (isEnabled && !isReadOnly) {
                        setModelValue(item, true)
                        show(item)
                    }
                }
            }
        }
        buttons.forEach(group::add)
        group.element.executeJs(
            """
            this.addEventListener('keydown', e => {
              if (!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Home','End'].includes(e.key)) return;
              const bs = [...this.querySelectorAll('button:not([disabled])')]; if (!bs.length) return;
              let i = bs.indexOf(e.target); if (i < 0) return; e.preventDefault();
              i = e.key === 'Home' ? 0 : e.key === 'End' ? bs.length-1 :
                (i + (['ArrowLeft','ArrowUp'].includes(e.key) ? -1 : 1) + bs.length) % bs.length;
              bs[i].focus(); bs[i].click();
            });
        """,
        )
        add(group)
        show(null)
    }

    override fun generateModelValue(): T? = value

    override fun setPresentationValue(newPresentationValue: T?) {
        require(
            newPresentationValue == null || newPresentationValue in items,
        ) { "Choice value '$newPresentationValue' is outside its items" }
        show(newPresentationValue)
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setAttribute("readonly", readOnly)
        show(value)
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        show(value)
    }

    private fun show(selected: T?) {
        buttons.forEachIndexed { index, button ->
            val isChecked = items[index] == selected
            button.element.setAttribute("aria-checked", isChecked.toString())
            button.element.setAttribute("tabindex", if (isChecked || (selected == null && index == 0)) "0" else "-1")
            button.isEnabled = isEnabled && !isReadOnly
            if (isSegmented) {
                button.setClassName("ts-seg__item--active", isChecked)
            } else {
                val glyph = button.children.findFirst().orElseThrow().element
                if (isChecked) {
                    glyph.classList.add("ts-radio--on")
                } else {
                    glyph.classList.remove("ts-radio--on")
                }
            }
        }
    }
}

internal class SwitchField(label: String) : CustomField<Boolean>(false, true), HasValidator<Boolean> {
    private val trigger = NativeButton().apply {
        addClassName("ts-switch")
        element.setAttribute("type", "button")
        element.setAttribute("role", "switch")
        element.setAttribute("aria-label", label)
        add(Span().apply { addClassName("ts-switch__knob") })
    }

    init {
        add(trigger)
        trigger.addClickListener {
            if (isEnabled && !isReadOnly) {
                setModelValue(!value, true)
                present(value)
            }
        }
        present(false)
    }

    override fun generateModelValue(): Boolean = value

    override fun setPresentationValue(newPresentationValue: Boolean) {
        present(newPresentationValue)
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setAttribute("readonly", readOnly)
        present(value)
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        present(value)
    }

    private fun present(selected: Boolean) {
        trigger.setClassName("ts-switch--on", selected)
        trigger.element.setAttribute("aria-checked", selected.toString())
        trigger.isEnabled = isEnabled && !isReadOnly
    }
}

@Tag("textarea")
internal class NativeCodeArea : Component()

@JsModule("./testsys-ui/code-editor.ts")
internal class CodeEditorField(label: String, minLines: Int) : CustomField<String>("", true), HasValidator<String> {
    private val numbers = Div("1").apply {
        addClassName("ts-code__lines")
        element.setAttribute("aria-hidden", true)
    }
    private val description = Span().apply {
        addClassName("ts-sr-only")
        setId("ts-code-description-${java.util.UUID.randomUUID()}")
    }
    private val helperDescription = Span().apply {
        addClassName("ts-sr-only")
        setId("ts-code-helper-${java.util.UUID.randomUUID()}")
    }
    private val area = NativeCodeArea().apply {
        element.classList.add("ts-code__area")
        element.setAttribute("aria-label", label)
        element.setAttribute("aria-describedby", "${description.id.orElseThrow()} ${helperDescription.id.orElseThrow()}")
        element.setAttribute("spellcheck", false)
        element.setProperty("value", "")
        element.style.set("min-height", "0")
        element.style.set("height", "100%")
        element.style.set("resize", "none")
        element.style.set("box-sizing", "border-box")
    }

    init {
        val height = minLines * EDITOR_LINE_PIXELS + EDITOR_VERTICAL_PADDING
        val box = Div(numbers, area).apply {
            addClassName("ts-code")
            element.style.set("min-height", "${height}px")
            element.style.set("height", "${height}px")
            element.style.set("resize", "vertical")
            element.style.set("overflow", "hidden")
        }
        add(box, description, helperDescription)
        area.element.addPropertyChangeListener("value", "input") { event ->
            if (event.isUserOriginated) {
                if (isEnabled && !isReadOnly) {
                    val code = requireNotNull(event.value as? String) { "Code editor input must be a string" }
                    setModelValue(code, true)
                    presentLines(value)
                } else {
                    area.element.setProperty("value", value)
                }
            }
        }
        addAttachListener { element.executeJs("window.testsysCodeEditor.attach($0, $1)", area.element, numbers.element) }
        addDetachListener { area.element.executeJs("window.testsysCodeEditor.detach(this)") }
    }

    override fun generateModelValue(): String = value

    override fun setPresentationValue(newPresentationValue: String) {
        area.element.setProperty("value", newPresentationValue)
        presentLines(newPresentationValue)
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setAttribute("readonly", readOnly)
        area.element.setProperty("readOnly", readOnly)
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        area.element.isEnabled = enabled
        area.element.setProperty("disabled", !enabled)
    }

    override fun setRequiredIndicatorVisible(requiredIndicatorVisible: Boolean) {
        super.setRequiredIndicatorVisible(requiredIndicatorVisible)
        area.element.setAttribute("aria-required", requiredIndicatorVisible.toString())
    }

    override fun setInvalid(invalid: Boolean) {
        super.setInvalid(invalid)
        area.element.setAttribute("aria-invalid", invalid.toString())
        description.text = if (invalid) errorMessage.orEmpty() else ""
    }

    override fun setErrorMessage(errorMessage: String?) {
        super.setErrorMessage(errorMessage)
        description.text = errorMessage.orEmpty().takeIf { isInvalid }.orEmpty()
    }

    override fun setHelperText(helperText: String?) {
        super.setHelperText(helperText)
        helperDescription.text = helperText.orEmpty()
    }

    private fun presentLines(code: String) {
        numbers.text = (1..(code.count { char -> char == '\n' } + 1)).joinToString("\n")
    }
}

/**
 * Adds a compact segmented filter of [items], ignoring the block edit mode.
 *
 * @param T the type of the choices.
 * @since %CURRENT_VERSION%
 */
fun <T : Any> ContentScope.segmentedControl(
    label: String,
    items: List<T>,
    itemLabel: (T) -> String,
    configure: ValueInput<T?>.() -> Unit = {},
): ValueInput<T?> {
    val field = ChoiceField(label, items, itemLabel, null, isSegmented = true)
    if (placement.isCompact) field.addClassName("ts-seg--sm")
    return placeComposite(label, field, configure)
}

internal fun <T, C> ContentScope.placeComposite(
    label: String,
    field: C,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : CustomField<T>, C : HasValidator<T> {
    nameControl(field, label)
    field.element.setAttribute("data-ts-input", true)
    add(field)
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T>> ->
        field.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return ValueInput(parts = null, field, field, field, subscribe).apply(configure)
}
