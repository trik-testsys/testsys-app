package tech.testsys.web.app.view

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User

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
 * Name of the directory of the changes no release holds yet; a release renames the directory to its version. The name
 * is assembled from pieces: a release replaces the whole placeholder in Kotlin sources with its version.
 */
internal const val UNRELEASED_VERSION = "%CURRENT_VERSION" + "%"

/** Common part of a version, which every user sees. */
private const val COMMON_PART = "common"

/** Parts of a version in their order on the page: the common part, then the parts of the roles. */
internal val CHANGELOG_PARTS = listOf(
    COMMON_PART, "administrator", "manager", "student", "developer", "judge", "supervisor", "participant", "observer",
)

/**
 * Returns the user changelog of the region [languageTag]: the files `changelog/<languageTag>/<version>/<part>.md` of
 * the classpath as pairs of the version and the Markdown text of its [parts]: [UNRELEASED_VERSION] first, then from
 * the newest version to the oldest.
 *
 * @throws IllegalStateException if a directory name is neither a SemVer version nor [UNRELEASED_VERSION] or a file
 * name is not a part.
 */
internal fun readChangelog(languageTag: String, parts: Set<String> = CHANGELOG_PARTS.toSet()): List<Pair<String, String>> =
    PathMatchingResourcePatternResolver().getResources("classpath*:changelog/$languageTag/*/*.md")
        .map { resource ->
            val part = checkNotNull(resource.filename).removeSuffix(".md")
            check(part in CHANGELOG_PARTS) { "Changelog file '$part.md' is not named by a part of a version" }
            // The URL of a resource may escape the percent signs of the unreleased directory.
            val version = resource.url.path.substringBeforeLast('/').substringAfterLast('/').replace("%25", "%")
            if (version != UNRELEASED_VERSION) parseVersion(version)
            version to part
        }
        .filter { (_, part) -> part in parts }
        .groupBy({ (version, _) -> version }, { (_, part) -> part })
        .map { (version, versionParts) ->
            // The class loader reads the file: the URL of a file found in a jar leaves the percent signs of the
            // unreleased directory unescaped, and such a URL does not open.
            version to versionParts.sortedBy(CHANGELOG_PARTS::indexOf).joinToString("\n") { part ->
                ClassPathResource("changelog/$languageTag/$version/$part.md").getContentAsString(Charsets.UTF_8)
            }
        }
        .sortedWith(
            compareBy<Pair<String, String>> { (version, _) -> version != UNRELEASED_VERSION }
                .thenBy(versionOrder.reversed()) { (version, _) -> version },
        )

/** Returns the changelog parts [user] sees: the common part and the parts of their roles, all parts for a supervisor. */
internal fun changelogPartsOf(user: User<*>): Set<String> = when (user) {
    is MultipleRoleUser -> if (user.data.roles.any { role -> role is Supervisor }) {
        CHANGELOG_PARTS.toSet()
    } else {
        setOf(COMMON_PART) + user.data.roles.map(::partOf)
    }
    is Participant -> setOf(COMMON_PART, "participant")
    is Observer -> setOf(COMMON_PART, "observer")
}

private fun partOf(role: CompatibleUserRole): String = when (role) {
    is Administrator -> "administrator"
    is Manager -> "manager"
    is Student -> "student"
    is Developer -> "developer"
    is Judge -> "judge"
    is Supervisor -> "supervisor"
}

private fun parseVersion(version: String): Pair<List<String>, List<String>> {
    val match = SEMVER.matchEntire(version)
    checkNotNull(match) { "Changelog directory '$version' is not named by a SemVer version" }
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
