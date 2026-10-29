package tech.testsys.infra.localization.codegen.mf2.model

import tech.testsys.infra.localization.codegen.mf2.function.ArgumentType
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.Selection
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionSpec

/**
 * One message with every expression resolved the way ICU4J 78.1 resolves it: a variable declared with a function
 * passes that function and its options on to every expression that uses it, and later options override earlier
 * ones. Rules and type inference read this model; it holds facts, not problems.
 *
 * @property source the parsed message.
 * @property declarations the `.input` and `.local` declarations in order.
 * @property selectors the `.match` selectors in order; empty for a message without `.match`.
 * @property patternParts the expressions and markup of every variant pattern, in text order.
 * @property references every use of a variable name, in text order.
 * @property arguments the message arguments in first-use order: a use is an operand that no declaration binds or
 *   the value of an option that takes an argument.
 * @property declarationParts the expression or markup of every declaration, in order.
 * @property expressions every expression of the declarations and patterns, in text order.
 * @property argumentZones the zones of every argument formatted by a date function, in first-use order.
 */
internal data class ResolvedMessage(
    val source: Mf2Message,
    val declarations: List<ResolvedDeclaration>,
    val selectors: List<ResolvedSelector>,
    val patternParts: List<Occurrence>,
    val references: List<Reference>,
    val arguments: List<String>,
) {
    val declarationParts: List<Occurrence>
        get() = declarations.map { it.part }

    val expressions: List<ResolvedExpression>
        get() = (declarationParts + patternParts).filterIsInstance<ResolvedExpression>()

    val argumentZones: Map<String, Set<EffectiveZone>>
        get() = expressions
            .mapNotNull { it.zonedArgument }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, zones) -> zones.toSet() }
}

/**
 * A declaration of the variable [name].
 *
 * @property isInput whether it is `.input` rather than `.local`.
 * @property part the declared expression, or markup of a `.local`.
 */
internal data class ResolvedDeclaration(val name: String, val isInput: Boolean, val part: Occurrence)

/** A placeholder of a declaration or pattern, in the order rules check them. */
internal sealed interface Occurrence

/** Markup `{#name}`, which the API cannot render. */
internal data class MarkupOccurrence(val markup: Markup) : Occurrence

/**
 * One expression occurrence.
 *
 * @property source the parsed expression.
 * @property operandReference the reference of a variable operand, or `null` for a literal or no operand.
 * @property base the value of the operand before the function of this expression.
 * @property function the function of the expression, or `null` for `{$x}` and `{|literal|}`.
 * @property options the own options of the expression in source order; empty if the function is not known.
 * @property value the value after the function: the base value for no or an unknown function.
 * @property zone the zone of a date function, or `null` for other functions.
 * @property knownFunction the function if it is supported, or `null`.
 * @property ownValues the own options by name, including those of an unknown function.
 * @property zonedArgument the argument a date function formats and its [zone], or `null`.
 */
internal data class ResolvedExpression(
    val source: Expression,
    val operandReference: Reference?,
    val base: Value,
    val function: FunctionRef?,
    val options: List<ResolvedOption>,
    val value: Value,
    val zone: EffectiveZone?,
) : Occurrence {
    val knownFunction: Mf2Function?
        get() = (function as? FunctionRef.Known)?.function

    val ownValues: Map<String, Operand>
        get() = source.function?.options.orEmpty()

    val zonedArgument: Pair<String, EffectiveZone>?
        get() {
            val argument = value.root as? Root.Argument ?: return null
            return zone?.let { argument.name to it }
        }
}

/**
 * The value of an expression: its [root] operand and the functions applied to it.
 *
 * @property function the last function applied, or `null` for an unannotated value.
 * @property options the options of every function applied, later ones overriding earlier ones.
 * @property isIntegerOperand whether the value always formats as an integer (`:integer` and its `:offset`).
 */
internal data class Value(
    val root: Root,
    val function: Mf2Function? = null,
    val options: Map<String, Operand> = emptyMap(),
    val isIntegerOperand: Boolean = false,
)

