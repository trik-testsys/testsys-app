// ICU4J 78.1 does not combine `icu:skeleton` with the standard formatting options: a number function formats with
// the skeleton alone and ignores the other options, and `:date` with `fields` and `length` ignores the skeleton
// instead. Pinned by OptionHonouredTests ("should ignore number options next to icu skeleton (ICU4J 78_1)", "should
// ignore icu skeleton next to date fields and length (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.DateFunction
import tech.testsys.infra.localization.codegen.mf2.function.DateTimeFunction
import tech.testsys.infra.localization.codegen.mf2.function.IntegerFunction
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.NumberFunction
import tech.testsys.infra.localization.codegen.mf2.function.TimeFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_LENGTH
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.HOUR12
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ICU_SKELETON
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.LENGTH
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.PRECISION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.SELECT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_PRECISION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_ZONE_STYLE
import tech.testsys.infra.localization.codegen.mf2.model.Operand
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects the formatting options set together with `icu:skeleton`. */
internal object SkeletonOptionsRule : FunctionRule() {
    // Every option of a number function formats, except the selection.
    private val allButSelect: (Map<String, Operand>) -> Set<String> = { options -> options.keys - SELECT }
    private val formattingOptions: Map<Mf2Function, (Map<String, Operand>) -> Set<String>> = mapOf(
        IntegerFunction to allButSelect,
        NumberFunction to allButSelect,
        DateFunction to only(FIELDS, LENGTH),
        TimeFunction to only(PRECISION, HOUR12, TIME_ZONE_STYLE),
        DateTimeFunction to only(DATE_FIELDS, DATE_LENGTH, TIME_PRECISION, HOUR12, TIME_ZONE_STYLE),
    )

    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        val options = expression.value.options
        if (ICU_SKELETON !in options) return emptyList()
        val formatting = formattingOptions[function]?.invoke(options).orEmpty()
        val condition = Problems.Options.togetherWith(ICU_SKELETON)
        return (formatting - ICU_SKELETON).map { Problems.Options.notHonoured(it, function.name, condition) }
    }

    private fun only(vararg names: String): (Map<String, Operand>) -> Set<String> = { options -> names.toSet() intersect options.keys }
}
