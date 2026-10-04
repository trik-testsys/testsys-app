// These tests render through ICU's MF2 formatter, a technology preview (@Deprecated).
@file:Suppress("DEPRECATION")

package tech.testsys.infra.localization.codegen.mf2.icu

import com.ibm.icu.message2.MessageFormatter
import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.util.ULocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import tech.testsys.infra.localization.codegen.mf2.function.Functions
import java.time.Instant
import java.util.Date
import java.util.Locale

/**
 * Pins what ICU4J 78.1 honours: the codegen whitelist must equal the set of options whose "honoured" case passes,
 * and every option rejected as not honoured must still be ignored. When an ICU upgrade changes either, a test fails
 * and the whitelist can be revised.
 */
class OptionHonouredTests {

    /** A sample rendered with and without [option] of [function]; `ERROR` stands for a formatting failure. */
    data class Case(
        val function: String,
        val option: String,
        val with: String,
        val without: String,
        val expectedWith: String,
        val expectedWithout: String,
        val argument: Any = 2,
        val locale: String = "en",
    ) {
        override fun toString(): String = ":$function $option"
    }

    @ParameterizedTest
    @MethodSource("tech.testsys.infra.localization.codegen.mf2.icu.OptionHonouredTests#honoured")
    fun `should change the output if a whitelisted option is set`(case: Case) {
        assertEquals(
            listOf(case.expectedWith, case.expectedWithout),
            listOf(format(case.locale, case.with, case.argument), format(case.locale, case.without, case.argument)),
        )
    }

    @Test
    fun `should cover exactly the whitelisted options of the built-in functions`() {
        assertEquals(whitelist(), honoured().map { it.function to it.option }.toSet())
    }

    @ParameterizedTest
    @MethodSource("tech.testsys.infra.localization.codegen.mf2.icu.OptionHonouredTests#ignored")
    fun `should still ignore an option rejected as not honoured`(case: Case) {
        assertEquals(
            listOf(case.expectedWithout, case.expectedWithout),
            listOf(format(case.locale, case.with, case.argument), format(case.locale, case.without, case.argument)),
        )
    }

    @Test
    fun `should cover every option rejected as not honoured`() {
        val rejected = Functions.builtIns.flatMap { function -> IgnoredOptions.of(function).map { option -> function.name to option } }
            .toSet()

        assertEquals(rejected, (ignored() + unobservable + misapplied).map { it.function to it.option }.toSet())
    }

    @Test
    fun `should still format the percent operand again after icu skeleton`() {
        assertEquals("8,734%", format("en", "{\$x :percent icu:skeleton=|percent scale/100|}", 0.8734))
    }

    @Test
    fun `should still drop the currency if a currency has icu skeleton`() {
        assertEquals("5.0", format("en", "{\$x :currency currency=USD icu:skeleton=.0}", 5))
    }

    @Nested
    inner class SelectionTests {

        @ParameterizedTest
        @CsvSource("0.01, one", "0.02, few", "0.05, many", "1, many", "0.015, other")
        fun `should select a percent on the value multiplied by 100`(value: Double, expected: String) {
            val pattern = ".input {\$x :percent} .match \$x one {{one}} few {{few}} many {{many}} * {{other}}"

            assertEquals(expected, format("ru", pattern, value))
        }

        @Test
        fun `should never match the literal key other of a number selector (ICU4J 78_1)`() {
            assertEquals("fallback", format("ru", ".input {\$x :number} .match \$x one {{one}} other {{other}} * {{fallback}}", 1.5))
        }
    }

    @Nested
    inner class ArgumentOptionTests {

        @Test
        fun `should pass message arguments to number functions as options (ICU4J 78_1)`() {
            val formatter = formatter("en", "{\$x :number}")

            assertEquals("1.2M", formatter.formatToString(mapOf("x" to 1_234_567, "notation" to "compact")))
        }
    }

    @Nested
    inner class ReannotationTests {

        @Test
        fun `should format an offset value re-annotated with integer unshifted (ICU4J 78_1)`() {
            assertEquals("5", format("en", ".input {\$x :integer} .local \$o = {\$x :offset subtract=1} {{{\$o :integer}}}", 5))
        }

        @Test
        fun `should keep only the last shift of an offset of an offset (ICU4J 78_1)`() {
            val pattern = ".input {\$x :integer} .local \$a = {\$x :offset subtract=1} .local \$b = {\$a :offset subtract=1} {{{\$b}}}"

            assertEquals("4", format("en", pattern, 5))
        }

        @Test
        fun `should apply the declaration options to a built-in function over a value declared with another function (ICU4J 78_1)`() {
            assertEquals("+5", format("en", ".input {\$x :number signDisplay=always} {{{\$x :integer}}}", 5))
        }

        @Test
        fun `should drop number-only options of the value an offset shifts (ICU4J 78_1)`() {
            val pattern = ".input {\$x :number minimumFractionDigits=1} .local \$o = {\$x :offset subtract=1} {{{\$o}}}"

            assertEquals("1", format("en", pattern, 2))
        }
    }

