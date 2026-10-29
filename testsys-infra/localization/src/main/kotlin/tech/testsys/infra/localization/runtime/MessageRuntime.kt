// MessageFormatter and MFFunctionRegistry are an ICU technology preview (@Deprecated @internal); only the package
// runtime touches the MF2 API.
@file:Suppress("DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.runtime

import com.ibm.icu.message2.MFFunctionRegistry
import com.ibm.icu.message2.MessageFormatter
import com.ibm.icu.util.TimeZone
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.InternalLocalizationApi
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Formats the MF2 messages of one region with ICU4J's `MessageFormatter` and the custom functions of
 * [RegionFunctions]; generated bundles call [format].
 */
@InternalLocalizationApi
internal class MessageRuntime(
    private val regionId: String,
    locale: ULocale,
    private val patterns: Map<String, String>,
    terms: Map<String, String>,
    private val dateZones: Map<String, Map<String, DateZone>>,
) {
    // ICU cannot format an Instant and ignores the timeZone option for zoned values, hence the conversion in format.
    // One formatter per key is shared between threads: MessageFormatter keeps no mutable state between calls.
    private val javaLocale: Locale = locale.toLocale()
    private val termFormatters: Map<String, MessageFormatter> =
        terms.mapValues { (_, pattern) -> FormatterSettings.newFormatter(javaLocale, pattern, null) }
    private val registry: MFFunctionRegistry = RegionFunctions.registry(regionId, locale, termFormatters)
    private val formatters = ConcurrentHashMap<String, MessageFormatter>()

    /** Formats the message [key] with [arguments]; `Instant` arguments are shown in their message zone or [timeZone]. */
    fun format(key: String, timeZone: ZoneId, vararg arguments: Pair<String, Any>): String {
        val formatter = formatters.computeIfAbsent(key) {
            val pattern = requireNotNull(patterns[it]) { "Region '$regionId' has no localization message '$it'" }
            FormatterSettings.newFormatter(javaLocale, pattern, registry)
        }
        val values = arguments.toMap()
        val zones = dateZones[key].orEmpty()
        val resolved = values.mapValues { (name, value) ->
            when (value) {
                is Instant -> value.atZone(icuZone(zoneOf(zones[name], values, timeZone)))
                is ZonedDateTime -> value.withZoneSameInstant(icuZone(value.zone))
                else -> value
            }
        }
        return formatter.formatToString(resolved)
    }

    private fun zoneOf(zone: DateZone?, values: Map<String, Any>, contextZone: ZoneId): ZoneId = when (zone) {
        null -> contextZone
        is DateZone.Fixed -> zone.zone
        is DateZone.Argument -> requireNotNull(values[zone.argument] as? ZoneId) {
            "Region '$regionId': the time zone argument '${zone.argument}' is not a ZoneId"
        }
    }
}

private const val GMT = "GMT"

// ICU parses a custom zone id only with the prefix GMT and silently shows any other id it does not know, such as
// +03:00 or UTC+03:00, in GMT; such an offset zone is passed as GMT±hh:mm.
@InternalLocalizationApi
internal fun icuZone(zone: ZoneId): ZoneId {
    val offset = zone.normalized() as? ZoneOffset
    return if (offset != null && TimeZone.getCanonicalID(zone.id) == null) ZoneId.ofOffset(GMT, offset) else zone
}
