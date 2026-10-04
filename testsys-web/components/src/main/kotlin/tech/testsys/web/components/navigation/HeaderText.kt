package tech.testsys.web.components.navigation

import java.util.Locale

/** Literal text segment used to highlight menu matches without interpreting HTML. */
internal data class HeaderTextPart(val text: String, val isMatched: Boolean)

/** Case-folded literal matches, merging overlaps and preserving original Unicode offsets. */
internal fun splitHeaderMatches(value: String, query: String): List<HeaderTextPart> {
    val needle = query.trim().lowercase(Locale.ROOT)
    if (needle.isEmpty()) return listOf(HeaderTextPart(value, false))
    val normalized = value.lowercase(Locale.ROOT)
    val starts = mutableListOf<Int>()
    val ends = mutableListOf<Int>()
    var offset = 0
    while (offset < value.length) {
        val length = Character.charCount(value.codePointAt(offset))
        val folded = value.substring(startIndex = offset, endIndex = offset + length).lowercase(Locale.ROOT)
        repeat(folded.length) {
            starts.add(offset)
            ends.add(offset + length)
        }
        offset += length
    }
    val ranges = mutableListOf<IntRange>()
    var from = 0
    while (from <= normalized.length - needle.length) {
        val index = normalized.indexOf(needle, startIndex = from)
        if (index < 0) break
        val start = starts[index]
        val end = ends[index + needle.length - 1]
        val previous = ranges.lastOrNull()
        if (previous != null && start <= previous.last + 1) {
            ranges[ranges.lastIndex] = previous.first until maxOf(end, previous.last + 1)
        } else {
            ranges.add(start until end)
        }
        from = index + 1
    }
    val parts = mutableListOf<HeaderTextPart>()
    var cursor = 0
    ranges.forEach { range ->
        if (range.first > cursor) parts.add(HeaderTextPart(value.substring(startIndex = cursor, endIndex = range.first), false))
        parts.add(HeaderTextPart(value.substring(startIndex = range.first, endIndex = range.last + 1), true))
        cursor = range.last + 1
    }
    if (cursor < value.length) parts.add(HeaderTextPart(value.substring(cursor), false))
    return parts
}
