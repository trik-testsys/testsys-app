package tech.testsys.infra.localization.codegen.source

import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.fileError

/**
 * A region declared in `regions.properties`.
 *
 * @property id the region id, which is also the name of the enum constant.
 * @property languageTag the BCP 47 tag of the region locale, which is also the name of its directory of bundle files.
 * @property locale the ICU locale parsed from [languageTag].
 * @property comment the `#` comment lines directly above the entry, or `null`; it becomes the constant's KDoc.
 * @property line the line of the entry.
 */
internal data class RegionDefinition(
    val id: String,
    val languageTag: String,
    val locale: ULocale,
    val comment: String?,
    val line: Int,
)

/** Parser of `regions.properties`, the single source of the region set. */
internal object RegionsFile {
    // The file is read line by line rather than with java.util.Properties: the comment above an entry is data.

    const val FILE_NAME = "regions.properties"

    private val entryShape = Regex("^([^=]*)=(.*)$")
    private val regionId = Regex("[A-Z]{2}")

    /** Parses the decoded [text] of the file in declaration order, reporting problems into [errors]. */
    fun parse(text: String, errors: MutableList<String>): List<RegionDefinition> {
        val regions = mutableListOf<RegionDefinition>()
        val comment = mutableListOf<String>()
        text.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            when {
                line.isEmpty() -> comment.clear()
                line.startsWith("#") -> comment += line.removePrefix("#").trim()
                line.startsWith("!") -> {
                    errors += fileError(FILE_NAME, index + 1, Problems.Files.COMMENTS_START_WITH_HASH)
                    comment.clear()
                }
                else -> {
                    parseEntry(line, index + 1, comment.joinToString(" ").ifEmpty { null }, regions, errors)
                        ?.let(regions::add)
                    comment.clear()
                }
            }
        }
        if (regions.isEmpty()) errors += fileError(FILE_NAME, 1, Problems.Regions.NONE_DECLARED)
        return regions
    }

    private fun parseEntry(
        line: String,
        lineNumber: Int,
        comment: String?,
        declared: List<RegionDefinition>,
        errors: MutableList<String>,
    ): RegionDefinition? {
        val match = entryShape.matchEntire(line)
        if (match == null) {
            errors += fileError(FILE_NAME, lineNumber, Problems.Regions.ENTRY_SHAPE)
            return null
        }
        val id = match.groupValues[1].trim()
        val tag = match.groupValues[2].trim()
        val locale = ULocale.forLanguageTag(tag)
        val problem = when {
            !regionId.matches(id) -> Problems.Regions.regionId(id)
            declared.any { it.id == id } -> Problems.Regions.declaredTwice(id)
            tag.isEmpty() || locale.toLanguageTag() != tag -> Problems.Regions.languageTag(tag, id)
            else -> null
        }
        if (problem != null) {
            errors += fileError(FILE_NAME, lineNumber, problem)
            return null
        }
        return RegionDefinition(id = id, languageTag = tag, locale = locale, comment = comment, line = lineNumber)
    }
}