    @Nested
    inner class CombinationTests {

        @Test
        fun `should ignore compactDisplay without compact notation (ICU4J 78_1)`() {
            assertEquals("1,234,567", format("en", "{\$x :number compactDisplay=long}", 1_234_567))
        }

        @Test
        fun `should apply only the last precision option (ICU4J 78_1)`() {
            assertEquals("1.5", format("en", "{\$x :number minimumFractionDigits=2 maximumFractionDigits=3}", 1.5))
        }

        @Test
        fun `should ignore hour12 without a time precision (ICU4J 78_1)`() {
            assertEquals("8:53${narrow}AM", format("en", "{\$x :time hour12=false}", date))
        }

        @Test
        fun `should render only the time zone for timeZoneStyle without a time precision (ICU4J 78_1)`() {
            assertEquals("UTC", format("en", "{\$x :time timeZoneStyle=short}", date))
        }

        @Test
        fun `should render the date with the zone for timeZoneStyle of datetime with a date and without a time precision (ICU4J 78_1)`() {
            val pattern = "{\$x :datetime dateFields=year-month-day dateLength=long timeZoneStyle=short}"

            assertEquals("October 9, 2025 at UTC", format("en", pattern, date))
        }

        @Test
        fun `should ignore number options next to icu skeleton (ICU4J 78_1)`() {
            assertEquals("5.00", format("en", "{\$x :integer signDisplay=always icu:skeleton=.00}", 5))
        }

        @Test
        fun `should ignore icu skeleton next to date fields and length (ICU4J 78_1)`() {
            assertEquals("October 9", format("en", "{\$x :date fields=month-day length=long icu:skeleton=yMMMM}", date))
        }

        @Test
        fun `should fall back to the default usage if the unit category does not define the usage (ICU4J 78_1)`() {
            val default = NumberFormatter.forSkeleton("unit/meter usage/default").locale(ULocale.US).format(5).toString()

            val banana = NumberFormatter.forSkeleton("unit/meter usage/banana").locale(ULocale.US).format(5).toString()

            assertEquals(default, banana)
        }
    }

