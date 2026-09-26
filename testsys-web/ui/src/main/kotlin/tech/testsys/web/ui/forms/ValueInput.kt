package tech.testsys.web.ui.forms

import com.vaadin.flow.component.AbstractField
import com.vaadin.flow.component.HasValidation
import com.vaadin.flow.component.HasValue
import com.vaadin.flow.data.binder.HasValidator
import com.vaadin.flow.data.binder.ValidationStatusChangeListener
import com.vaadin.flow.data.binder.Validator
import com.vaadin.flow.shared.Registration
import tech.testsys.web.ui.ElementHandle
import tech.testsys.web.ui.layout.BlockEditState
import tech.testsys.web.ui.layout.FieldParts

/**
 * Handle of a form field whose visibility covers the label and the control; it binds to a Vaadin `Binder` like any
 * field and shows its errors. `Binder` does not skip a hidden handle, since it is not a component: call
 * `setIsAppliedPredicate { input.isVisible }` on the binding to skip it.
 *
 * @param T the type of the field value.
 * @property isEnabled whether the field is enabled; a disabled field is greyed out and ignores input.
 * @property isEditable whether the user can change the value; a field is editable only if it and its block are,
 * otherwise it is read-only.
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

    var isEnabled: Boolean
        get() = fieldComponent.isEnabled
        set(value) {
            fieldComponent.isEnabled = value
            parts.field.setClassName("ts-field--disabled", !value)
        }

    var isEditable: Boolean = true
        set(value) {
            field = value
            applyReadOnly()
        }

    private var isBlockEditable = true

    /** Makes the field follow the edit mode of its block. */
    internal fun followBlock(state: BlockEditState) {
        state.follow { editable ->
            isBlockEditable = editable
            applyReadOnly()
        }
    }

    private fun applyReadOnly() {
        fieldComponent.isReadOnly = !(isEditable && isBlockEditable)
    }

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
        fieldComponent.isRequiredIndicatorVisible = requiredIndicatorVisible
        parts.requiredMark.isVisible = requiredIndicatorVisible
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
}
