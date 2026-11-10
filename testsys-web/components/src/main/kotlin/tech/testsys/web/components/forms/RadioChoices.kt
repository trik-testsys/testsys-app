@file:OptIn(InternalComponentsApi::class)

package tech.testsys.web.components.forms

import com.vaadin.flow.component.html.Span
import com.vaadin.flow.component.radiobutton.RadioButtonGroup
import com.vaadin.flow.data.renderer.ComponentRenderer
import tech.testsys.web.components.core.CssClass
import tech.testsys.web.components.core.InternalComponentsApi
import tech.testsys.web.components.core.addClassName
import tech.testsys.web.components.layout.BlockRowScope

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
    ChoiceField(label, items, itemLabel, itemMeta),
    hint,
    configure,
)

internal class ChoiceField<T : Any>(
    label: String,
    private val choices: List<T>,
    itemLabel: (T) -> String,
    itemMeta: ((T) -> String)?,
) : RadioButtonGroup<T?>() {
    init {
        val duplicates = choices.groupBy { choice -> choice }.filterValues { same -> same.size > 1 }.keys
        require(duplicates.isEmpty()) { "Choice items of '$label' must be unique, repeated: $duplicates" }

        setAriaLabel(label)
        addClassName(CssClass.RadioGroup)
        setItemLabelGenerator { item -> itemLabel(requireNotNull(item) { "Choice of '$label' has no item to render" }) }

        if (itemMeta != null) {
            setRenderer(
                ComponentRenderer<Span, T?> { item ->
                    val choice = requireNotNull(item) { "Choice of '$label' has no item to render" }
                    Span(Span(itemLabel(choice))).apply {
                        addClassName(CssClass.ChoiceLabel)
                        add(Span(itemMeta(choice)).apply { addClassName(CssClass.OptionMeta) })
                    }
                },
            )
        }

        setItems(choices)
    }

    override fun setValue(value: T?) {
        require(value == null || value in choices) { "Choice value '$value' is outside its items" }
        super.setValue(value)
    }
}
