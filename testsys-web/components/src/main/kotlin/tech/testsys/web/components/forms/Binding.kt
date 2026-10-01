package tech.testsys.web.components.forms

import com.vaadin.flow.data.binder.Binder

/**
 * Skips this binding during general Binder validation and explicit `writeBean`/`writeBeanIfValid` while its
 * [ValueInput] or any server-side ancestor is hidden, preserving the bean value until the field is shown again.
 *
 * Use with explicit form saving: `readBean` still populates hidden fields and performs its usual conversions.
 * Direct `Binding.validate`, field change handling and automatic writes through `setBean` retain Vaadin behaviour;
 * CSS-only visibility is not considered. Replaces any previously configured application predicate.
 *
 * @param BEAN the type of the bound bean.
 * @param TARGET the converted model type, independent of the field value type.
 * @return this binding.
 * @throws IllegalArgumentException if the bound field is not a [ValueInput].
 * @since %CURRENT_VERSION%
 */
@Suppress("VERBOSE_DOC")
fun <BEAN, TARGET> Binder.Binding<BEAN, TARGET>.skipWhenHidden(): Binder.Binding<BEAN, TARGET> {
    val input = field
    require(input is ValueInput<*>) { "skipWhenHidden requires ValueInput, got ${input.javaClass.name}" }
    setIsAppliedPredicate {
        generateSequence(input.component.element) { element -> element.parent }
            .all { element -> element.isVisible }
    }
    return this
}
