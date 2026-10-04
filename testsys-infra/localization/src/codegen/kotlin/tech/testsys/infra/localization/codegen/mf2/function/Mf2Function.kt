package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.model.EffectiveZone
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.Operand
import tech.testsys.infra.localization.codegen.mf2.model.Value
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand

/** The Kotlin type of a message argument in the generated method. */
internal enum class ArgumentType { STRING, INT, NUMBER, INSTANT, ZONED_DATE_TIME, ZONE_ID, CURRENCY_AMOUNT }

/** How a function selects a variant of `.match`. */
internal enum class Selection {
    /** By the string value, as `:string`. */
    TEXT,

    /** By exact value or plural category, as `:integer`. */
    NUMERIC,
}

/**
 * An MF2 function the codegen supports, with everything the MF2 spec and the design of this module say about it.
 * What ICU4J 78.1 does differently is not described here but in the `mf2.icu` package.
 *
 * @property name the function name without `:`.
 * @property options the MF2 options of the function by name, plus `icu:skeleton` where ICU supports it.
 * @property isCustom whether the runtime of this module implements the function rather than ICU.
 * @property isNumeric whether the function formats a number.
 * @property selection how the function selects, or `null` if it only formats.
 * @property requiredOptions the options every expression of the function needs.
 * @property operandFunctions the functions whose declared values the function may annotate, or `null` if MF2
 *   does not restrict them.
 */
internal sealed class Mf2Function(val name: String, options: List<OptionSpec>) {
    val options: Map<String, OptionSpec> = options.associateBy(OptionSpec::name)
    open val isCustom: Boolean = false
    open val isNumeric: Boolean = false
    open val selection: Selection? = null
    open val requiredOptions: List<String> = emptyList()
    open val operandFunctions: Set<Mf2Function>? = null

    /** Returns the Kotlin type of an argument that is the operand with the combined [options]. */
    abstract fun operandType(options: Map<String, Operand>): ArgumentType

    /** Returns whether the value of an expression over [base] with the combined [options] formats as an integer. */
    open fun producesInteger(base: Value, options: Map<String, Operand>): Boolean = false

    /** Returns the problems of the [operand] of an expression with its [own] and combined [options]. */
    open fun operandProblems(operand: Operand?, own: Map<String, Operand>, options: Map<String, Operand>): List<String> = when (operand) {
        null -> listOf(Problems.Operands.missing(name))
        is LiteralOperand -> listOfNotNull(literalProblem(operand.value, options))
        is VariableOperand -> emptyList()
    }

    /** Returns the problems of the combined [options] that no single option shows. */
    open fun combinationProblems(options: Map<String, Operand>): List<String> = emptyList()

    /** Returns the zone a date function formats in with the combined [options], or `null` for other functions. */
    open fun effectiveZone(options: Map<String, Operand>): EffectiveZone? = null

    /** Returns why the literal operand [value] is invalid with the combined [options], or `null`. */
    protected open fun literalProblem(value: String, options: Map<String, Operand>): String? = null
}
