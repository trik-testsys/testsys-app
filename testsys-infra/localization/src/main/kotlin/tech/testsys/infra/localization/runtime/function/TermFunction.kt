// FunctionFactory, Function and MessageFormatter are an ICU technology preview (@Deprecated @internal); only the
// package runtime touches the MF2 API.
@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime.function

import com.ibm.icu.message2.Function
import com.ibm.icu.message2.FunctionFactory
import com.ibm.icu.message2.MessageFormatter
import com.ibm.icu.text.PluralRules
import tech.testsys.infra.localization.InternalLocalizationApi
import java.util.Locale

/**
 * `:term`: a glossary term of the region [regionId] from its [terms] in the grammatical form chosen by the `case`
 * option and by the number, through [pluralRules], or `number=sg|pl`. [NAME] is the function name.
 */
@InternalLocalizationApi
internal class TermFunction(
    private val regionId: String,
    private val terms: Map<String, MessageFormatter>,
    private val pluralRules: PluralRules,
) : FunctionFactory {

    override fun create(locale: Locale, fixedOptions: Map<String, Any?>): Function = object : TextFunction() {
        override fun text(toFormat: Any?): String {
            val name = requireNotNull(fixedOptions.text(NAME_OPTION)) { "':$NAME' needs the option '$NAME_OPTION'" }
            val form = if (toFormat == null) {
                requireNotNull(fixedOptions.text(NUMBER)) { "':$NAME' without an operand needs the option '$NUMBER'" }
            } else {
                pluralRules.select(operandValue(toFormat, fixedOptions).toDouble())
            }
            val term = requireNotNull(terms[name]) { "Region '$regionId' has no term '$name'" }
            return term.formatToString(mapOf(CASE to fixedOptions.text(CASE), FORM to form))
        }
    }

    companion object {
        const val NAME = "term"

        private const val NAME_OPTION = "name"
        private const val NUMBER = "number"

        // The option `case` and the input variables `$case` and `$form` of a term message.
        private const val CASE = "case"
        private const val FORM = "form"
    }
}
