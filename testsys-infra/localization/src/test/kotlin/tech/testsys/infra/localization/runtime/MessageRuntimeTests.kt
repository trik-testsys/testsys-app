// The ICU-quirk tests use ICU's MF2 API directly; it is a technology preview (@Deprecated).
@file:Suppress("DEPRECATION")

package tech.testsys.infra.localization.runtime

import com.ibm.icu.message2.MessageFormatter
import com.ibm.icu.util.ULocale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.InternalLocalizationApi
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

@OptIn(InternalLocalizationApi::class)
class MessageRuntimeTests {

    @Nested
    inner class FormatTests {

        @Test
        fun `should format an instant in the fixed zone of the message`() {
            val runtime = MessageRuntime(
                regionId = "RU",
                locale = ULocale.forLanguageTag("ru-RU"),
                patterns = mapOf("key" to "{\$at :time precision=minute hour12=false timeZone=|Asia/Tokyo|}"),
                terms = emptyMap(),
                dateZones = mapOf("key" to mapOf("at" to DateZone.Fixed(ZoneId.of("Asia/Tokyo")))),
            )

            assertEquals("17:53", runtime.format("key", MOSCOW, "at" to INSTANT))
        }

        @Test
        fun `should format an instant in the zone of the zone argument`() {
            val runtime = zoneArgumentRuntime()

            assertEquals("17:53", runtime.format("key", MOSCOW, "at" to INSTANT, "zone" to ZoneId.of("Asia/Tokyo")))
        }

        @Test
        fun `should raise IllegalArgumentException if the zone argument is not a ZoneId`() {
            val runtime = zoneArgumentRuntime()

            assertThrows(IllegalArgumentException::class.java) {
                runtime.format("key", MOSCOW, "at" to INSTANT, "zone" to "Asia/Tokyo")
            }
        }

        @Test
        fun `should format an instant in the context zone`() {
            val runtime = ruRuntime("key" to "{\$at :time precision=minute hour12=false}")

            assertEquals("11:53", runtime.format("key", MOSCOW, "at" to INSTANT))
        }

        @Test
        fun `should format an instant in a context zone given as an offset`() {
            val runtime = ruRuntime("key" to "{\$at :time precision=minute hour12=false}")

            assertEquals("11:53", runtime.format("key", ZoneOffset.ofHours(3), "at" to INSTANT))
        }

        @Test
        fun `should format a zoned operand with an offset zone id in its own zone`() {
            val runtime = ruRuntime("key" to "{\$at :time precision=minute hour12=false timeZone=input}")

            assertEquals("11:53", runtime.format("key", MOSCOW, "at" to INSTANT.atZone(ZoneId.of("UTC+03:00"))))
        }

        @Test
        fun `should raise IllegalArgumentException if the key is unknown`() {
            val runtime = ruRuntime("key" to "text")

            assertThrows(IllegalArgumentException::class.java) { runtime.format("missing", MOSCOW) }
        }

        private fun zoneArgumentRuntime(): MessageRuntime = MessageRuntime(
            regionId = "RU",
            locale = ULocale.forLanguageTag("ru-RU"),
            patterns = mapOf("key" to "{\$at :time precision=minute hour12=false timeZone=\$zone}"),
            terms = emptyMap(),
            dateZones = mapOf("key" to mapOf("at" to DateZone.Argument("zone"))),
        )
    }

    // These tests pin the ICU4J 78.1 behaviour that the runtime works around; when an ICU upgrade changes it,
    // the corresponding conversion can be revisited.
    @Nested
    inner class IcuQuirkTests {

        private fun formatter(pattern: String): MessageFormatter = MessageFormatter.builder()
            .setLocale(Locale.forLanguageTag("ru-RU"))
            .setPattern(pattern)
            .setErrorHandlingBehavior(MessageFormatter.ErrorHandlingBehavior.STRICT)
            .build()

        @Test
        fun `should fail to format an Instant (ICU4J 78_1)`() {
            val formatter = formatter("{\$d :datetime}")

            assertThrows(IllegalArgumentException::class.java) { formatter.formatToString(mapOf("d" to INSTANT)) }
        }

        @Test
        fun `should ignore the timeZone option for a zoned operand (ICU4J 78_1)`() {
            val formatter = formatter("{\$d :time precision=minute hour12=false timeZone=|Asia/Tokyo|}")

            assertEquals("11:53", formatter.formatToString(mapOf("d" to INSTANT.atZone(MOSCOW))))
        }

        @Test
        fun `should show a zoned operand with an offset zone id it cannot parse in GMT (ICU4J 78_1)`() {
            val formatter = formatter("{\$d :time precision=minute hour12=false}")

            assertEquals("08:53", formatter.formatToString(mapOf("d" to INSTANT.atZone(ZoneId.of("UTC+03:00")))))
        }
    }
}
