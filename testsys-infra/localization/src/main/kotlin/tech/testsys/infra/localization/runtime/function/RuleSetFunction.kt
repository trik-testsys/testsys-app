// FunctionFactory and Function are an ICU technology preview (@Deprecated @internal); only the package runtime
// touches the MF2 API.
@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime.function

import com.ibm.icu.message2.Function
import com.ibm.icu.message2.FunctionFactory
import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.InternalLocalizationApi
import java.math.BigDecimal
import java.util.Locale

/**
 * `:spellout` and `:ordinal`: the function [name], an ICU rule-based number format of the [kind] in [regionLocale].
 * Operands must be finite; shifted integers must fit in Long and fractions in finite Double. [SPELLOUT] and
 * [ORDINAL] are the function names; `rules` selects the rule set.
 */
@InternalLocalizationApi
internal class RuleSetFunction(private val name: String, private val regionLocale: ULocale, private val kind: Int) : FunctionFactory {
    // RuleBasedNumberFormat is not thread-safe and expensive to build, so each thread keeps its own.
    private val formats = ThreadLocal.withInitial { RuleBasedNumberFormat(regionLocale, kind) }

    override fun create(locale: Locale, fixedOptions: Map<String, Any?>): Function = object : TextFunction() {
        override fun text(toFormat: Any?): String {
            val ruleSet = RULE_SET_PREFIX + requireNotNull(fixedOptions.text(RULES)) { "':$name' needs the option '$RULES'" }
            val number = operandValue(toFormat, fixedOptions)
            require(isFinite(number)) { "':$name' operand '$number' must be finite (NaN and infinity are not supported)" }
            val decimal = BigDecimal(number.toString())
            val format = formats.get()
            return if (decimal.stripTrailingZeros().scale() <= 0) {
                val integer = try {
                    decimal.longValueExact()
                } catch (e: ArithmeticException) {
                    throw IllegalArgumentException("':$name' operand '$number' is an integer outside the Long range", e)
                }
                format.format(integer, ruleSet)
            } else {
                val fraction = number.toDouble()
                require(fraction.isFinite()) { "':$name' operand '$number' is a fraction outside the finite Double range" }
                format.format(fraction, ruleSet)
            }
        }
    }

    companion object {
        const val SPELLOUT = "spellout"
        const val ORDINAL = "ordinal"

        private const val RULES = "rules"
        private const val RULE_SET_PREFIX = "%"
    }
}
