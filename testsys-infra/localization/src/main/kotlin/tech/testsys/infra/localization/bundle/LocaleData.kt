package tech.testsys.infra.localization.bundle

import com.ibm.icu.number.LocalizedNumberFormatter
import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.text.BreakIterator
import com.ibm.icu.text.Collator
import com.ibm.icu.text.DateIntervalFormat
import com.ibm.icu.text.Normalizer2
import com.ibm.icu.text.RelativeDateTimeFormatter
import java.util.Calendar

/**
 * Region-bound bundle of ICU text and formatting facilities.
 *
 * A [LocaleData] groups the ICU APIs whose behaviour is locale-sensitive — number
 * formatting, date/time interval and relative formatting, Unicode
 * normalisation, collation, text segmentation, and calendar arithmetic — into
 * a single value object configured for one [SupportedRegion]. Build one per
 * region via [forRegion] and pass it to call sites that need
 * locale-aware behaviour, instead of re-deriving the underlying formatters on
 * every call. This guarantees every component shares a consistent view of the
 * region and avoids repeated ICU resource lookups.
 *
 * Per-call stateful APIs (e.g. `StringSearch`, which is parameterised by a
 * pattern and target) and pure static utilities (e.g. `UCharacter`) are
 * deliberately not held here: the former cannot be reused across queries, and
 * the latter is invoked directly via its static methods.
 *
 * @property numberFormatter Locale-aware number, currency, percentage, and
 *   unit formatting entry point.
 *   Example — render a participant's score as a percentage:
 *   ```
 *   val text = locale.numberFormatter
 *       .unit(NoUnit.PERCENT)
 *       .precision(Precision.fixedFraction(1))
 *       .format(0.872)
 *       .toString() // "87,2 %"
 *   ```
 * @property dateIntervalFormatter Formats date or date-time ranges with
 *   region-correct separators and field ordering.
 *   Example — render a contest's open window:
 *   ```
 *   val from = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 5) }
 *   val to = Calendar.getInstance().apply { set(2026, Calendar.JANUARY, 7) }
 *   locale.dateIntervalFormatter.format(DateInterval(from.timeInMillis, to.timeInMillis))
 *   // "5–7 янв. 2026 г."
 *   ```
 * @property relativeDateTimeFormatter Produces phrases like "через 3 дня" or
 *   "2 минуты назад" in the region's language.
 *   Example — show how long until a task deadline:
 *   ```
 *   locale.relativeDateTimeFormatter.format(
 *       3.0, RelativeDateTimeFormatter.Direction.NEXT,
 *       RelativeDateTimeFormatter.RelativeUnit.DAYS,
 *   ) // "через 3 дня"
 *   ```
 * @property normalizer Unicode NFC normaliser used to canonicalise text
 *   before comparison, hashing, or storage.
 *   Example — normalise a user-submitted answer before equality checking
 *   so that pre-composed and decomposed forms compare equal:
 *   ```
 *   val canonical = locale.normalizer.normalize(rawAnswer)
 *   ```
 * @property collator Locale-aware string comparator used for sorting and
 *   case- or accent-insensitive matching.
 *   Example — sort participant display names by Russian alphabetical order:
 *   ```
 *   val ordered = participants.sortedWith(locale.collator)
 *   ```
 * @property breakIterator Iterates word boundaries according to the region's
 *   segmentation rules.
 *   Example — count words in a free-form essay answer:
 *   ```
 *   locale.breakIterator.setText(answer)
 *   var words = 0
 *   var start = locale.breakIterator.first()
 *   var end = locale.breakIterator.next()
 *   while (end != BreakIterator.DONE) {
 *       if (answer.substring(start, end).any(Char::isLetterOrDigit)) words++
 *       start = end
 *       end = locale.breakIterator.next()
 *   }
 *   ```
 * @property calendar Calendar system used for date arithmetic and field
 *   extraction (year, month, week-of-year) in the region.
 *   Example — compute a submission's deadline 7 days from now:
 *   ```
 *   val deadline = (locale.calendar.clone() as Calendar).apply {
 *       add(Calendar.DAY_OF_MONTH, 7)
 *   }.time
 *   ```
 * @since %CURRENT_VERSION%
 */
@Suppress("VERBOSE_DOC")
data class LocaleData(
    val numberFormatter: LocalizedNumberFormatter,
    val dateIntervalFormatter: DateIntervalFormat,
    val relativeDateTimeFormatter: RelativeDateTimeFormatter,
    val normalizer: Normalizer2,
    val collator: Collator,
    val breakIterator: BreakIterator,
    val calendar: Calendar,
) {
    companion object {
        /**
         * Builds the [LocaleData] of [region].
         *
         * @since %CURRENT_VERSION%
         */
        fun forRegion(region: SupportedRegion): LocaleData {
            val uLocale = region.toULocale()
            return LocaleData(
                numberFormatter = NumberFormatter.withLocale(uLocale),
                dateIntervalFormatter = DateIntervalFormat.getInstance("yMMMdHm", uLocale),
                relativeDateTimeFormatter = RelativeDateTimeFormatter.getInstance(uLocale),
                normalizer = Normalizer2.getNFCInstance(),
                collator = Collator.getInstance(uLocale),
                breakIterator = BreakIterator.getWordInstance(uLocale),
                calendar = Calendar.getInstance(uLocale.toLocale()),
            )
        }
    }
}
