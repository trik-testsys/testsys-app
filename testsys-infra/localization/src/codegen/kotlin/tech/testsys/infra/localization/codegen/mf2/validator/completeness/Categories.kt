package tech.testsys.infra.localization.codegen.mf2.validator.completeness

import com.ibm.icu.number.FormattedNumber
import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.number.Precision
import com.ibm.icu.text.PluralRules
import com.ibm.icu.util.ULocale

private const val DECIMAL_PROBE_LIMIT = 1_000
private const val TENTHS = 10.0
private const val HUNDREDTHS = 100.0

private val decimalProbes: List<FormattedNumber> by lazy {
    val oneDigit = NumberFormatter.withLocale(ULocale.ROOT).precision(Precision.fixedFraction(1))
    val twoDigits = NumberFormatter.withLocale(ULocale.ROOT).precision(Precision.fixedFraction(2))
    (0 until DECIMAL_PROBE_LIMIT).flatMap { step -> listOf(oneDigit.format(step / TENTHS), twoDigits.format(step / HUNDREDTHS)) }
}

/** Categories of [rules] that some integer selects, from ICU's integer samples. */
internal fun integerCategories(rules: PluralRules): Set<String> = rules.keywords.filter { !rules.getSamples(it).isNullOrEmpty() }.toSet()

/**
 * Categories of [rules] that a number with visible fraction digits selects. ICU has no stable API for decimal
 * samples, so formatted numbers with one and two fraction digits are probed.
 */
internal fun decimalCategories(rules: PluralRules): Set<String> = decimalProbes.map(rules::select).toSet()
