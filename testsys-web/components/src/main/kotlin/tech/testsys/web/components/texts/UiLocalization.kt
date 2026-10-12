package tech.testsys.web.components.texts

import com.ibm.icu.text.DateTimePatternGenerator
import tech.testsys.infra.localization.Localization
import tech.testsys.infra.localization.bundle.SupportedRegion
import java.time.DayOfWeek
import java.time.Month
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

private const val DATE_SKELETON = "ddMMyyyy"
private const val TIME_SKELETON = "Hm"
private val SUNDAY_FIRST = DayOfWeek.entries.sortedBy { day -> day.value % DayOfWeek.entries.size }

/**
 * Messages, locale data and time zone of one UI. Vaadin serializes access to a UI with its session lock, so the UI
 * keeps one instance for all its requests and `UI.access` tasks. Code running without the lock, such as an upload
 * handler, gets its strings computed in advance.
 *
 * @property timeZone the zone of the dates shown to the user.
 * @property messages the messages of the region in [timeZone].
 * @property locale the locale of the region.
 * @property monthNames the twelve standalone month names starting from January.
 * @property weekdays the seven weekday names starting from Sunday.
 * @property weekdaysShort the seven short weekday names starting from Sunday.
 * @property firstDayOfWeek the day a calendar week starts with.
 * @property dateFormat the numeric date pattern of the locale, e.g. `dd.MM.yyyy`.
 * @property timeFormat the hour and minute pattern of the locale, e.g. `HH:mm`.
 */
internal class UiLocalization(region: SupportedRegion, val timeZone: ZoneId) {
    val messages: Localization = Localization.forRegion(region, timeZone)
    val locale: Locale = region.toULocale().toLocale()
    val monthNames: List<String> = Month.entries.map { month ->
        month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { letter -> letter.titlecase(locale) }
    }
    val weekdays: List<String> = SUNDAY_FIRST.map { day -> day.getDisplayName(TextStyle.FULL, locale) }
    val weekdaysShort: List<String> = SUNDAY_FIRST.map { day -> day.getDisplayName(TextStyle.SHORT_STANDALONE, locale) }
    val firstDayOfWeek: DayOfWeek = WeekFields.of(locale).firstDayOfWeek
    private val patterns = DateTimePatternGenerator.getInstance(region.toULocale())
    val dateFormat: String = patterns.getBestPattern(DATE_SKELETON)
    val timeFormat: String = patterns.getBestPattern(TIME_SKELETON)
}
