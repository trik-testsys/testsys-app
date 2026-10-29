package tech.testsys.infra.localization.codegen.mf2.validator

import tech.testsys.infra.localization.codegen.mf2.function.Functions
import tech.testsys.infra.localization.codegen.mf2.function.Mf2Function
import tech.testsys.infra.localization.codegen.mf2.function.option.OptionNames
import tech.testsys.infra.localization.codegen.mf2.icu.IgnoredOptions
import tech.testsys.infra.localization.codegen.mf2.model.Declaration
import tech.testsys.infra.localization.codegen.mf2.model.Expression
import tech.testsys.infra.localization.codegen.mf2.model.FunctionRef
import tech.testsys.infra.localization.codegen.mf2.model.InputDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.LocalDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.Markup
import tech.testsys.infra.localization.codegen.mf2.model.MarkupOccurrence
import tech.testsys.infra.localization.codegen.mf2.model.Mf2Message
import tech.testsys.infra.localization.codegen.mf2.model.Occurrence
import tech.testsys.infra.localization.codegen.mf2.model.Operand
import tech.testsys.infra.localization.codegen.mf2.model.OptionStatus
import tech.testsys.infra.localization.codegen.mf2.model.Pattern
import tech.testsys.infra.localization.codegen.mf2.model.PatternMessage
import tech.testsys.infra.localization.codegen.mf2.model.PatternPart
import tech.testsys.infra.localization.codegen.mf2.model.Reference
import tech.testsys.infra.localization.codegen.mf2.model.ReferenceKind
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedExpression
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedOption
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedSelector
import tech.testsys.infra.localization.codegen.mf2.model.Root
import tech.testsys.infra.localization.codegen.mf2.model.SelectMessage
import tech.testsys.infra.localization.codegen.mf2.model.TextPart
import tech.testsys.infra.localization.codegen.mf2.model.Value
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand

/**
 * Builds the [ResolvedMessage] of a message in one walk: declarations, then selectors, then patterns. It reports
 * nothing: unknown functions and variables used before their declaration become facts of the model.
 */
internal object Resolver {

    /** Resolves [message]. */
    fun resolve(message: Mf2Message): ResolvedMessage = Walk(message).resolveMessage()

    // The scope grows with every declaration, so a declaration sees only the ones before it.
    private class Walk(private val message: Mf2Message) {
        private val scope = mutableMapOf<String, Value>()
        private val references = mutableListOf<Reference>()
        private val arguments = linkedSetOf<String>()
        private val unknownFunctionDeclarations = mutableSetOf<String>()

        fun resolveMessage(): ResolvedMessage {
            val declarations = message.declarations.mapIndexed(::declaration)
            val selectors = selectors()
            val patternParts = patterns().flatMap { pattern -> pattern.parts.mapNotNull(::patternPart) }
            return ResolvedMessage(message, declarations, selectors, patternParts, references.toList(), arguments.toList())
        }

        private fun declaration(index: Int, declaration: Declaration): ResolvedDeclaration {
            references += Reference(declaration.name, ReferenceKind.DECLARATION)
            val later = message.declarations.drop(index + 1).map { it.name }.toSet()
            val part = when (declaration) {
                is InputDeclaration -> expression(declaration.expression, later, isInput = true)
                is LocalDeclaration -> when (val value = declaration.value) {
                    is Markup -> MarkupOccurrence(value)
                    is Expression -> expression(value, later, isInput = false)
                }
            }
            if (part is ResolvedExpression) scope[declaration.name] = part.value
            if (part is ResolvedExpression && part.function is FunctionRef.Unknown) unknownFunctionDeclarations += declaration.name
            return ResolvedDeclaration(declaration.name, declaration is InputDeclaration, part)
        }

        private fun selectors(): List<ResolvedSelector> {
            val select = message as? SelectMessage ?: return emptyList()
            return select.selectors.mapIndexed { index, name ->
                references += Reference(name, ReferenceKind.SELECTOR, isBound = name in scope)
                ResolvedSelector(
                    name = name,
                    value = scope[name],
                    keys = select.variants.map { variant -> variant.keys[index] },
                    hasUnknownFunction = name in unknownFunctionDeclarations,
                )
            }
        }

        private fun patterns(): List<Pattern> = when (message) {
            is PatternMessage -> listOf(message.pattern)
            is SelectMessage -> message.variants.map { it.pattern }
        }

        private fun patternPart(part: PatternPart): Occurrence? = when (part) {
            is TextPart -> null
            is Markup -> MarkupOccurrence(part)
            is Expression -> expression(part, laterDeclarations = emptySet(), isInput = false)
        }

        private fun expression(expression: Expression, laterDeclarations: Set<String>, isInput: Boolean): ResolvedExpression {
            val operand = expression.operand
            val reference = (operand as? VariableOperand)?.let { operandReference(it.name, laterDeclarations, isInput) }
            val base = baseValue(operand)
            val call = expression.function ?: return ResolvedExpression(
                source = expression,
                operandReference = reference,
                base = base,
                function = null,
                options = emptyList(),
                value = base,
                zone = null,
            )
            val function = Functions.byName[call.name] ?: return ResolvedExpression(
                source = expression,
                operandReference = reference,
                base = base,
                function = FunctionRef.Unknown(call.name),
                options = emptyList(),
                value = base,
                zone = null,
            )
            val options = call.options.map { (name, value) -> option(function, name, value) }
            val combined = base.options + call.options
            val value = Value(base.root, function, combined, function.producesInteger(base, combined))
            return ResolvedExpression(
                source = expression,
                operandReference = reference,
                base = base,
                function = FunctionRef.Known(function),
                options = options,
                value = value,
                zone = function.effectiveZone(combined),
            )
        }

        private fun operandReference(name: String, laterDeclarations: Set<String>, isInput: Boolean): Reference {
            val isBound = name in scope
            val kind = if (isInput) ReferenceKind.INPUT_OPERAND else ReferenceKind.OPERAND
            val reference = Reference(name, kind, isBound, isUsedBeforeDeclaration = name in laterDeclarations)
            references += reference
            // Arguments follow source order even though option values are typed before the operand.
            if (!isBound) arguments += name
            return reference
        }

        private fun baseValue(operand: Operand?): Value = when (operand) {
            is VariableOperand -> scope[operand.name] ?: Value(Root.Argument(operand.name))
            is LiteralOperand -> Value(Root.Literal(operand.value))
            null -> Value(Root.None)
        }

        private fun option(function: Mf2Function, name: String, value: Operand): ResolvedOption {
            val status = status(function, name)
            val variable = (value as? VariableOperand)?.name
            val isDeclared = variable != null && variable in scope
            val resolved = ResolvedOption(name, value, status, isDeclaredVariable = isDeclared)
            if (variable != null && status is OptionStatus.Supported) {
                references += Reference(variable, ReferenceKind.OPTION_VALUE, isBound = isDeclared)
                resolved.argument?.let { arguments += it.name }
            }
            return resolved
        }

        private fun status(function: Mf2Function, name: String): OptionStatus {
            val spec = function.options[name]
            return when {
                name.startsWith(OptionNames.U_NAMESPACE) -> OptionStatus.Unsupported
                name in IgnoredOptions.of(function) -> OptionStatus.IgnoredByIcu
                spec != null -> OptionStatus.Supported(spec)
                else -> OptionStatus.Unknown
            }
        }
    }
}
