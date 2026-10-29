// ICU4J 78.1 ignores `hour12` unless the time precision (`precision` of `:time`, `timePrecision` of `:datetime`)
// is set. `timeZoneStyle` of `:time` without `precision` is not ignored but renders only the zone name; it is rejected
// the same way. `timeZoneStyle` of `:datetime` without `timePrecision` renders only the zone name too, unless the date
// part (`dateFields` and `dateLength`) is set: then ICU renders the date with the zone, so it is accepted. Pinned by
// OptionHonouredTests ("should ignore hour12 without a time precision (ICU4J 78_1)", "should render only the time zone
// for timeZoneStyle without a time precision (ICU4J 78_1)", "should render the date with the zone for timeZoneStyle of
// datetime with a date and without a time precision (ICU4J 78_1)").
package tech.testsys.infra.localization.codegen.mf2.icu

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.DateTimeFunction
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.TimeFunction
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_LENGTH
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.HOUR12
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.PRECISION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_PRECISION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_ZONE_STYLE
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.validator.FunctionRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/**
 * Rejects `hour12` of `:time` and `:datetime` without the time precision, and `timeZoneStyle` without the time
 * precision unless it is a `:datetime` with the date part.
 */
internal object TimePrecisionRule : FunctionRule() {
    private val precisionOptions: Map<Mf2Function, String> = mapOf(
        TimeFunction to PRECISION,
        DateTimeFunction to TIME_PRECISION,
    )
    private val dependentOptions = listOf(HOUR12, TIME_ZONE_STYLE)

    override fun findProblems(expression: ResolvedExpression, function: Mf2Function, context: RuleContext): List<String> {
        val precision = precisionOptions[function] ?: return emptyList()
        val options = expression.value.options
        if (precision in options) return emptyList()
        val hasDate = function == DateTimeFunction && DATE_FIELDS in options && DATE_LENGTH in options
        val condition = Problems.Options.without(precision)
        return dependentOptions.filter { it in options && !(it == TIME_ZONE_STYLE && hasDate) }
            .map { Problems.Options.notHonoured(it, function.name, condition) }
    }
}
