// MFParser and MFDataModel are an ICU technology preview (@Deprecated @internal). This facade is the only codegen
// file allowed to touch them; everything else works with the model in mf2/model.
@file:Suppress("DEPRECATION", "Deprecation")

package tech.testsys.infra.localization.codegen.mf2.parser

import com.ibm.icu.message2.MFDataModel
import com.ibm.icu.message2.MFParseException
import com.ibm.icu.message2.MFParser
import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.model.Attribute
import tech.testsys.infra.localization.codegen.mf2.model.CatchAllKey
import tech.testsys.infra.localization.codegen.mf2.model.Declaration
import tech.testsys.infra.localization.codegen.mf2.model.Expression
import tech.testsys.infra.localization.codegen.mf2.model.FunctionCall
import tech.testsys.infra.localization.codegen.mf2.model.InputDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.LiteralKey
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.LocalDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.Markup
import tech.testsys.infra.localization.codegen.mf2.model.Mf2Message
import tech.testsys.infra.localization.codegen.mf2.model.Operand
import tech.testsys.infra.localization.codegen.mf2.model.Pattern
import tech.testsys.infra.localization.codegen.mf2.model.PatternMessage
import tech.testsys.infra.localization.codegen.mf2.model.Placeholder
import tech.testsys.infra.localization.codegen.mf2.model.SelectMessage
import tech.testsys.infra.localization.codegen.mf2.model.TextPart
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand
import tech.testsys.infra.localization.codegen.mf2.model.Variant

/** A message that ICU's MF2 parser rejects; the message is `syntax error: ` followed by ICU's first line of explanation. */
internal class Mf2SyntaxException(message: String) : IllegalArgumentException(message)

/**
 * Facade over ICU's MF2 parser: parses a message and converts ICU's data model into [Mf2Message].
 */
internal object Mf2Parser {

    /** Parses [source]; throws [Mf2SyntaxException] if ICU rejects it. */
    fun parse(source: String): Mf2Message {
        val message = try {
            MFParser.parse(source)
        } catch (e: MFParseException) {
            throw Mf2SyntaxException(Problems.Syntax.syntaxError(e.message.orEmpty().lineSequence().first()))
        }
        return when (message) {
            is MFDataModel.PatternMessage -> PatternMessage(message.declarations.map(::declaration), pattern(message.pattern))
            is MFDataModel.SelectMessage -> SelectMessage(
                declarations = message.declarations.map(::declaration),
                selectors = message.selectors.map(::selectorName),
                variants = message.variants.map(::variant),
            )
            else -> error("Unexpected MF2 message type ${message.javaClass.name}")
        }
    }

    private fun declaration(declaration: MFDataModel.Declaration): Declaration = when (declaration) {
        is MFDataModel.InputDeclaration -> InputDeclaration(declaration.name, expression(declaration.value))
        is MFDataModel.LocalDeclaration -> LocalDeclaration(declaration.name, placeholder(declaration.value))
        else -> error("Unexpected MF2 declaration type ${declaration.javaClass.name}")
    }

    private fun selectorName(selector: MFDataModel.Expression): String = (selector as? MFDataModel.VariableExpression)?.arg?.name
        ?: error("Unexpected MF2 selector type ${selector.javaClass.name}")

    private fun variant(variant: MFDataModel.Variant): Variant = Variant(
        keys = variant.keys.map { key ->
            when (key) {
                is MFDataModel.CatchallKey -> CatchAllKey
                is MFDataModel.Literal -> LiteralKey(key.value)
                else -> error("Unexpected MF2 key type ${key.javaClass.name}")
            }
        },
        pattern = pattern(variant.value),
    )

    private fun pattern(pattern: MFDataModel.Pattern): Pattern = Pattern(
        pattern.parts.map { part ->
            when (part) {
                is MFDataModel.StringPart -> TextPart(part.value)
                is MFDataModel.Expression -> placeholder(part)
                else -> error("Unexpected MF2 pattern part ${part.javaClass.name}")
            }
        },
    )

    // Markup implements MFDataModel.Expression too, so it is checked first.
    private fun placeholder(expression: MFDataModel.Expression): Placeholder = when (expression) {
        is MFDataModel.Markup -> Markup(expression.name)
        else -> expression(expression)
    }

    private fun expression(expression: MFDataModel.Expression): Expression = when (expression) {
        is MFDataModel.VariableExpression -> Expression(
            VariableOperand(expression.arg.name),
            expression.function?.let(::functionCall),
            expression.attributes.map(::attribute),
        )
        is MFDataModel.LiteralExpression -> Expression(
            LiteralOperand(expression.arg.value),
            expression.function?.let(::functionCall),
            expression.attributes.map(::attribute),
        )
        is MFDataModel.FunctionExpression -> Expression(
            null,
            functionCall(expression.function),
            expression.attributes.map(::attribute),
        )
        else -> error("Unexpected MF2 expression type ${expression.javaClass.name}")
    }

    private fun functionCall(function: MFDataModel.FunctionRef): FunctionCall =
        FunctionCall(function.name, function.options.values.associate { it.name to operand(it.value) })

    private fun operand(value: MFDataModel.LiteralOrVariableRef): Operand = when (value) {
        is MFDataModel.Literal -> LiteralOperand(value.value)
        is MFDataModel.VariableRef -> VariableOperand(value.name)
        else -> error("Unexpected MF2 operand type ${value.javaClass.name}")
    }

    private fun attribute(attribute: MFDataModel.Attribute): Attribute =
        Attribute(attribute.name, (attribute.value as? MFDataModel.Literal)?.value)
}