    companion object {
        private val date: Date = Date.from(Instant.parse("2025-10-09T08:53:20Z"))
        private val narrow = Char(0x202F).toString()

        // Rejected options that ICU does not ignore but applies wrongly; pinned by dedicated tests above.
        private val misapplied = listOf(
            Case("percent", "icu:skeleton", "", "", "", ""),
            Case("currency", "icu:skeleton", "", "", "", ""),
        )

        // Rejected options whose effect no output can show, so no sample could fail if ICU applied them: `:percent`
        // ignores its fraction digits, which leaves trailingZeroDisplay no zeros to strip; `:currency` ignores notation,
        // which leaves compactDisplay nothing to style; `:time` shows no calendar field, so the calendar it applies
        // never changes the output.
        private val unobservable = listOf(
            Case("percent", "trailingZeroDisplay", "", "", "", ""),
            Case("currency", "compactDisplay", "", "", "", ""),
            Case("time", "calendar", "", "", "", ""),
        )

        private fun formatter(locale: String, pattern: String): MessageFormatter = MessageFormatter.builder()
            .setLocale(Locale.forLanguageTag(locale))
            .setPattern(pattern)
            .setErrorHandlingBehavior(MessageFormatter.ErrorHandlingBehavior.STRICT)
            .build()

        private fun format(locale: String, pattern: String, argument: Any): String =
            runCatching { formatter(locale, pattern).formatToString(mapOf("x" to argument)) }.getOrDefault("ERROR")

        private fun whitelist(): Set<Pair<String, String>> =
            Functions.builtIns.flatMap { function -> IgnoredOptions.honouredOptions(function).map { option -> function.name to option } }
                .toSet()

        private fun number(
            function: String,
            option: String,
            value: String,
            argument: Any,
            with: String,
            without: String,
            extra: String = "",
        ) = Case(
            function,
            option,
            "{\$x :$function $extra$option=$value}",
            "{\$x :$function $extra}".replace(" }", "}"),
            with,
            without,
            argument,
        )

        private fun ordinal(function: String, argument: Any) = Case(
            function,
            "select",
            ".input {\$x :$function select=ordinal} .match \$x one {{st}} two {{nd}} few {{rd}} * {{th}}",
            ".input {\$x :$function} .match \$x one {{st}} two {{nd}} few {{rd}} * {{th}}",
            "nd",
            "th",
            argument,
        )

        private fun dateCase(
            function: String,
            option: String,
            with: String,
            without: String,
            expectedWith: String,
            expectedWithout: String,
        ) = Case(
            function,
            option,
            "{\$x :$function $with}",
            "{\$x :$function $without}".replace(" }", "}"),
            expectedWith,
            expectedWithout,
            date,
        )

        @JvmStatic
        fun honoured(): List<Case> = listOf(
            ordinal("integer", 2),
            number("integer", "numberingSystem", "fullwide", 12, "１２", "12"),
            number("integer", "signDisplay", "always", 5, "+5", "5"),
            number("integer", "useGrouping", "never", 12_345, "12345", "12,345"),
            number("integer", "icu:skeleton", ".00", 5, "5.00", "5"),
            ordinal("number", 2),
            number("number", "notation", "scientific", 12_345, "1.2345E4", "12,345"),
            number("number", "compactDisplay", "long", 1_234_567, "1.2 million", "1.2M", extra = "notation=compact "),
            number("number", "numberingSystem", "fullwide", 12, "１２", "12"),
            number("number", "signDisplay", "always", 5, "+5", "5"),
            number("number", "useGrouping", "never", 12_345, "12345", "12,345"),
            number("number", "minimumFractionDigits", "2", 1.5, "1.50", "1.5"),
            number("number", "maximumFractionDigits", "1", 1.26, "1.3", "1.26"),
            number("number", "minimumSignificantDigits", "4", 1.5, "1.500", "1.5"),
            number("number", "maximumSignificantDigits", "2", 1_234, "1,200", "1,234"),
            number("number", "icu:skeleton", ".00", 1.5, "1.50", "1.5"),
            ordinal("percent", 0.02),
            number("percent", "numberingSystem", "fullwide", 0.12, "１２%", "12%"),
            number("percent", "signDisplay", "always", 0.5, "+50%", "50%"),
            number("percent", "useGrouping", "never", 123.45, "12345%", "12,345%"),
            number("percent", "maximumSignificantDigits", "1", 0.8734, "90%", "87.34%"),
            Case("currency", "currency", "{\$x :currency currency=EUR}", "{\$x :currency}", "€5.00", "ERROR", 5),
            number("currency", "currencyDisplay", "name", 5, "5.00 US dollars", "$5.00", extra = "currency=USD "),
            number("currency", "numberingSystem", "fullwide", 12, "$１２.００", "$12.00", extra = "currency=USD "),
            number("currency", "signDisplay", "always", 5, "+$5.00", "$5.00", extra = "currency=USD "),
            number("currency", "useGrouping", "never", 12_345, "$12345.00", "$12,345.00", extra = "currency=USD "),
            number("currency", "maximumSignificantDigits", "1", 1_234, "$1,000", "$1,234.00", extra = "currency=USD "),
            Case("offset", "add", "{\$x :offset add=2}", "{\$x :offset}", "7", "ERROR", 5),
            Case("offset", "subtract", "{\$x :offset subtract=2}", "{\$x :offset}", "3", "ERROR", 5),
            dateCase("date", "fields", "fields=month-day length=long", "length=long", "October 9", "Thu, Oct 9, 2025"),
            dateCase("date", "length", "fields=month-day length=long", "fields=month-day", "October 9", "Thu, Oct 9, 2025"),
            dateCase(
                "date",
                "calendar",
                "fields=year-month-day length=long calendar=buddhist",
                "fields=year-month-day length=long",
                "October 9, 2568 BE",
                "October 9, 2025",
            ),
            dateCase("date", "timeZone", "timeZone=|Pacific/Pago_Pago|", "", "Wed, Oct 8, 2025", "Thu, Oct 9, 2025"),
            dateCase("date", "icu:skeleton", "icu:skeleton=yMMMM", "", "October 2025", "Thu, Oct 9, 2025"),
            dateCase("time", "precision", "precision=second", "", "8:53:20${narrow}AM", "8:53${narrow}AM"),
            dateCase("time", "hour12", "precision=minute hour12=false", "precision=minute", "08:53", "8:53${narrow}AM"),
            dateCase(
                "time",
                "timeZoneStyle",
                "precision=minute timeZoneStyle=short",
                "precision=minute",
                "8:53${narrow}AM UTC",
                "8:53${narrow}AM",
            ),
            dateCase("time", "timeZone", "timeZone=|Asia/Tokyo|", "", "5:53${narrow}PM", "8:53${narrow}AM"),
            dateCase("time", "icu:skeleton", "icu:skeleton=Hms", "", "08:53:20", "8:53${narrow}AM"),
            dateCase(
                "datetime",
                "dateFields",
                "dateFields=month-day dateLength=long",
                "dateLength=long",
                "October 9",
                "Thu, Oct 9, 2025, 8:53${narrow}AM",
            ),
            dateCase(
                "datetime",
                "dateLength",
                "dateFields=month-day dateLength=long",
                "dateFields=month-day",
                "October 9",
                "Thu, Oct 9, 2025, 8:53${narrow}AM",
            ),
            dateCase("datetime", "timePrecision", "timePrecision=second", "", "8:53:20${narrow}AM", "Thu, Oct 9, 2025, 8:53${narrow}AM"),
            dateCase("datetime", "hour12", "timePrecision=minute hour12=false", "timePrecision=minute", "08:53", "8:53${narrow}AM"),
            dateCase(
                "datetime",
                "timeZoneStyle",
                "timePrecision=minute timeZoneStyle=short",
                "timePrecision=minute",
                "8:53${narrow}AM UTC",
                "8:53${narrow}AM",
            ),
            dateCase(
                "datetime",
                "calendar",
                "dateFields=year-month-day dateLength=long calendar=buddhist",
                "dateFields=year-month-day dateLength=long",
                "October 9, 2568 BE",
                "October 9, 2025",
            ),
            dateCase(
                "datetime",
                "timeZone",
                "timeZone=|Asia/Tokyo|",
                "",
                "Thu, Oct 9, 2025, 5:53${narrow}PM",
                "Thu, Oct 9, 2025, 8:53${narrow}AM",
            ),
            dateCase("datetime", "icu:skeleton", "icu:skeleton=yMMMM", "", "October 2025", "Thu, Oct 9, 2025, 8:53${narrow}AM"),
        )

        // A sample of an ignored option: rendered with and without the option, it gives [expected] both times.
        private fun same(function: String, option: String, value: String, argument: Any, expected: String, extra: String = "") =
            number(function, option, value, argument, expected, expected, extra)

        // Options that ICU ignores entirely; each sample would change if ICU applied the option.
        @JvmStatic
        fun ignored(): List<Case> = listOf(
            same("integer", "minimumIntegerDigits", "3", 5, "5"),
            same("integer", "maximumSignificantDigits", "1", 1_234, "1,234"),
            same("number", "minimumIntegerDigits", "3", 5, "5"),
            same("number", "roundingPriority", "morePrecision", 1.23456, "1.2", "maximumFractionDigits=4 maximumSignificantDigits=2 "),
            same("number", "roundingIncrement", "5", 1.23, "1.2", "maximumFractionDigits=1 "),
            same("number", "roundingMode", "floor", 1.6, "2", "maximumFractionDigits=0 "),
            same("number", "trailingZeroDisplay", "stripIfInteger", 5, "5.00", "minimumFractionDigits=2 "),
            same("number", "style", "percent", 0.5, "0.5"),
            same("percent", "minimumIntegerDigits", "4", 0.05, "5%"),
            same("percent", "minimumFractionDigits", "2", 0.5, "50%"),
            same("percent", "maximumFractionDigits", "0", 0.8734, "87.34%"),
            same("percent", "minimumSignificantDigits", "4", 0.5, "50%"),
            same("percent", "roundingPriority", "lessPrecision", 0.8734, "87.3%", "maximumSignificantDigits=3 "),
            same("percent", "roundingIncrement", "5", 0.8734, "87.34%"),
            same("percent", "roundingMode", "floor", 0.8734, "87.34%"),
            same("percent", "notation", "compact", 12_345.0, "1,234,500%"),
            same("percent", "compactDisplay", "long", 12_345.0, "1,234,500%", "notation=compact "),
            same("currency", "currencySign", "accounting", -5, "-$5.00", "currency=USD "),
            same("currency", "notation", "compact", 1_234_567, "$1,234,567.00", "currency=USD "),
            same("currency", "minimumIntegerDigits", "3", 5, "$5.00", "currency=USD "),
            same("currency", "fractionDigits", "0", 5.5, "$5.50", "currency=USD "),
            same("currency", "minimumFractionDigits", "3", 5, "$5.00", "currency=USD "),
            same("currency", "maximumFractionDigits", "0", 5.5, "$5.50", "currency=USD "),
            same("currency", "minimumSignificantDigits", "4", 5, "$5.00", "currency=USD "),
            same("currency", "roundingPriority", "morePrecision", 5.555, "$5.6", "currency=USD maximumSignificantDigits=2 "),
            same("currency", "roundingIncrement", "5", 5.57, "$5.57", "currency=USD "),
            same("currency", "roundingMode", "floor", 5.555, "$5.56", "currency=USD "),
            same("currency", "trailingZeroDisplay", "stripIfInteger", 5, "$5.00", "currency=USD "),
        )
    }
}
