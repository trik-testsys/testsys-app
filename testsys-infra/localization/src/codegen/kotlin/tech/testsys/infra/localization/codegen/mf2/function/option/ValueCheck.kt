package tech.testsys.infra.localization.codegen.mf2.function.option

import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems

/** A check of a literal option value. */
internal fun interface ValueCheck {
    /** Returns why [value] is invalid for [locale], or `null` if it is valid. */
    fun findProblem(value: String, locale: ULocale): String?
}

/** The value is one of [values]. */
internal class EnumValues(vararg values: String) : ValueCheck {
    private val values: List<String> = values.toList()

    override fun findProblem(value: String, locale: ULocale): String? = if (value in values) null else Problems.Values.expectedOneOf(values)
}

/** The value is a digit size `0` or `[1-9][0-9]*` from [minimum] to 999. */
internal class DigitSize(private val minimum: Int = 0) : ValueCheck {
    override fun findProblem(value: String, locale: ULocale): String? {
        val number = value.takeIf(SHAPE::matches)?.toIntOrNull()
        return if (number != null && number in minimum..MAX_DIGITS) null else Problems.Values.digitRange(minimum, MAX_DIGITS)
    }

    private companion object {
        const val MAX_DIGITS = 999
        val SHAPE = Regex("0|[1-9][0-9]*")
    }
}

/** The value matches [pattern]; [mismatchProblem] explains the mismatch. */
internal class PatternCheck(private val pattern: Regex, private val mismatchProblem: String) : ValueCheck {
    override fun findProblem(value: String, locale: ULocale): String? = if (pattern.matches(value)) null else mismatchProblem
}
