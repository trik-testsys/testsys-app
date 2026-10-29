// FunctionFactory and Function are an ICU technology preview (@Deprecated @internal); only the package runtime
// touches the MF2 API.
@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime.function

import com.ibm.icu.message2.Function
import com.ibm.icu.message2.FunctionFactory
import com.ibm.icu.number.LocalizedNumberFormatter
import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.InternalLocalizationApi
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * `:unit`: a number with a measurement unit in [regionLocale], formatted through the skeleton built by [UnitSkeleton].
 * [NAME] is the function name.
 */
@InternalLocalizationApi
internal class UnitFunction(private val regionLocale: ULocale) : FunctionFactory {
    private val formatters = ConcurrentHashMap<String, LocalizedNumberFormatter>()

    override fun create(locale: Locale, fixedOptions: Map<String, Any?>): Function = object : TextFunction() {
        override fun text(toFormat: Any?): String {
            val skeleton = UnitSkeleton.of(fixedOptions)
            val formatter = formatters.computeIfAbsent(skeleton) { NumberFormatter.forSkeleton(it).locale(regionLocale) }
            return formatter.format(formattable(operandValue(toFormat, fixedOptions))).toString()
        }
    }

    // LocalizedNumberFormatter formats only these number types, while the API passes any Number, such as a Short.
    private fun formattable(number: Number): Number = when (number) {
        is Int, is Long, is Float, is Double, is BigInteger, is BigDecimal -> number
        is Short, is Byte -> number.toLong()
        else -> BigDecimal(number.toString())
    }

    companion object {
        const val NAME = "unit"
    }
}
