package tech.testsys.infra.localization.bundle

import com.ibm.icu.util.DateInterval
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

// The JVM default zone is UTC in this test task, so every expectation below proves that the passed zone applies.
class LocaleDataTests {

    private val interval = DateInterval(
        Instant.parse("2026-01-05T09:00:00Z").toEpochMilli(),
        Instant.parse("2026-01-07T15:30:00Z").toEpochMilli(),
    )

    @Test
    fun `should format the interval in the passed zone`() {
        val locale = LocaleData.forRegion(SupportedRegion.RU, ZoneId.of("Europe/Moscow"))

        assertEquals(
            "5 янв. 2026${NARROW_NBSP}г., 12:00 – 7 янв. 2026${NARROW_NBSP}г., 18:30",
            locale.dateIntervalFormatter.format(interval),
        )
    }

    @Test
    fun `should format the interval in an offset zone rather than in GMT`() {
        val locale = LocaleData.forRegion(SupportedRegion.RU, ZoneOffset.ofHours(3))

        assertEquals(
            "5 янв. 2026${NARROW_NBSP}г., 12:00 – 7 янв. 2026${NARROW_NBSP}г., 18:30",
            locale.dateIntervalFormatter.format(interval),
        )
    }

    @Test
    fun `should create the calendar in the passed zone`() {
        val locale = LocaleData.forRegion(SupportedRegion.RU, ZoneId.of("Asia/Tokyo"))

        assertEquals("Asia/Tokyo", locale.calendar.timeZone.id)
    }

    @Test
    fun `should create the calendar in an offset zone rather than in GMT`() {
        val locale = LocaleData.forRegion(SupportedRegion.RU, ZoneId.of("UTC+03:00"))

        assertEquals(TimeUnit.HOURS.toMillis(3).toInt(), locale.calendar.timeZone.rawOffset)
    }

    private companion object {
        // ICU separates the Russian year abbreviation with a narrow no-break space; the code point keeps the
        // expectations visible in the source.
        val NARROW_NBSP = Char(0x202F).toString()
    }
}
