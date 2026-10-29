package tech.testsys.infra.localization.codegen.mf2.model

/**
 * The codegen's own model of one MF2 message, converted from ICU's technology-preview data model by the parser facade.
 * Validators work only with this model, so ICU's `MFDataModel` stays confined to one file.
 */
internal sealed interface Mf2Message {
    val declarations: List<Declaration>
}

/** A message without `.match`: declarations and one pattern. */
internal data class PatternMessage(
    override val declarations: List<Declaration>,
    val pattern: Pattern,
) : Mf2Message

/** A message with `.match`: declarations, selector variable names and variants. */
internal data class SelectMessage(
    override val declarations: List<Declaration>,
    val selectors: List<String>,
    val variants: List<Variant>,
) : Mf2Message

/** A `.input` or `.local` declaration of the variable [name]. */
internal sealed interface Declaration {
    val name: String
}

/** `.input {$name …}`; [expression] always has the variable [name] as its operand. */
internal data class InputDeclaration(override val name: String, val expression: Expression) : Declaration

/** `.local $name = {…}`; [value] may be markup, which validators reject. */
internal data class LocalDeclaration(override val name: String, val value: Placeholder) : Declaration

/** A sequence of text and placeholders. */
internal data class Pattern(val parts: List<PatternPart>)

/** A part of a [Pattern]. */
internal sealed interface PatternPart

/** Literal text with MF2 escapes already resolved. */
internal data class TextPart(val text: String) : PatternPart

/** A `{…}` placeholder: an expression or markup. */
internal sealed interface Placeholder : PatternPart

/** An expression `{operand :function options @attributes}`; [operand] is `null` for `{:function}`. */
internal data class Expression(
    val operand: Operand?,
    val function: FunctionCall?,
    val attributes: List<Attribute>,
) : Placeholder

/** Markup `{#name}`, `{/name}` or `{#name/}`. */
internal data class Markup(val name: String) : Placeholder

/** An expression operand or an option value. */
internal sealed interface Operand

/** A literal `|text|` or `text`. */
internal data class LiteralOperand(val value: String) : Operand

/** A variable reference `$name`. */
internal data class VariableOperand(val name: String) : Operand

/** A function annotation `:name` with [options] in source order. */
internal data class FunctionCall(val name: String, val options: Map<String, Operand>)

/** An attribute `@name` or `@name=value`. */
internal data class Attribute(val name: String, val value: String?)

/** A variant of a [SelectMessage]: one key per selector. */
internal data class Variant(val keys: List<VariantKey>, val pattern: Pattern)

/** A variant key. */
internal sealed interface VariantKey

/** A literal key such as `one` or `0`. */
internal data class LiteralKey(val value: String) : VariantKey

/** The catch-all key `*`. */
internal data object CatchAllKey : VariantKey
