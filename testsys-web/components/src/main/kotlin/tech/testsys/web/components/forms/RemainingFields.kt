@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.Tag
import com.vaadin.flow.component.checkbox.Switch
import com.vaadin.flow.component.customfield.CustomField
import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Div
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.DomEvent
import tech.testsys.web.components.core.DomProperty
import tech.testsys.web.components.core.HtmlAttribute
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.add
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.core.addPropertyChangeListener
import tech.testsys.web.components.core.setAriaHidden
import tech.testsys.web.components.core.setAriaInvalid
import tech.testsys.web.components.core.setAriaRequired
import tech.testsys.web.components.core.setAttribute
import tech.testsys.web.components.core.setCodeMinLines
import tech.testsys.web.components.core.setProperty
import tech.testsys.web.components.core.setReadOnly
import tech.testsys.web.components.core.setSpellcheckPresence
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
): ValueInput<T?> = addInput(
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
): ValueInput<T?> = addInput(
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
    return addInput(label, labelSize, size, field, hint, configure)
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
    return addInput(label, labelSize, size, field, hint, configure)
}

@JsModule(SEGMENTED_CHOICE_MODULE)
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
        addClassName(if (isSegmented) CssClass.Seg else CssClass.RadioGroup)
        setItemLabelGenerator { item -> itemLabel(requireNotNull(item)) }
        if (isSegmented || itemMeta != null) {
            setRenderer(
                ComponentRenderer<Span, T?> { item ->
                    val choice = requireNotNull(item)
                    Span(Span(itemLabel(choice))).apply {
                        addClassName(CssClass.ChoiceLabel)
                        itemMeta?.let { meta -> add(Span(meta(choice)).apply { addClassName(CssClass.OptionMeta) }) }
                    }
                },
            )
        }
        setItems(choices)
        if (isSegmented) {
            addAttachListener { element.attachSegmentedChoice() }
            addDetachListener { element.detachSegmentedChoice() }
        }
    }

    override fun setValue(value: T?) {
        require(value == null || value in choices) { "Choice value '$value' is outside its items" }
        super.setValue(value)
    }
}

@Tag(NATIVE_CODE_AREA_TAG)
internal class NativeCodeArea : Component()

@JsModule(CODE_EDITOR_MODULE)
internal class CodeEditorField(label: String, minLines: Int) : CustomField<String>("", true), HasValidator<String> {
    private val numbers = Div("1").apply {
        addClassName(CssClass.CodeLines)
        element.setAriaHidden(true)
    }
    private val description = Span().apply {
        addClassName(CssClass.SrOnly)
        setId("ts-code-description-${java.util.UUID.randomUUID()}")
    }
    private val helperDescription = Span().apply {
        addClassName(CssClass.SrOnly)
        setId("ts-code-helper-${java.util.UUID.randomUUID()}")
    }
    private val area = NativeCodeArea().apply {
        element.classList.add(CssClass.CodeArea)
        element.setAttribute(HtmlAttribute.AriaLabel, label)
        element.setAttribute(HtmlAttribute.AriaDescribedBy, "${description.id.orElseThrow()} ${helperDescription.id.orElseThrow()}")
        element.setSpellcheckPresence(false)
        element.setProperty(DomProperty.Value, "")
    }

    init {
        val box = Div(numbers, area).apply {
            addClassName(CssClass.Code)
            element.style.setCodeMinLines(minLines)
        }
        add(box, description, helperDescription)
        area.element.addPropertyChangeListener(DomProperty.Value, DomEvent.Input) { event ->
            if (event.isUserOriginated) {
                if (isEnabled && !isReadOnly) {
                    val code = requireNotNull(event.value as? String) { "Code editor input must be a string" }
                    setModelValue(code, true)
                    presentLines(value)
                } else {
                    area.element.setProperty(DomProperty.Value, value)
                }
            }
        }
        addAttachListener { element.attachCodeEditor(area = area.element, numbers = numbers.element) }
        addDetachListener { area.element.detachCodeEditor() }
    }

    override fun generateModelValue(): String = value

    override fun setPresentationValue(newPresentationValue: String) {
        area.element.setProperty(DomProperty.Value, newPresentationValue)
        presentLines(newPresentationValue)
    }

    override fun setReadOnly(readOnly: Boolean) {
        super.setReadOnly(readOnly)
        element.setReadOnly(readOnly)
        area.element.setProperty(DomProperty.ReadOnly, readOnly)
    }

    override fun onEnabledStateChanged(enabled: Boolean) {
        super.onEnabledStateChanged(enabled)
        area.element.isEnabled = enabled
        area.element.setProperty(DomProperty.Disabled, !enabled)
    }

    override fun setRequiredIndicatorVisible(requiredIndicatorVisible: Boolean) {
        super.setRequiredIndicatorVisible(requiredIndicatorVisible)
        area.element.setAriaRequired(requiredIndicatorVisible)
    }

    override fun setInvalid(invalid: Boolean) {
        super.setInvalid(invalid)
        area.element.setAriaInvalid(invalid)
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
    if (placement.isCompact) field.addClassName(CssClass.SegSm)
    return addLabelLessInput(label, field, configure)
}

private fun Element.attachSegmentedChoice(): PendingJavaScriptResult = executeJs("window.testsysSegmentedChoice.attach(this)")

private fun Element.detachSegmentedChoice(): PendingJavaScriptResult = executeJs("window.testsysSegmentedChoice.detach(this)")

private fun Element.attachCodeEditor(area: Element, numbers: Element): PendingJavaScriptResult =
    executeJs("window.testsysCodeEditor.attach($0, $1)", area, numbers)

private fun Element.detachCodeEditor(): PendingJavaScriptResult = executeJs("window.testsysCodeEditor.detach(this)")
