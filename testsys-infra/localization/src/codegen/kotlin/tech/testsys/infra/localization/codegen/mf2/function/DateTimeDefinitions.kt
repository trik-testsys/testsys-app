package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.option.CalendarCheck
import tech.testsys.infra.localization.codegen.mf2.function.option.DateSkeleton
import tech.testsys.infra.localization.codegen.mf2.function.option.DateSkeletonCheck
import tech.testsys.infra.localization.codegen.mf2.function.option.EnumValues
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.CALENDAR
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_LENGTH
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.HOUR12
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.ICU_SKELETON
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.INPUT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.LENGTH
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_PRECISION
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_ZONE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_ZONE_STYLE
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec
import tech.testsys.infra.localization.codegen.mf2.function.option.ZoneIdCheck
import tech.testsys.infra.localization.codegen.mf2.model.EffectiveZone
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.Operand
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand

/**
 * A date or time function: it formats one moment in its effective zone.
 *
 * @property standardSkeletons the canonical skeleton of every standard option combination, mapped to that
 *   combination; an `icu:skeleton` equal to one of them is redundant.
 */
internal sealed class TemporalFunction(name: String, options: List<OptionSpec>) : Mf2Function(name, options) {
    abstract val standardSkeletons: Map<String, String>

    override fun operandType(options: Map<String, Operand>): ArgumentType =
        if (DateTimeDefinitions.isInputZone(options)) ArgumentType.ZONED_DATE_TIME else ArgumentType.INSTANT

    override fun effectiveZone(options: Map<String, Operand>): EffectiveZone = when (val zone = options[TIME_ZONE]) {
        null -> EffectiveZone.Context
        LiteralOperand(INPUT) -> EffectiveZone.Input
        is LiteralOperand -> EffectiveZone.Fixed(zone.value)
        is VariableOperand -> EffectiveZone.Argument(zone.name)
    }

    override fun literalProblem(value: String, options: Map<String, Operand>): String? = Literals.isoDateProblem(value, name)
        ?: Problems.Operands.needsVariable(TIME_ZONE, INPUT).takeIf { DateTimeDefinitions.isInputZone(options) }
}

/** The options the date and time functions share and the option → skeleton tables of ICU4J 78.1. */
internal object DateTimeDefinitions {
    private const val LONG = "long"
    private const val MEDIUM = "medium"
    private const val SHORT = "short"

    val hour12 = OptionSpec(HOUR12, check = EnumValues("true", "false"))
    val timeZoneStyle = OptionSpec(TIME_ZONE_STYLE, check = EnumValues(LONG, SHORT))
    val calendar = OptionSpec(CALENDAR, check = CalendarCheck)
    val timeZone = OptionSpec(TIME_ZONE, ArgumentType.ZONE_ID, ZoneIdCheck)

    // The option -> skeleton tables of ICU4J 78.1 DateTimeFunctionFactory: fields, length and skeleton.
    private val dateStyles: List<Triple<String, String, String>> = listOf(
        Triple("weekday", LONG, "EEEE"), Triple("weekday", MEDIUM, "E"), Triple("weekday", SHORT, "EEEEEE"),
        Triple("day-weekday", LONG, "dEEEE"), Triple("day-weekday", MEDIUM, "dE"), Triple("day-weekday", SHORT, "dEEEEEE"),
        Triple("month-day", LONG, "MMMMd"), Triple("month-day", MEDIUM, "MMMd"), Triple("month-day", SHORT, "Md"),
        Triple("month-day-weekday", LONG, "MMMMdEEEE"), Triple("month-day-weekday", MEDIUM, "MMMdE"),
        Triple("month-day-weekday", SHORT, "MdEEEEEE"),
        Triple("year-month-day", LONG, "yMMMMd"), Triple("year-month-day", MEDIUM, "yMMMd"), Triple("year-month-day", SHORT, "yMd"),
        Triple("year-month-day-weekday", LONG, "yMMMMdEEEE"), Triple("year-month-day-weekday", MEDIUM, "yMMMdE"),
        Triple("year-month-day-weekday", SHORT, "yMdEEEEEE"),
    )

