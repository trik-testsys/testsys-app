package tech.testsys.infra.localization.runtime.function

import com.ibm.icu.util.MeasureUnit
import tech.testsys.infra.localization.InternalLocalizationApi
import java.math.BigDecimal

/**
 * Option-to-skeleton mapping of the custom `:unit` function: each of its [options], the options of the MF2 `:unit`
 * function, becomes a stem of an ICU number skeleton.
 */
@InternalLocalizationApi
internal object UnitSkeleton {
    private const val UNIT = "unit"
    private const val USAGE = "usage"
    private const val UNIT_DISPLAY = "unitDisplay"
    private const val NOTATION = "notation"
    private const val COMPACT_DISPLAY = "compactDisplay"
    private const val NUMBERING_SYSTEM = "numberingSystem"
    private const val SIGN_DISPLAY = "signDisplay"
    private const val USE_GROUPING = "useGrouping"
    private const val MINIMUM_INTEGER_DIGITS = "minimumIntegerDigits"
    private const val MINIMUM_FRACTION_DIGITS = "minimumFractionDigits"
    private const val MAXIMUM_FRACTION_DIGITS = "maximumFractionDigits"
    private const val MINIMUM_SIGNIFICANT_DIGITS = "minimumSignificantDigits"
    private const val MAXIMUM_SIGNIFICANT_DIGITS = "maximumSignificantDigits"
    private const val ROUNDING_PRIORITY = "roundingPriority"
    private const val ROUNDING_INCREMENT = "roundingIncrement"
    private const val ROUNDING_MODE = "roundingMode"
    private const val TRAILING_ZERO_DISPLAY = "trailingZeroDisplay"

    private const val DEFAULT_MAX_FRACTION_DIGITS = 3
    private const val DEFAULT_MAX_SIGNIFICANT_DIGITS = 21

    val options: Set<String> = setOf(
        UNIT, USAGE, UNIT_DISPLAY, NOTATION, COMPACT_DISPLAY, NUMBERING_SYSTEM, SIGN_DISPLAY, USE_GROUPING,
        MINIMUM_INTEGER_DIGITS, MINIMUM_FRACTION_DIGITS, MAXIMUM_FRACTION_DIGITS, MINIMUM_SIGNIFICANT_DIGITS,
        MAXIMUM_SIGNIFICANT_DIGITS, ROUNDING_PRIORITY, ROUNDING_INCREMENT, ROUNDING_MODE, TRAILING_ZERO_DISPLAY,
    )

    private val unitWidths = mapOf(
        "short" to "unit-width-short",
        "narrow" to "unit-width-narrow",
        "long" to "unit-width-full-name",
    )
    private val notations = mapOf(
        "scientific" to "scientific",
        "engineering" to "engineering",
    )
    private val signDisplays = mapOf(
        "auto" to "sign-auto",
        "always" to "sign-always",
        "exceptZero" to "sign-except-zero",
        "negative" to "sign-negative",
        "never" to "sign-never",
    )
    private val groupings = mapOf(
        "auto" to "group-auto",
        "always" to "group-on-aligned",
        "never" to "group-off",
        "min2" to "group-min2",
    )
    private val roundingModes = mapOf(
        "ceil" to "rounding-mode-ceiling",
        "floor" to "rounding-mode-floor",
        "expand" to "rounding-mode-up",
        "trunc" to "rounding-mode-down",
        "halfExpand" to "rounding-mode-half-up",
        "halfTrunc" to "rounding-mode-half-down",
        "halfEven" to "rounding-mode-half-even",
    )

