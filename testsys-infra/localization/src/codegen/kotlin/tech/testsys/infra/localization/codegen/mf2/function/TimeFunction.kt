package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.PRECISION

private const val TIME = "time"

/** `:time`: the time of a moment. */
internal data object TimeFunction : TemporalFunction(
    name = TIME,
    options = listOf(
        DateTimeDefinitions.precision(PRECISION),
        DateTimeDefinitions.hour12,
        DateTimeDefinitions.timeZoneStyle,
        DateTimeDefinitions.calendar,
        DateTimeDefinitions.timeZone,
        DateTimeDefinitions.skeleton(TIME) { TimeFunction.standardSkeletons },
    ),
) {
    override val standardSkeletons: Map<String, String> by lazy { DateTimeDefinitions.timeSkeletons(PRECISION) }
}
