// Function and FormattedPlaceholder are an ICU technology preview (@Deprecated @internal); only the package runtime
// touches the MF2 API.
@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime.function

import com.ibm.icu.message2.FormattedPlaceholder
import com.ibm.icu.message2.Function
import tech.testsys.infra.localization.InternalLocalizationApi

/** A formatting-only custom function: [text] renders the operand; the function cannot be a selector. */
@InternalLocalizationApi
internal abstract class TextFunction : Function {
    /** Renders [toFormat]. */
    abstract fun text(toFormat: Any?): String

    override fun formatToString(toFormat: Any?, variableOptions: Map<String, Any?>): String = text(toFormat)

    override fun format(toFormat: Any?, variableOptions: Map<String, Any?>): FormattedPlaceholder = placeholder(toFormat, text(toFormat))
}