    /** Builds the number skeleton for the resolved [options] of a `:unit` expression. */
    fun of(options: Map<String, Any?>): String {
        val stems = listOfNotNull(
            "unit/" + unitIdentifier(requireNotNull(options.text(UNIT)) { "':$UNIT' needs the option '$UNIT'" }),
            options.text(USAGE)?.let { "usage/$it" },
            options.text(UNIT_DISPLAY)?.let { stem(unitWidths, UNIT_DISPLAY, it) },
            notation(options.text(NOTATION), options.text(COMPACT_DISPLAY)),
            options.text(NUMBERING_SYSTEM)?.let { "numbering-system/$it" },
            options.text(SIGN_DISPLAY)?.let { stem(signDisplays, SIGN_DISPLAY, it) },
            options.text(USE_GROUPING)?.let { stem(groupings, USE_GROUPING, it) },
            integer(options, MINIMUM_INTEGER_DIGITS)?.let { "integer-width/*" + "0".repeat(it) },
            precision(options),
            options.text(ROUNDING_MODE)?.let { stem(roundingModes, ROUNDING_MODE, it) },
        )
        return stems.joinToString(" ")
    }

    // The build checks a literal unit, but the value of `unit=$u` is a message argument glued into the skeleton: a space
    // or a `/` in it would add stems or stem options, so it must be exactly one ICU unit identifier.
    private fun unitIdentifier(value: String): String {
        try {
            MeasureUnit.forIdentifier(value)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("':$UNIT' option '$UNIT' has value '$value', expected one ICU unit identifier", e)
        }
        return value
    }

    private fun notation(notation: String?, compactDisplay: String?): String? = when (notation) {
        "compact" -> if (compactDisplay == "long") "compact-long" else "compact-short"
        null, "standard" -> null
        else -> stem(notations, NOTATION, notation)
    }

    // The build accepts only the values of the tables; any other value is a defect of the build checks.
    private fun stem(table: Map<String, String>, option: String, value: String): String =
        requireNotNull(table[value]) { "':$UNIT' option '$option' has unsupported value '$value'" }

    private fun precision(options: Map<String, Any?>): String? {
        val stem = incrementStem(options) ?: digitsStem(options)
        val shouldStripZeros = options.text(TRAILING_ZERO_DISPLAY) == "stripIfInteger"
        return if (stem != null && shouldStripZeros) "$stem/w" else stem
    }

    private fun incrementStem(options: Map<String, Any?>): String? {
        val increment = integer(options, ROUNDING_INCREMENT)?.takeIf { it != 1 } ?: return null
        val scale = integer(options, MAXIMUM_FRACTION_DIGITS) ?: 0
        return "precision-increment/" + BigDecimal(increment).movePointLeft(scale).toPlainString()
    }

    private fun digitsStem(options: Map<String, Any?>): String? {
        val minFraction = integer(options, MINIMUM_FRACTION_DIGITS)
        val maxFraction = integer(options, MAXIMUM_FRACTION_DIGITS)
        val minSignificant = integer(options, MINIMUM_SIGNIFICANT_DIGITS)
        val maxSignificant = integer(options, MAXIMUM_SIGNIFICANT_DIGITS)
        val fraction = fractionStem(minFraction, maxFraction)
        val significant = significantStem(minSignificant, maxSignificant)
        if (fraction == null || significant == null) return significant ?: fraction
        return when (options.text(ROUNDING_PRIORITY)) {
            "morePrecision" -> "$fraction/${significant}r"
            "lessPrecision" -> "$fraction/${significant}s"
            else -> significant
        }
    }

    private fun fractionStem(min: Int?, max: Int?): String? {
        if (min == null && max == null) return null
        val minimum = min ?: 0
        val maximum = max ?: maxOf(minimum, DEFAULT_MAX_FRACTION_DIGITS)
        return if (maximum == 0) "precision-integer" else "." + "0".repeat(minimum) + "#".repeat(maximum - minimum)
    }

    private fun significantStem(min: Int?, max: Int?): String? {
        if (min == null && max == null) return null
        val minimum = min ?: 1
        val maximum = max ?: DEFAULT_MAX_SIGNIFICANT_DIGITS
        return "@".repeat(minimum) + "#".repeat(maximum - minimum)
    }

    private fun integer(options: Map<String, Any?>, name: String): Int? = when (val value = options[name]) {
        null -> null
        is Number -> value.toInt()
        else -> value.toString().toInt()
    }
}
