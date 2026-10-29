package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.Problems

/**
 * Shapes of MF2 literals: [NUMBER] and [INTEGER] literals, [EXACT_KEY] numeric variant keys and [ISO_DATE] dates.
 */
internal object Literals {
    val NUMBER = Regex("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+\\-]?[0-9]+)?")
    val INTEGER = Regex("-?(0|[1-9][0-9]*)")
    val EXACT_KEY = Regex("0|-?[1-9][0-9]*")

    // Calendar validity is intentionally not checked; these fragments preserve the accepted literal shapes.
    private const val DATE = "([0-9]{4})-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])"
    private const val TIME = "T([01][0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])"
    private const val FRACTION = "(\\.[0-9]{1,3})?"

    // At the maximum offset of fourteen hours, no nonzero minutes are allowed.
    private const val ZONE = "(Z|[+-]((0[0-9]|1[0-3]):[0-5][0-9]|14:00))?"

    val ISO_DATE = Regex("$DATE($TIME$FRACTION$ZONE)?")

    /** Returns why [value] is not a number literal operand of [function], or `null`. */
    fun numberProblem(value: String, function: String): String? =
        if (NUMBER.matches(value)) null else Problems.Operands.notNumber(value, function)

    /** Returns why [value] is not an integer literal operand of [function], or `null`. */
    fun integerProblem(value: String, function: String): String? =
        if (INTEGER.matches(value)) null else Problems.Operands.notInteger(value, function)

    /** Returns why [value] is not an ISO 8601 date literal operand of [function], or `null`. */
    fun isoDateProblem(value: String, function: String): String? =
        if (ISO_DATE.matches(value)) null else Problems.Operands.notIsoDate(value, function)
}
