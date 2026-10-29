// FormattedPlaceholder and PlainStringFormattedValue are an ICU technology preview (@Deprecated @internal); only the
// package runtime touches the MF2 API.
@file:Suppress("DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime.function

import com.ibm.icu.message2.FormattedPlaceholder
import com.ibm.icu.message2.PlainStringFormattedValue
import tech.testsys.infra.localization.InternalLocalizationApi
import java.math.BigDecimal

private const val ADD = "add"
private const val SUBTRACT = "subtract"

// ICU passes every message argument as "variableOptions" and lets an argument `$name` shadow the option `name=`,
// so custom functions read their options only from the fixed options, which hold literal and resolved values.

/** Returns the fixed option [name] as text, or `null` if it is not set. */
@InternalLocalizationApi
internal fun Map<String, Any?>.text(name: String): String? = this[name]?.toString()

/**
 * Returns the number [toFormat] stands for. An operand declared as `.local $x = {$n :offset subtract=1}` arrives
 * unshifted, with `subtract` among the inherited [fixedOptions]; finite numbers are shifted exactly, while
 * non-finite numbers retain the ICU-supported NaN or infinity value.
 */
@InternalLocalizationApi
internal fun operandValue(toFormat: Any?, fixedOptions: Map<String, Any?>): Number {
    val number = numberOf((toFormat as? FormattedPlaceholder)?.input ?: toFormat)
    val shift = (fixedOptions.text(ADD)?.toInt()?.toLong() ?: 0L) - (fixedOptions.text(SUBTRACT)?.toInt()?.toLong() ?: 0L)
    return when {
        shift == 0L || !isFinite(number) -> number
        else -> BigDecimal(number.toString()) + BigDecimal.valueOf(shift)
    }
}

/** Returns whether [number] is neither NaN nor infinity, without narrowing decimal or integer values to Double. */
@InternalLocalizationApi
internal fun isFinite(number: Number): Boolean = when (number) {
    is Double -> number.isFinite()
    is Float -> number.isFinite()
    else -> true
}

/** Returns the placeholder of [input] rendered as [text]. */
@InternalLocalizationApi
internal fun placeholder(input: Any?, text: String): FormattedPlaceholder = FormattedPlaceholder(input, PlainStringFormattedValue(text))

private fun numberOf(input: Any?): Number = when (input) {
    is Number -> input
    is CharSequence -> BigDecimal(input.toString())
    else -> throw IllegalArgumentException("Expected a number operand, got ${input?.javaClass?.name}")
}
