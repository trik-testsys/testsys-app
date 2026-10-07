@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.dependency.JsModule
import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.page.PendingJavaScriptResult
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.data.renderer.ComponentRenderer
import com.vaadin.flow.dom.Element
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.layout.BlockRowScope
import tech.testsys.web.components.layout.ContentScope

private const val MAX_SEGMENTED_CHOICES = 4

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
        val duplicates = choices.groupBy { choice -> choice }.filterValues { same -> same.size > 1 }.keys
        require(duplicates.isEmpty()) { "Choice items of '$label' must be unique, repeated: $duplicates" }
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
