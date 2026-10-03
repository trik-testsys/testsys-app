package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.INTEGER_PLURAL_VARIANTS
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class OptionsRuleTests {

    @Test
    fun `should reject u options`() {
        assertEquals(
            listOf(
                "RU / task.a: option 'u:dir' is not supported: u: options have no effect on plain-text output with bidi " +
                    "isolation NONE",
            ),
            ruErrors("task.a={\$x :string u:dir=rtl}"),
        )
    }

    @Test
    fun `should reject an unknown option`() {
        assertEquals(listOf("RU / task.a: unknown option 'foo' of ':number'"), ruErrors("task.a={\$x :number foo=bar}"))
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '#',
        value = [
            "{\$x :number notation=fancy}#invalid value 'fancy' of option 'notation' of ':number': expected one of: " +
                "standard, scientific, engineering, compact",
            "{\$x :number maximumFractionDigits=01}#invalid value '01' of option 'maximumFractionDigits' of ':number': " +
                "expected an integer in 0..999",
            "{\$x :number maximumSignificantDigits=0}#invalid value '0' of option 'maximumSignificantDigits' of " +
                "':number': expected an integer in 1..999",
            "{\$x :currency currency=XYZ}#invalid value 'XYZ' of option 'currency' of ':currency': expected an ISO 4217 code",
            "{\$x :number numberingSystem=klingon}#invalid value 'klingon' of option 'numberingSystem' of ':number': not " +
                "a decimal ICU numbering system",
            "{\$x :number numberingSystem=roman}#invalid value 'roman' of option 'numberingSystem' of ':number': not " +
                "a decimal ICU numbering system",
            "{\$d :datetime timeZone=Mars}#invalid value 'Mars' of option 'timeZone' of ':datetime': expected 'input', " +
                "'UTC' or an IANA zone id such as |Europe/Moscow|",
            "{\$d :time timeZone=|+03:00|}#invalid value '+03:00' of option 'timeZone' of ':time': expected 'input', " +
                "'UTC' or an IANA zone id such as |Europe/Moscow|",
            "{\$d :time timeZone=|UTC+03:00|}#invalid value 'UTC+03:00' of option 'timeZone' of ':time': expected " +
                "'input', 'UTC' or an IANA zone id such as |Europe/Moscow|",
            "{\$d :date fields=year-month-day length=long calendar=martian}#invalid value 'martian' of option 'calendar' " +
                "of ':date': expected a BCP 47 calendar type such as gregory or buddhist",
            "{\$d :date fields=year-month-day length=long calendar=gregorian}#invalid value 'gregorian' of option " +
                "'calendar' of ':date': expected a BCP 47 calendar type such as gregory or buddhist",
        ],
    )
    fun `should reject an invalid literal option value`(message: String, problem: String) {
        assertEquals(listOf("RU / task.a: $problem"), ruErrors("task.a=$message"))
    }

    @Test
    fun `should reject an unknown RBNF rule set`() {
        assertEquals(
            listOf("RU / task.a: invalid value 'spellout-banana' of option 'rules' of ':spellout': ICU has no such rule set for ru-RU"),
            ruErrors("task.a={\$n :spellout rules=spellout-banana}"),
        )
    }

    @Test
    fun `should reject a bad unit`() {
        assertEquals(
            listOf("RU / task.a: invalid value 'parsec-per-banana' of option 'unit' of ':unit': not an ICU unit identifier"),
            ruErrors("task.a={\$m :unit unit=parsec-per-banana}"),
        )
    }

    @Test
    fun `should reject an invalid number skeleton`() {
        val errors = ruErrors("task.a={\$n :number icu:skeleton=banana}")

        assertEquals(1, errors.size)
        assertEquals(
            true,
            errors.single().startsWith(
                "RU / task.a: invalid value 'banana' of option 'icu:skeleton' of ':number': ICU rejects the number skeleton: ",
            ),
        )
    }

    @Test
    fun `should reject a garbage date skeleton`() {
        assertEquals(
            listOf(
                "RU / task.a: invalid value 'yMMMMMMMd' of option 'icu:skeleton' of ':date': the field 'M' cannot be 7 letters long " +
                    "(allowed: 1, 2, 3, 4, 5)",
            ),
            ruErrors("task.a={\$d :date icu:skeleton=yMMMMMMMd}"),
        )
    }

    @Test
    fun `should reject a date skeleton that standard options produce`() {
        assertEquals(
            listOf(
                "RU / task.a: invalid value 'dMMMM' of option 'icu:skeleton' of ':date': it equals the standard options " +
                    "'fields=month-day length=long' of ':date'; use them instead",
            ),
            ruErrors("task.a={\$d :date icu:skeleton=dMMMM}"),
        )
    }

    @Test
    fun `should reject a datetime skeleton of a date with a zone that standard options produce`() {
        assertEquals(
            listOf(
                "RU / task.a: invalid value 'yMMMMdz' of option 'icu:skeleton' of ':datetime': it equals the standard options " +
                    "'dateFields=year-month-day dateLength=long timeZoneStyle=short' of ':datetime'; use them instead",
            ),
            ruErrors("task.a={\$d :datetime icu:skeleton=yMMMMdz}"),
        )
    }

    @Test
    fun `should reject a variable select`() {
        assertEquals(
            listOf("RU / task.a: option 'select' of ':integer' must be a literal"),
            ruErrors("task.a=.input {\$n :integer select=\$mode} .match \$n $INTEGER_PLURAL_VARIANTS"),
        )
    }

    @Test
    fun `should reject explicit plural selection`() {
        assertEquals(
            listOf("RU / task.a: invalid value 'plural' of option 'select' of ':integer': plural is the default; remove the option"),
            ruErrors("task.a=.input {\$n :integer select=plural} .match \$n $INTEGER_PLURAL_VARIANTS"),
        )
    }

    @Test
    fun `should reject a variable option that refers to a declared variable`() {
        assertEquals(
            listOf(
                "RU / task.a: option 'maximumFractionDigits' of ':number' must take a message argument; '\$p' is declared in " +
                    "the message",
            ),
            ruErrors("task.a=.input {\$p :integer} {{{\$x :number maximumFractionDigits=\$p}}}"),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "{\$x :number notation=compact}",
            "{\$x :number maximumFractionDigits=2}",
            "{\$x :currency currency=EUR}",
            "{\$x :number numberingSystem=arab}",
            "{\$d :datetime timeZone=|Europe/Moscow|}",
            "{\$d :time timeZone=UTC}",
            "{\$d :date fields=year-month-day length=long calendar=buddhist}",
            "{\$d :date fields=year-month-day length=long calendar=gregory}",
            "{\$n :spellout rules=spellout-numbering}",
            "{\$m :unit unit=kilometer-per-hour}",
            "{\$n :number icu:skeleton=compact-short}",
            "{\$d :date icu:skeleton=yMMMM}",
            ".input {\$n :integer select=ordinal} .match \$n * {{x}}",
        ],
    )
    fun `should accept a valid literal option value`(message: String) {
        val problems = OptionsRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept a message argument as the value of an option that takes one`() {
        val problems = OptionsRule.problemsIn("{\$x :number maximumFractionDigits=\$precision}")

        assertEquals(emptyList<String>(), problems)
    }
}
