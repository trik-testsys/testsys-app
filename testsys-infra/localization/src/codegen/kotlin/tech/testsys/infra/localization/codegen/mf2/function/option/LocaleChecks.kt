package tech.testsys.infra.localization.codegen.mf2.function.option

import com.ibm.icu.text.NumberingSystem
import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems

/** The value is the lowercase name of a decimal, non-algorithmic ICU numbering system, such as `latn` or `arab`. */
internal object NumberingSystemCheck : ValueCheck {
    // ICU number formatting falls back to latn for an algorithmic system such as roman: it would render 12, not XII.
    private const val DECIMAL_RADIX = 10

    override fun findProblem(value: String, locale: ULocale): String? {
        val system = value.takeIf { it == it.lowercase() }?.let(NumberingSystem::getInstanceByName)
        val isDecimal = system != null && !system.isAlgorithmic && system.radix == DECIMAL_RADIX
        return if (isDecimal) null else Problems.Values.NUMBERING_SYSTEM
    }
}

/** The value is a BCP 47 calendar type that ICU knows, such as `gregory` or `buddhist`. */
internal object CalendarCheck : ValueCheck {
    // ICU MF2 puts the value into the Unicode locale keyword `ca`, which takes BCP 47 types: `gregory`, not the ICU
    // calendar id `gregorian`.
    private const val CALENDAR_KEYWORD = "calendar"
    private const val CALENDAR_LOCALE_KEY = "ca"

    private val calendars: Set<String> by lazy {
        Calendar.getKeywordValuesForLocale(CALENDAR_KEYWORD, ULocale.ROOT, false)
            .mapNotNull { id -> ULocale.toUnicodeLocaleType(CALENDAR_LOCALE_KEY, id) }
            .toSet()
    }

    override fun findProblem(value: String, locale: ULocale): String? = if (value in calendars) null else Problems.Values.CALENDAR
}

/** The value is the name, without `%`, of an RBNF rule set of the [kind] (`RuleBasedNumberFormat.SPELLOUT`, …). */
internal class RuleSetCheck(private val kind: Int) : ValueCheck {
    override fun findProblem(value: String, locale: ULocale): String? {
        val ruleSets = RuleBasedNumberFormat(locale, kind).ruleSetNames.map { it.removePrefix(RULE_SET_PREFIX) }
        val isKnown = !value.startsWith(RULE_SET_PREFIX) && value in ruleSets
        return if (isKnown) null else Problems.Values.ruleSet(locale.toLanguageTag())
    }

    private companion object {
        const val RULE_SET_PREFIX = "%"
    }
}
