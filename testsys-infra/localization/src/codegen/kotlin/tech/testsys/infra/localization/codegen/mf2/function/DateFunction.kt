package tech.testsys.infra.localization.codegen.mf2.function

import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.FIELDS
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.LENGTH

private const val DATE = "date"

/** `:date`: the date of a moment. */
internal data object DateFunction : TemporalFunction(
    name = DATE,
    options = listOf(
        DateTimeDefinitions.fields(FIELDS),
        DateTimeDefinitions.length(LENGTH),
        DateTimeDefinitions.calendar,
        DateTimeDefinitions.timeZone,
        DateTimeDefinitions.skeleton(DATE) { DateFunction.standardSkeletons },
    ),
) {
    override val standardSkeletons: Map<String, String> by lazy { DateTimeDefinitions.dateSkeletons() }
}
