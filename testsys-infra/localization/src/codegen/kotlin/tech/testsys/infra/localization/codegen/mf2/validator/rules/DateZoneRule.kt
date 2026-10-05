package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.INPUT
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames.TIME_ZONE
import tech.testsys.infra.localization.codegen.mf2.model.EffectiveZone
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.validator.MessageRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects a date argument formatted in different time zones: the runtime converts it to one zone. */
internal object DateZoneRule : MessageRule {
    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> = message.argumentZones
        .filterValues { it.size > 1 }
        .map { (argument, zones) -> Problems.Zones.differentZones(argument, zones.map(::describe)) }

    private fun describe(zone: EffectiveZone): String = when (zone) {
        EffectiveZone.Context -> Problems.Zones.CONTEXT
        EffectiveZone.Input -> Problems.Zones.literal(TIME_ZONE, INPUT)
        is EffectiveZone.Fixed -> Problems.Zones.fixed(TIME_ZONE, zone.zoneId)
        is EffectiveZone.Argument -> Problems.Zones.argument(TIME_ZONE, zone.argument)
    }
}
