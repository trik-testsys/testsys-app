// MFFunctionRegistry and MessageFormatter are an ICU technology preview (@Deprecated @internal); only the package
// runtime touches the MF2 API.
@file:Suppress("DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime

import com.ibm.icu.message2.MFFunctionRegistry
import com.ibm.icu.message2.MessageFormatter
import com.ibm.icu.text.PluralRules
import com.ibm.icu.text.RuleBasedNumberFormat
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.InternalLocalizationApi
import tech.testsys.infra.localization.runtime.function.RuleSetFunction
import tech.testsys.infra.localization.runtime.function.TermFunction
import tech.testsys.infra.localization.runtime.function.UnitFunction

/** The custom functions `:term`, `:spellout`, `:ordinal` and `:unit` of one region. */
@InternalLocalizationApi
internal object RegionFunctions {

    /** Builds the function registry of the region [regionId] in [locale] with the formatters of its [terms]. */
    fun registry(regionId: String, locale: ULocale, terms: Map<String, MessageFormatter>): MFFunctionRegistry = MFFunctionRegistry.builder()
        .setFunction(TermFunction.NAME, TermFunction(regionId, terms, PluralRules.forLocale(locale)))
        .setFunction(RuleSetFunction.SPELLOUT, RuleSetFunction(RuleSetFunction.SPELLOUT, locale, RuleBasedNumberFormat.SPELLOUT))
        .setFunction(RuleSetFunction.ORDINAL, RuleSetFunction(RuleSetFunction.ORDINAL, locale, RuleBasedNumberFormat.ORDINAL))
        .setFunction(UnitFunction.NAME, UnitFunction(locale))
        .build()
}
