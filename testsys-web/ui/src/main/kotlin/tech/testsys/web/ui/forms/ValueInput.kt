package tech.testsys.web.ui.forms

import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.binder.ValidationStatusChangeListener
import com.vaadin.flow.data.binder.Validator
import com.vaadin.flow.dom.SignalBinding
import com.vaadin.flow.function.SerializableConsumer
import com.vaadin.flow.shared.Registration
import com.vaadin.flow.signals.BindingActiveException
import com.vaadin.flow.signals.Signal
import tech.testsys.web.ui.Bindable
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.layout.BlockEditState
import tech.testsys.web.ui.layout.FieldParts

/**
 * Handle of a form field whose visibility covers the label and the control; it binds to a Vaadin `Binder` like any
 * field and shows its errors. `Binder` does not skip a hidden handle, since it is not a component: call
 * `setIsAppliedPredicate { input.isVisible }` on the binding to skip it.
 *
 * @param T the type of the field value.
 * @property isEnabled whether the field is enabled; a disabled field is greyed out and ignores input. A manual change
 * while [bindEnabled] is bound, and a second binding, throw [BindingActiveException].
 * @property isEditable whether the user can change the value; a field is editable only if it and its block are,
 * otherwise it is read-only. A manual change while [bindEditable] or `bindReadOnly` is bound, and a second binding,
 * throw [BindingActiveException].
 * @since %CURRENT_VERSION%
 */
class ValueInput<T> internal constructor(
    private val parts: FieldParts,
    component: AbstractField<*, T>,
    private val validation: HasValidation,
    private val validator: HasValidator<T>,
    private val subscribe: (HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T>>) -> Registration,
) : ElementHandle(parts.field), HasValue<HasValue.ValueChangeEvent<T>, T>, HasValidation, HasValidator<T> {
    private val fieldComponent: AbstractField<*, T> = component

    private val enabled = Bindable(parts.field.element, initial = component.isEnabled) { value ->
        fieldComponent.isEnabled = value
        parts.field.setClassName("ts-field--disabled", !value)
    }

    private var isBlockEditable = true

    private val editable = Bindable(parts.field.element, initial = true) { value ->
        showReadOnly(isFieldEditable = value, isInEditableBlock = isBlockEditable)
    }

    private val requiredIndicator =
        Bindable(parts.field.element, initial = component.isRequiredIndicatorVisible) { value ->
            fieldComponent.isRequiredIndicatorVisible = value
            parts.requiredMark.isVisible = value
        }

    var isEnabled: Boolean
        get() = fieldComponent.isEnabled
        set(value) {
            enabled.value = value
        }

    var isEditable: Boolean
        get() = editable.value
        set(value) {
            editable.value = value
        }

    /** Makes the field follow the edit mode of its block. */
    internal fun followBlock(state: BlockEditState) {
        state.follow { blockEditable ->
            isBlockEditable = blockEditable
            showReadOnly(isFieldEditable = editable.value, isInEditableBlock = blockEditable)
        }
    }

    /**
     * Binds [isEnabled] to [signal]: every value it produces is applied at once. A manual [isEnabled] while bound,
     * and a second binding, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEnabled(signal: Signal<Boolean>): SignalBinding<Boolean> = enabled.bind(signal)

    /**
     * Binds [isEditable] to [signal]: every value it produces is applied at once, and the field stays read-only while
     * its block is not editable. A manual [isEditable] or `readOnly` while bound, and a second binding, including
     * `bindReadOnly`, throw [BindingActiveException].
     *
     * @since %CURRENT_VERSION%
     */
    fun bindEditable(signal: Signal<Boolean>): SignalBinding<Boolean> = editable.bind(signal)

    override fun bindReadOnly(readOnlySignal: Signal<Boolean>): SignalBinding<Boolean> =
        editable.bind(readOnlySignal) { readOnly -> !readOnly }

    override fun bindRequiredIndicatorVisible(requiredSignal: Signal<Boolean>): SignalBinding<Boolean> =
        requiredIndicator.bind(requiredSignal)

    override fun bindValue(valueSignal: Signal<T>, writeCallback: SerializableConsumer<T>?): SignalBinding<T> =
        fieldComponent.bindValue(valueSignal, writeCallback)

    override fun setValue(value: T) {
        fieldComponent.value = value
    }

    override fun getValue(): T = fieldComponent.value

    override fun getEmptyValue(): T = fieldComponent.emptyValue

    override fun addValueChangeListener(listener: HasValue.ValueChangeListener<in HasValue.ValueChangeEvent<T>>): Registration =
        subscribe(listener)

    override fun setReadOnly(readOnly: Boolean) {
        isEditable = !readOnly
    }

    override fun isReadOnly(): Boolean = fieldComponent.isReadOnly

    override fun setRequiredIndicatorVisible(requiredIndicatorVisible: Boolean) {
        requiredIndicator.value = requiredIndicatorVisible
    }

    override fun isRequiredIndicatorVisible(): Boolean = fieldComponent.isRequiredIndicatorVisible

    override fun setErrorMessage(errorMessage: String?) {
        validation.errorMessage = errorMessage
    }

    override fun getErrorMessage(): String? = validation.errorMessage

    override fun setInvalid(invalid: Boolean) {
        validation.isInvalid = invalid
    }

    override fun isInvalid(): Boolean = validation.isInvalid

    override fun setManualValidation(enabled: Boolean) {
        validation.setManualValidation(enabled)
    }

    override fun getDefaultValidator(): Validator<T> = validator.defaultValidator

    override fun addValidationStatusChangeListener(listener: ValidationStatusChangeListener<T>): Registration? =
        validator.addValidationStatusChangeListener(listener)

    /** Makes the control read-only unless both the field and its block are editable. */
    private fun showReadOnly(isFieldEditable: Boolean, isInEditableBlock: Boolean) {
        fieldComponent.isReadOnly = !(isFieldEditable && isInEditableBlock)
    }
}
