package tech.testsys.infra.localization.codegen.mf2.function.option

import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.Operand

/**
 * How a numeric selector matches keys: the `select` option. `CHECK` accepts only the values that differ from the
 * default, so the default is never written and there is one way to write it.
 *
 * @property keyword the value of the `select` option.
 */
internal enum class SelectMode(val keyword: String) {
    /** By plural category, the default. */
    PLURAL("plural"),

    /** By ordinal category. */
    ORDINAL("ordinal"),

    /** By exact value only. */
    EXACT("exact"),
    ;

    /** Returns the categories the mode selects by in [locale], or `null` if it matches exact values only. */
    fun pluralRules(locale: ULocale): PluralRules? = when (this) {
        PLURAL -> PluralRules.forLocale(locale, PluralRules.PluralType.CARDINAL)
        ORDINAL -> PluralRules.forLocale(locale, PluralRules.PluralType.ORDINAL)
        EXACT -> null
    }

    companion object {
        private const val CARDINAL = "cardinal"

        val CHECK: ValueCheck = ValueCheck { value, _ ->
            when (value) {
                ORDINAL.keyword, EXACT.keyword -> null
                PLURAL.keyword, CARDINAL -> Problems.Values.DEFAULT_SELECT
                else -> Problems.Values.expectedOneOf(listOf(ORDINAL.keyword, EXACT.keyword))
            }
        }

        /** Returns the mode of the combined [options] of a selector; an invalid value selects by plural category. */
        fun of(options: Map<String, Operand>): SelectMode {
            val value = (options[OptionNames.SELECT] as? LiteralOperand)?.value
            return entries.firstOrNull { it.keyword == value } ?: PLURAL
        }
    }
}
