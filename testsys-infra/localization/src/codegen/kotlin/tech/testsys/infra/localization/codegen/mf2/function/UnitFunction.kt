package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.option.EnumValues
import tech.testsys.infra.localization.codegen.mf2.function.option.NumberOptions
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.COMPACT_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MAXIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_FRACTION_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.MINIMUM_SIGNIFICANT_DIGITS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.NOTATION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_INCREMENT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_MODE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ROUNDING_PRIORITY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.UNIT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.UNIT_DISPLAY
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.USAGE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.function.option.PatternCheck
import tech.testsys.infra.localization.codegen.mf2.function.option.UnitIdentifierCheck
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/**
 * `:unit`: a number with a measurement unit. The runtime of this module turns every option into a stem of an ICU
 * number skeleton, so the options combine as the skeleton requires.
 */
internal data object UnitFunction : Mf2Function(
    name = "unit",
    options = listOf(
        OptionSpec(UNIT, ArgumentType.STRING, UnitIdentifierCheck),
        OptionSpec(USAGE, check = PatternCheck(Regex("[a-z]+(-[a-z]+)*"), Problems.Values.USAGE)),
        OptionSpec(UNIT_DISPLAY, check = EnumValues("short", "narrow", "long")),
        NumberOptions.notation,
        NumberOptions.compactDisplay,
        NumberOptions.numberingSystem,
        NumberOptions.signDisplay,
        NumberOptions.useGrouping,
        NumberOptions.minimumIntegerDigits,
        NumberOptions.minimumFractionDigits,
        NumberOptions.maximumFractionDigits,
        NumberOptions.minimumSignificantDigits,
        NumberOptions.maximumSignificantDigits,
        NumberOptions.roundingPriority,
        NumberOptions.roundingIncrement,
        // ICU number skeletons have no stems for the rounding modes halfCeil and halfFloor.
        OptionSpec(ROUNDING_MODE, check = EnumValues("ceil", "floor", "expand", "trunc", "halfExpand", "halfTrunc", "halfEven")),
        NumberOptions.trailingZeroDisplay,
    ),
) {
    private const val AUTO_PRIORITY = "auto"
    private const val NO_INCREMENT = "1"

    override val isCustom: Boolean = true
    override val isNumeric: Boolean = true
    override val requiredOptions: List<String> = listOf(UNIT)
    override val operandFunctions: Set<Mf2Function> by lazy { setOf(IntegerFunction, NumberFunction, OffsetFunction) }

    override fun operandType(options: Map<String, Operand>): ArgumentType = ArgumentType.NUMBER

    override fun combinationProblems(options: Map<String, Operand>): List<String> = listOfNotNull(
        compactDisplayProblem(options),
        roundingPriorityProblem(options),
        roundingIncrementProblem(options),
        usageProblem(options),
    )

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.numberProblem(value, name)

    private fun compactDisplayProblem(options: Map<String, Operand>): String? {
        val hasEffect = COMPACT_DISPLAY !in options || options[NOTATION] == LiteralOperand(COMPACT)
        return if (hasEffect) null else Problems.Options.noEffectWithout(COMPACT_DISPLAY, name, NOTATION, COMPACT)
    }

    // The skeleton stem `.00/@@r` needs both limits.
    private fun roundingPriorityProblem(options: Map<String, Operand>): String? {
        val isSet = priority(options) != AUTO_PRIORITY
        val hasBothLimits = hasFractionDigits(options) && hasSignificantDigits(options)
        return if (isSet && !hasBothLimits) Problems.Options.needsFractionAndSignificant(ROUNDING_PRIORITY, name) else null
    }

    // The skeleton stem `precision-increment/0.05` takes its scale from the maximum fraction digits.
    private fun roundingIncrementProblem(options: Map<String, Operand>): String? {
        val isSet = (literal(options, ROUNDING_INCREMENT) ?: NO_INCREMENT) != NO_INCREMENT
        val hasScale = MAXIMUM_FRACTION_DIGITS in options && !hasSignificantDigits(options) && priority(options) == AUTO_PRIORITY
        return if (isSet && !hasScale) {
            Problems.Options.needsMaximumFraction(ROUNDING_INCREMENT, name, MAXIMUM_FRACTION_DIGITS, ROUNDING_PRIORITY)
        } else {
            null
        }
    }

    // The unit category, which decides the valid usages, is known only for a literal unit.
    private fun usageProblem(options: Map<String, Operand>): String? {
        val hasUsageWithoutLiteralUnit = options[USAGE] is LiteralOperand && options[UNIT] !is LiteralOperand
        return if (hasUsageWithoutLiteralUnit) Problems.Options.needsLiteral(USAGE, name, UNIT) else null
    }

    private fun priority(options: Map<String, Operand>): String = literal(options, ROUNDING_PRIORITY) ?: AUTO_PRIORITY

    private fun hasFractionDigits(options: Map<String, Operand>): Boolean =
        MINIMUM_FRACTION_DIGITS in options || MAXIMUM_FRACTION_DIGITS in options

    private fun hasSignificantDigits(options: Map<String, Operand>): Boolean =
        MINIMUM_SIGNIFICANT_DIGITS in options || MAXIMUM_SIGNIFICANT_DIGITS in options

    private fun literal(options: Map<String, Operand>, option: String): String? = (options[option] as? LiteralOperand)?.value
}
