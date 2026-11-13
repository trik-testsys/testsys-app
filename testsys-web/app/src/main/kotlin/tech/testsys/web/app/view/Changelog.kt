package tech.testsys.web.app.view

import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/** SemVer 2.0 version without build metadata: the numeric core and the optional pre-release identifiers. */
private val SEMVER = Regex("""(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?""")

/** Number of the numeric parts of a version core: major, minor and patch. */
private const val CORE_PARTS = 3

/** Precedence of SemVer 2.0 versions: from the oldest to the newest. */
internal val versionOrder: Comparator<String> = Comparator { left, right ->
    val (leftCore, leftPreRelease) = parseVersion(left)
    val (rightCore, rightPreRelease) = parseVersion(right)
    leftCore.zip(rightCore).map { (a, b) -> compareNumbers(a, b) }.firstOrNull { result -> result != 0 }
        ?: comparePreReleases(leftPreRelease, rightPreRelease)
}

/**
 * Returns the user changelog of the region [languageTag]: the files `changelog/<languageTag>/<version>.md` of the
 * classpath as pairs of the version and its Markdown text, from the newest version to the oldest.
 *
 * @throws IllegalStateException if a file name is not a SemVer version.
 */
internal fun readChangelog(languageTag: String): List<Pair<String, String>> =
    PathMatchingResourcePatternResolver().getResources("classpath*:changelog/$languageTag/*.md")
        .map { resource ->
            val version = checkNotNull(resource.filename).removeSuffix(".md").also(::parseVersion)
            version to resource.getContentAsString(Charsets.UTF_8)
        }
        .sortedWith(compareBy(versionOrder.reversed()) { (version, _) -> version })

private fun parseVersion(version: String): Pair<List<String>, List<String>> {
    val match = SEMVER.matchEntire(version)
    checkNotNull(match) { "Changelog file '$version.md' is not named by a SemVer version" }
    val preRelease = match.groupValues[CORE_PARTS + 1]
    return match.groupValues.subList(1, CORE_PARTS + 1) to if (preRelease.isEmpty()) emptyList() else preRelease.split('.')
}

// A release follows its pre-releases; identifiers compare one by one, and a longer list of equal ones is newer.
private fun comparePreReleases(left: List<String>, right: List<String>): Int = when {
    left.isEmpty() || right.isEmpty() -> right.size.coerceAtMost(1) - left.size.coerceAtMost(1)
    else -> left.zip(right).map { (a, b) -> compareIdentifiers(a, b) }.firstOrNull { result -> result != 0 }
        ?: left.size.compareTo(right.size)
}

// Numeric identifiers compare as numbers and precede alphanumeric ones, which compare in ASCII order.
private fun compareIdentifiers(left: String, right: String): Int {
    val isLeftNumeric = left.all { symbol -> symbol in '0'..'9' }
    val isRightNumeric = right.all { symbol -> symbol in '0'..'9' }
    return when {
        isLeftNumeric && isRightNumeric -> compareNumbers(left, right)
        isLeftNumeric -> -1
        isRightNumeric -> 1
        else -> left.compareTo(right)
    }
}

private fun compareNumbers(left: String, right: String): Int = left.toBigInteger().compareTo(right.toBigInteger())