/** Where a value comes from. */
internal sealed interface Root {
    /** The message argument [name]. */
    data class Argument(val name: String) : Root

    /** The literal [value]. */
    data class Literal(val value: String) : Root

    /** No operand, as in `{:term …}`. */
    data object None : Root
}

/** The function of an expression. */
internal sealed interface FunctionRef {
    /** A supported [function]. */
    data class Known(val function: Mf2Function) : FunctionRef

    /** A function [name] the codegen does not support. */
    data class Unknown(val name: String) : FunctionRef
}

/**
 * An own option of an expression with a supported function.
 *
 * @property status how the function supports the option.
 * @property isDeclaredVariable whether the value is a variable declared in the message.
 * @property argument the message argument the option takes its value from, or `null` for a literal, a declared
 *   variable or an option that takes no argument.
 */
internal data class ResolvedOption(
    val name: String,
    val value: Operand,
    val status: OptionStatus,
    val isDeclaredVariable: Boolean,
) {
    val argument: MessageArgument?
        get() {
            val variable = value as? VariableOperand ?: return null
            val type = (status as? OptionStatus.Supported)?.spec?.variableType ?: return null
            return MessageArgument(variable.name, type).takeUnless { isDeclaredVariable }
        }
}

/** A message argument [name] of the Kotlin [type]. */
internal data class MessageArgument(val name: String, val type: ArgumentType)

/** How a function supports an option. */
internal sealed interface OptionStatus {
    /** The function supports the option as [spec] describes. */
    data class Supported(val spec: OptionSpec) : OptionStatus

    /** An option of the function that ICU4J 78.1 ignores. */
    data object IgnoredByIcu : OptionStatus

    /** A `u:` option, which has no effect on plain-text output. */
    data object Unsupported : OptionStatus

    /** An option the function does not have. */
    data object Unknown : OptionStatus
}

/**
 * A `.match` selector.
 *
 * @property value the declared value of the selector variable, or `null` if nothing declares it.
 * @property keys the key of this selector in every variant, in variant order.
 * @property hasUnknownFunction whether the declaration of the selector variable has a function the codegen does not
 *   support.
 * @property selection how the value selects, or `null` if it cannot select.
 * @property literalKeys the distinct literal keys in variant order.
 */
internal data class ResolvedSelector(
    val name: String,
    val value: Value?,
    val keys: List<VariantKey>,
    val hasUnknownFunction: Boolean = false,
) {
    val selection: Selection?
        get() = value?.function?.selection

    val literalKeys: List<String>
        get() = keys.filterIsInstance<LiteralKey>().map { it.value }.distinct()
}

/**
 * A use of the variable [name].
 *
 * @property isBound whether a declaration binds the name at this point.
 * @property isUsedBeforeDeclaration whether a later declaration declares the name.
 */
internal data class Reference(
    val name: String,
    val kind: ReferenceKind,
    val isBound: Boolean = false,
    val isUsedBeforeDeclaration: Boolean = false,
)

/** Where a variable name appears. */
internal enum class ReferenceKind {
    /** The name a declaration declares. */
    DECLARATION,

    /** The operand of an `.input` declaration, which is its own name. */
    INPUT_OPERAND,

    /** The operand of any other expression. */
    OPERAND,

    /** The value of an option. */
    OPTION_VALUE,

    /** A `.match` selector. */
    SELECTOR,
}

/** The time zone a date expression formats in. */
internal sealed interface EffectiveZone {
    /** The zone of the formatting context. */
    data object Context : EffectiveZone

    /** `timeZone=input`: the zone of the zoned operand. */
    data object Input : EffectiveZone

    /** A literal zone [zoneId], including `UTC`. */
    data class Fixed(val zoneId: String) : EffectiveZone

    /** `timeZone=$argument`: the `ZoneId` message argument [argument]. */
    data class Argument(val argument: String) : EffectiveZone
}
