package tech.testsys.web.components.forms

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.checkbox.Switch
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.renderer.ComponentRenderer
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

private const val MAX_SEGMENTED_CHOICES = 4
private const val DEFAULT_EDITOR_LINES = 10

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
 * Adds a segmented choice from [items] shown by [itemLabel] on [size] columns beside [labelSize] label columns.
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
    val field = Switch()
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

@JsModule("./testsys-ui/segmented-choice.ts")
internal class ChoiceField<T : Any>(
    label: String,
    private val choices: List<T>,
    itemLabel: (T) -> String,
    itemMeta: ((T) -> String)?,
    isSegmented: Boolean,
) : RadioButtonGroup<T?>() {
    init {
        require(
            !isSegmented || choices.size in 2..MAX_SEGMENTED_CHOICES,
        ) { "Segmented controls require two to four choices, got ${choices.size}" }
        require(choices.distinct().size == choices.size) { "Choice items must be unique" }
        setAriaLabel(label)
        addClassName(if (isSegmented) "ts-seg" else "ts-radio-group")
        setItemLabelGenerator { item -> itemLabel(requireNotNull(item)) }
        if (isSegmented || itemMeta != null) {
            setRenderer(
                ComponentRenderer<Span, T?> { item ->
                    val choice = requireNotNull(item)
                    Span(Span(itemLabel(choice))).apply {
                        addClassName("ts-choice-label")
                        itemMeta?.let { meta -> add(Span(meta(choice)).apply { addClassName("ts-option__meta") }) }
                    }
                },
            )
        }
        setItems(choices)
        if (isSegmented) {
            addAttachListener { element.executeJs("window.testsysSegmentedChoice.attach(this)") }
            addDetachListener { element.executeJs("window.testsysSegmentedChoice.detach(this)") }
        }
    }

    override fun setValue(value: T?) {
        require(value == null || value in choices) { "Choice value '$value' is outside its items" }
        super.setValue(value)
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
    }

    init {
        val box = Div(numbers, area).apply {
            addClassName("ts-code")
            element.style.set("--ts-code-min-lines", minLines.toString())
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
    return addLabelLessInput(label, field, configure)
}

internal fun <T, C> ContentScope.placeComposite(
    label: String,
    field: C,
    configure: ValueInput<T>.() -> Unit,
): ValueInput<T>
    where C : CustomField<T>, C : HasValidator<T> {
    val subscribe = { listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T>> ->
        field.addValueChangeListener { event -> listener.valueChanged(event) }
    }
    return placeLabelLessInput(label, field, subscribe, configure)
}
