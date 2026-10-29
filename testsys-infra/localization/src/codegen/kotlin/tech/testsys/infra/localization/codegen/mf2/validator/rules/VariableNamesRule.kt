package tech.testsys.infra.localization.codegen.mf2.validator.rules

import tech.testsys.infra.localization.codegen.Problems
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.validator.MessageRule
import tech.testsys.infra.localization.codegen.mf2.validator.RuleContext

/** Rejects a variable name that cannot be a Kotlin parameter name: input variables become method parameters. */
internal object VariableNamesRule : MessageRule {
    private val shape = Regex("[a-z][A-Za-z0-9]*")
    private val kotlinKeywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
        "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
        "val", "var", "when", "while",
    )

    override fun findProblems(message: ResolvedMessage, context: RuleContext): List<String> = message.references
        .map { it.name }
        .distinct()
        .filter { !shape.matches(it) || it in kotlinKeywords }
        .map(Problems.Variables::name)
}
