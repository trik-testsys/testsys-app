package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.model.Expression
import tech.testsys.infra.localization.codegen.mf2.model.FunctionCall
import tech.testsys.infra.localization.codegen.mf2.model.InputDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.LocalDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.Pattern
import tech.testsys.infra.localization.codegen.mf2.model.PatternMessage
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn

class DeclarationOrderRuleTests {

    @Test
    fun `should accept an operand declared before its use`() {
        val problems = DeclarationOrderRule.problemsIn(".input {\$n :integer} .local \$o = {\$n :offset subtract=1} {{{\$o}}}")

        assertEquals(emptyList<String>(), problems)
    }

    // ICU4J rejects this order while parsing, so the message is built directly.
    @Test
    fun `should reject an operand that a later declaration declares`() {
        val message = PatternMessage(
            declarations = listOf(
                LocalDeclaration("a", Expression(VariableOperand("b"), null, emptyList())),
                InputDeclaration("b", Expression(VariableOperand("b"), FunctionCall("integer", emptyMap()), emptyList())),
            ),
            pattern = Pattern(listOf(Expression(VariableOperand("a"), null, emptyList()))),
        )

        val problems = DeclarationOrderRule.problemsIn(message)

        assertEquals(listOf("'\$b' is used before its declaration"), problems)
    }
}
