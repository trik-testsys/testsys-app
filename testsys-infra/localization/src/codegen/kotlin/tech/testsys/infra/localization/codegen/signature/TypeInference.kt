package tech.testsys.infra.localization.codegen.signature

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.function.ArgumentType
import tech.testsys.infra.localization.codegen.mf2.function.Selection
import tech.testsys.infra.localization.codegen.mf2.model.EffectiveZone
import tech.testsys.infra.localization.codegen.mf2.model.FunctionRef
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.model.Root
import tech.testsys.infra.localization.codegen.mf2.model.SelectMessage

/**
 * The argument types of one message.
 *
 * @property placeholders the typed arguments in first-use order.
 * @property dateZones the zone of every date argument that does not use the context zone.
 * @property problems the uses of one argument with incompatible types.
 */
internal data class Inference(
    val placeholders: Map<String, Placeholder>,
    val dateZones: Map<String, DateZoneSpec>,
    val problems: List<String>,
)

/**
 * Kotlin types of message arguments, derived from the functions and options that use them. Every use gives a
 * placeholder; the placeholders of one argument are merged in text order.
 */
internal object TypeInference {

    /** Infers the argument types of [message]. */
    fun infer(message: ResolvedMessage): Inference {
        val selectedKeys = selectedKeys(message)
        val problems = mutableListOf<String>()
        val placeholders = uses(message)
            .filterValues { it.isNotEmpty() }
            .mapValues { (name, uses) -> merge(typed(name, uses, selectedKeys[name]), problems) }
        return Inference(placeholders, dateZones(message), problems)
    }

    // Every argument, including those without a typed use, keeps its first-use position.
    private fun uses(message: ResolvedMessage): Map<String, List<Placeholder>> {
        val uses = message.arguments.associateWith { mutableListOf<Placeholder>() }
        message.expressions.flatMap(::placeholders).forEach { uses.getValue(it.name) += it }
        return uses
    }

    // The option values of an expression are typed before its operand.
    private fun placeholders(expression: ResolvedExpression): List<Placeholder> {
        val root = expression.value.root as? Root.Argument
        return when (val function = expression.function) {
            null -> listOfNotNull(root?.takeIf { expression.base.function == null }?.let { Placeholder.StringPlaceholder(it.name) })
            is FunctionRef.Unknown -> emptyList()
            is FunctionRef.Known -> {
                val options = expression.options.mapNotNull { option ->
                    option.argument?.let { argument -> placeholder(argument.name, argument.type) }
                }
                val operand = root?.let { placeholder(it.name, function.function.operandType(expression.value.options)) }
                options + listOfNotNull(operand)
            }
        }
    }

    // The keys of the `:string` selectors of the argument, if every selector selects.
    private fun selectedKeys(message: ResolvedMessage): Map<String, Set<String>> {
        if (message.source !is SelectMessage || message.selectors.any { it.selection == null }) return emptyMap()
        return message.selectors.mapNotNull { selector ->
            val root = selector.value?.root as? Root.Argument
            if (selector.selection != Selection.TEXT || root == null) null else root.name to selector.literalKeys.toSet()
        }.toMap()
    }

    private fun typed(name: String, uses: List<Placeholder>, keys: Set<String>?): List<Placeholder> {
        if (keys == null) return uses
        return uses.map { if (it is Placeholder.StringPlaceholder) Placeholder.SelectPlaceholder(name, keys) else it }
    }

    private fun merge(uses: List<Placeholder>, problems: MutableList<String>): Placeholder = uses.reduce { merged, next ->
        mergePlaceholders(merged, next) ?: merged.also {
            problems += Problems.Signatures.conflictingTypes(merged.name, merged.typeName, next.typeName)
        }
    }

    private fun dateZones(message: ResolvedMessage): Map<String, DateZoneSpec> = message.argumentZones
        .mapNotNull { (argument, zones) -> spec(zones.first())?.let { zone -> argument to zone } }
        .toMap()

    private fun spec(zone: EffectiveZone): DateZoneSpec? = when (zone) {
        EffectiveZone.Context, EffectiveZone.Input -> null
        is EffectiveZone.Fixed -> DateZoneSpec.Fixed(zone.zoneId)
        is EffectiveZone.Argument -> DateZoneSpec.Argument(zone.argument)
    }

    private fun placeholder(name: String, type: ArgumentType): Placeholder = when (type) {
        ArgumentType.STRING -> Placeholder.StringPlaceholder(name)
        ArgumentType.INT -> Placeholder.IntPlaceholder(name)
        ArgumentType.NUMBER -> Placeholder.NumberPlaceholder(name)
        ArgumentType.INSTANT -> Placeholder.InstantPlaceholder(name)
        ArgumentType.ZONED_DATE_TIME -> Placeholder.ZonedDateTimePlaceholder(name)
        ArgumentType.ZONE_ID -> Placeholder.ZoneIdPlaceholder(name)
        ArgumentType.CURRENCY_AMOUNT -> Placeholder.CurrencyAmountPlaceholder(name)
    }
}
