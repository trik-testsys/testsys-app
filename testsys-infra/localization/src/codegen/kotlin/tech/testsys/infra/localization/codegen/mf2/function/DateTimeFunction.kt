package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.DATE_LENGTH
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_PRECISION

private const val DATETIME = "datetime"

/** `:datetime`: the date and time of a moment. */
internal data object DateTimeFunction : TemporalFunction(
    name = DATETIME,
    options = listOf(
        DateTimeDefinitions.fields(DATE_FIELDS),
        DateTimeDefinitions.length(DATE_LENGTH),
        DateTimeDefinitions.precision(TIME_PRECISION),
        DateTimeDefinitions.hour12,
        DateTimeDefinitions.timeZoneStyle,
        DateTimeDefinitions.calendar,
        DateTimeDefinitions.timeZone,
        DateTimeDefinitions.skeleton(DATETIME) { DateTimeFunction.standardSkeletons },
    ),
) {
    override val standardSkeletons: Map<String, String> by lazy { DateTimeDefinitions.dateTimeSkeletons() }
}
