package tech.testsys.infra.localization.codegen.mf2.function.option

import com.ibm.icu.number.NumberFormatter
import com.ibm.icu.number.SkeletonSyntaxException
import com.ibm.icu.util.ULocale
import tech.testsys.infra.localization.codegen.Problems

/** The value is a number skeleton that `NumberFormatter.forSkeleton` accepts. */
internal object NumberSkeletonCheck : ValueCheck {
    override fun findProblem(value: String, locale: ULocale): String? = try {
        NumberFormatter.forSkeleton(value)
        null
    } catch (e: SkeletonSyntaxException) {
        Problems.Values.numberSkeleton(e.message)
    }
}

/**
 * The value is a date skeleton of known fields of valid widths that no standard option combination of the date
 * [function] produces; [standardSkeletons] maps canonical skeletons to those combinations.
 */
internal class DateSkeletonCheck(
    private val function: String,
    private val standardSkeletons: () -> Map<String, String>,
) : ValueCheck {
    // ICU checks number skeletons itself but accepts any date skeleton: `yMMMMMMMd` renders «9 0000010 2025 г.».
    // A date skeleton that a standard option combination already produces is rejected, so `icu:skeleton` stays an
    // escape hatch for formats the MF2 options cannot express.

    override fun findProblem(value: String, locale: ULocale): String? {
        val runs = DateSkeleton.runs(value)
        return runs.firstNotNullOfOrNull { (letter, width) -> DateSkeleton.fieldProblem(letter, width) }
            ?: DateSkeleton.repeatedField(runs)?.let(Problems.Values::repeatedField)
            ?: standardSkeletons()[DateSkeleton.canonical(runs)]?.let { Problems.Values.standardSkeleton(it, function) }
    }
}

/** Syntax of date skeletons: runs of UTS #35 pattern field letters and their canonical order. */
internal object DateSkeleton {
    private const val FULL_ZONE_WIDTH = 4

    // Pattern field letters of UTS #35 with the widths a skeleton may use.
    private val fieldWidths: Map<Char, Set<Int>> = listOf(
        field('G', maxWidth = 5), field('y', maxWidth = 9), field('Y', maxWidth = 9), field('u', maxWidth = 9),
        field('U', maxWidth = 5), field('r', maxWidth = 9), field('Q', maxWidth = 5), field('q', maxWidth = 5),
        field('M', maxWidth = 5), field('L', maxWidth = 5), field('w', maxWidth = 2), field('W', maxWidth = 1),
        field('d', maxWidth = 2), field('D', maxWidth = 3), field('F', maxWidth = 1), field('g', maxWidth = 9),
        field('E', maxWidth = 6), field('e', maxWidth = 6), field('c', maxWidth = 6), field('a', maxWidth = 5),
        field('b', maxWidth = 5), field('B', maxWidth = 5), field('h', maxWidth = 2), field('H', maxWidth = 2),
        field('K', maxWidth = 2), field('k', maxWidth = 2), field('j', maxWidth = 6), field('J', maxWidth = 2),
        field('C', maxWidth = 6), field('m', maxWidth = 2), field('s', maxWidth = 2), field('S', maxWidth = 9),
        field('A', maxWidth = 9), field('z', maxWidth = 4), field('Z', maxWidth = 5), field('V', maxWidth = 4),
        field('X', maxWidth = 5), field('x', maxWidth = 5),
        'O' to setOf(1, FULL_ZONE_WIDTH), 'v' to setOf(1, FULL_ZONE_WIDTH),
    ).toMap()

    /** Splits [skeleton] into runs of one letter and their widths. */
    fun runs(skeleton: String): List<Pair<Char, Int>> {
        val runs = mutableListOf<Pair<Char, Int>>()
        skeleton.forEach { char ->
            val last = runs.lastOrNull()
            if (last != null && last.first == char) runs[runs.lastIndex] = char to last.second + 1 else runs += char to 1
        }
        return runs
    }

    /** Returns [runs] as a skeleton with the fields in letter order, so equal field sets compare equal. */
    fun canonical(runs: List<Pair<Char, Int>>): String =
        runs.sortedBy { it.first }.joinToString("") { (letter, width) -> letter.toString().repeat(width) }

    /** Returns [skeleton] with the fields in letter order. */
    fun canonical(skeleton: String): String = canonical(runs(skeleton))

    /** Returns why a run of [width] letters [letter] is not a valid field, or `null`. */
    fun fieldProblem(letter: Char, width: Int): String? {
        val widths = fieldWidths[letter] ?: return Problems.Values.notDateField(letter)
        return if (width in widths) null else Problems.Values.fieldWidth(letter, width, widths.sorted())
    }

    /** Returns the first letter that has more than one run in [runs], or `null`. */
    fun repeatedField(runs: List<Pair<Char, Int>>): Char? = runs.groupBy { it.first }.filterValues { it.size > 1 }.keys.firstOrNull()

    private fun field(letter: Char, maxWidth: Int): Pair<Char, Set<Int>> = letter to (1..maxWidth).toSet()
}
