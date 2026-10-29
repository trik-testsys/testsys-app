package tech.testsys.infra.localization.codegen.mf2.function.option

import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.util.Currency
import com.ibm.icu.util.MeasureUnit
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems
import java.time.ZoneId

/** The value is `input` or a region-based zone id, such as `UTC` or `Europe/Moscow`, but not an offset such as `+03:00`. */
internal object ZoneIdCheck : ValueCheck {
    // ICU knows every region-based zone id of the JDK, but shows an offset id it cannot parse, such as +03:00, in GMT.
    private val zoneIds: Set<String> = ZoneId.getAvailableZoneIds()

    override fun findProblem(value: String, locale: ULocale): String? =
        if (value == OptionNames.INPUT || value in zoneIds) null else Problems.Values.ZONE
}

/** The value is an ISO 4217 currency code that ICU knows. */
internal object CurrencyCodeCheck : ValueCheck {
    private val shape = Regex("[A-Z]{3}")

    override fun findProblem(value: String, locale: ULocale): String? {
        val isKnown = shape.matches(value) && Currency.isAvailable(value, null, null)
        return if (isKnown) null else Problems.Values.CURRENCY_CODE
    }
}

/** The value is an ICU unit identifier, such as `megabyte` or `kilometer-per-hour`. */
internal object UnitIdentifierCheck : ValueCheck {
    private const val UNIT_STEM = "unit/"

    override fun findProblem(value: String, locale: ULocale): String? = if (isUnit(value)) null else Problems.Values.UNIT

    // Both reject a unit with an IllegalArgumentException; SkeletonSyntaxException extends it.

    /** Returns whether ICU accepts [value] as a unit of a number skeleton. */
    fun isUnit(value: String): Boolean = try {
        MeasureUnit.forIdentifier(value)
        NumberFormatter.forSkeleton(UNIT_STEM + value)
        true
    } catch (ignored: IllegalArgumentException) {
        false
    }
}
