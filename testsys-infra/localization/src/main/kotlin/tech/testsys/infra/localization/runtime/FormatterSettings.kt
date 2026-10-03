// MessageFormatter and MFFunctionRegistry are an ICU technology preview (@Deprecated @internal); only the package
// runtime touches the MF2 API.
@file:Suppress("DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime

import com.ibm.icu.message2.MFFunctionRegistry
import com.ibm.icu.message2.MessageFormatter
import tech.testsys.infra.localization.InternalLocalizationApi
import java.util.Locale

/** The settings of every ICU `MessageFormatter` of the runtime. */
@InternalLocalizationApi
internal object FormatterSettings {
    /** Builds the formatter of [pattern] in [locale] with the custom functions of [registry], if any. */
    fun newFormatter(locale: Locale, pattern: String, registry: MFFunctionRegistry?): MessageFormatter {
        val builder = MessageFormatter.builder()
            .setLocale(locale)
            .setPattern(pattern)
            // Build-time validation makes runtime errors unexpected, so STRICT fails fast.
            .setErrorHandlingBehavior(MessageFormatter.ErrorHandlingBehavior.STRICT)
            // The API returns plain strings; keep ICU's default isolation explicit.
            .setBidiIsolation(MessageFormatter.BidiIsolation.NONE)
        registry?.let(builder::setFunctionRegistry)
        return builder.build()
    }
}