    // Precision, hour12 (empty if not set) and skeleton.
    private val timeStyles: List<Triple<String, String, String>> = listOf(
        Triple("hour", "", "j"), Triple("hour", "true", "h"), Triple("hour", "false", "H"),
        Triple("minute", "", "jm"), Triple("minute", "true", "hm"), Triple("minute", "false", "Hm"),
        Triple("second", "", "jms"), Triple("second", "true", "hms"), Triple("second", "false", "Hms"),
    )
    private val zoneStyles: Map<String, String> = mapOf(LONG to "zzzz", SHORT to "z")

    /** The `fields`-like option [name] of the date part. */
    fun fields(name: String): OptionSpec = OptionSpec(
        name,
        check = EnumValues("weekday", "day-weekday", "month-day", "month-day-weekday", "year-month-day", "year-month-day-weekday"),
    )

    /** The `length`-like option [name] of the date part. */
    fun length(name: String): OptionSpec = OptionSpec(name, check = EnumValues(LONG, MEDIUM, SHORT))

    /** The `precision`-like option [name] of the time part. */
    fun precision(name: String): OptionSpec = OptionSpec(name, check = EnumValues("hour", "minute", "second"))

    /** The `icu:skeleton` option of the date [function] with its [standardSkeletons]. */
    fun skeleton(function: String, standardSkeletons: () -> Map<String, String>): OptionSpec =
        OptionSpec(ICU_SKELETON, check = DateSkeletonCheck(function, standardSkeletons))

    /** Returns whether the combined [options] format a zoned operand in its own zone (`timeZone=input`). */
    fun isInputZone(options: Map<String, Operand>): Boolean = options[TIME_ZONE] == LiteralOperand(INPUT)

    /** The standard skeletons of `:date`. */
    fun dateSkeletons(): Map<String, String> = dateStyles.associate { (fields, length, skeleton) ->
        DateSkeleton.canonical(skeleton) to "$FIELDS=$fields $LENGTH=$length"
    }

    /** The standard skeletons of a time function whose precision option is [precisionOption]. */
    fun timeSkeletons(precisionOption: String): Map<String, String> {
        val combinations = linkedMapOf<String, String>()
        timeStyles.forEach { (precision, hour12, skeleton) ->
            val base = "$precisionOption=$precision" + if (hour12.isEmpty()) "" else " $HOUR12=$hour12"
            combinations[DateSkeleton.canonical(skeleton)] = base
            zoneStyles.forEach { (style, zone) ->
                combinations[DateSkeleton.canonical(skeleton + zone)] = "$base $TIME_ZONE_STYLE=$style"
            }
        }
        return combinations
    }

    /**
     * The standard skeletons of `:datetime`: its date part, its date part with a zone style, its time part and every
     * pair of the date and time parts.
     */
    fun dateTimeSkeletons(): Map<String, String> {
        val dates = dateStyles.associate { (fields, length, skeleton) -> skeleton to "$DATE_FIELDS=$fields $DATE_LENGTH=$length" }
        val times = timeSkeletons(TIME_PRECISION)
        val combinations = linkedMapOf<String, String>()
        dates.forEach { (skeleton, options) -> combinations[DateSkeleton.canonical(skeleton)] = options }
        dates.forEach { (skeleton, options) ->
            zoneStyles.forEach { (style, zone) ->
                combinations[DateSkeleton.canonical(skeleton + zone)] = "$options $TIME_ZONE_STYLE=$style"
            }
        }
        times.forEach { (skeleton, options) -> combinations.putIfAbsent(skeleton, options) }
        dates.forEach { (date, dateOptions) ->
            times.forEach { (time, timeOptions) -> combinations[DateSkeleton.canonical(date + time)] = "$dateOptions $timeOptions" }
        }
        return combinations
    }
}
