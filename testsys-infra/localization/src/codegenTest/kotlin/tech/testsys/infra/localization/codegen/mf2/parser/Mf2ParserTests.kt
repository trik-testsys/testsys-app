package tech.testsys.infra.localization.codegen.mf2.parser

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.model.Attribute
import tech.testsys.infra.localization.codegen.mf2.model.CatchAllKey
import tech.testsys.infra.localization.codegen.mf2.model.Expression
import tech.testsys.infra.localization.codegen.mf2.model.FunctionCall
import tech.testsys.infra.localization.codegen.mf2.model.InputDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.LiteralKey
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.LocalDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.Markup
import tech.testsys.infra.localization.codegen.mf2.model.Pattern
import tech.testsys.infra.localization.codegen.mf2.model.PatternMessage
import tech.testsys.infra.localization.codegen.mf2.model.SelectMessage
import tech.testsys.infra.localization.codegen.mf2.model.TextPart
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand
import tech.testsys.infra.localization.codegen.mf2.model.Variant

class Mf2ParserTests {

    @Test
    fun `should parse a local declaration and a reference to it`() {
        val message = Mf2Parser.parse(".local \$shifted = {\$count :offset subtract=1} {{{\$shifted}}}")

        assertEquals(
            PatternMessage(
                declarations = listOf(
                    LocalDeclaration(
                        "shifted",
                        Expression(VariableOperand("count"), FunctionCall("offset", mapOf("subtract" to LiteralOperand("1"))), emptyList()),
                    ),
                ),
                pattern = Pattern(listOf(Expression(VariableOperand("shifted"), null, emptyList()))),
            ),
            message,
        )
    }

    @Test
    fun `should parse a literal expression operand`() {
        val message = Mf2Parser.parse("{|12.5| :number}")

        assertEquals(
            PatternMessage(
                declarations = emptyList(),
                pattern = Pattern(listOf(Expression(LiteralOperand("12.5"), FunctionCall("number", emptyMap()), emptyList()))),
            ),
            message,
        )
    }

    @Test
    fun `should parse a function without an operand`() {
        val message = Mf2Parser.parse("{:term name=task case=gen number=pl}")

        assertEquals(
            PatternMessage(
                emptyList(),
                Pattern(
                    listOf(
                        Expression(
                            null,
                            FunctionCall(
                                name = "term",
                                options = mapOf(
                                    "name" to LiteralOperand("task"),
                                    "case" to LiteralOperand("gen"),
                                    "number" to LiteralOperand("pl"),
                                ),
                            ),
                            emptyList(),
                        ),
                    ),
                ),
            ),
            message,
        )
    }

    @Test
    fun `should parse literal and variable option values`() {
        val message = Mf2Parser.parse("{\$amount :number notation=compact maximumFractionDigits=\$digits}")

        assertEquals(
            PatternMessage(
                emptyList(),
                Pattern(
                    listOf(
                        Expression(
                            VariableOperand("amount"),
                            FunctionCall(
                                name = "number",
                                options = mapOf(
                                    "notation" to LiteralOperand("compact"),
                                    "maximumFractionDigits" to VariableOperand("digits"),
                                ),
                            ),
                            emptyList(),
                        ),
                    ),
                ),
            ),
            message,
        )
    }

    @Test
    fun `should parse attributes with and without values`() {
        val message = Mf2Parser.parse("{\$name @hidden @label=|User name|}")

        assertEquals(
            PatternMessage(
                emptyList(),
                Pattern(
                    listOf(
                        Expression(
                            operand = VariableOperand("name"),
                            function = null,
                            attributes = listOf(Attribute(name = "hidden", value = null), Attribute(name = "label", value = "User name")),
                        ),
                    ),
                ),
            ),
            message,
        )
    }

    @Test
    fun `should parse opening closing and standalone markup`() {
        val message = Mf2Parser.parse("{#b}text{/b}{#br/}")

        assertEquals(
            PatternMessage(emptyList(), Pattern(listOf(Markup("b"), TextPart("text"), Markup("b"), Markup("br")))),
            message,
        )
    }

    @Test
    fun `should resolve escapes in text`() {
        val message = Mf2Parser.parse("{{\\{name\\} \\\\}}")

        assertEquals(PatternMessage(emptyList(), Pattern(listOf(TextPart("{name} \\")))), message)
    }

    @Test
    fun `should report the first line of the ICU explanation of a syntax error`() {
        val error = assertThrows(Mf2SyntaxException::class.java) { Mf2Parser.parse("{\$x") }

        assertEquals("syntax error: Parse error [3]: Space expected", error.message)
    }

    @Test
    fun `should reject an option variable shared by two declarations (ICU4J 78_1)`() {
        val error = assertThrows(Mf2SyntaxException::class.java) {
            Mf2Parser.parse(".input {\$x :number maximumFractionDigits=\$p} .input {\$y :number maximumFractionDigits=\$p} {{{\$x} {\$y}}}")
        }

        assertEquals("syntax error: Variable 'p' already declared", error.message)
    }

    @Test
    fun `should parse a pattern message into text and placeholders`() {
        val message = Mf2Parser.parse("Привет, {\$name}!")

        assertEquals(
            PatternMessage(
                declarations = emptyList(),
                pattern = Pattern(
                    listOf(TextPart("Привет, "), Expression(VariableOperand("name"), null, emptyList()), TextPart("!")),
                ),
            ),
            message,
        )
    }

    @Test
    fun `should parse a select message into declarations, selectors and variants`() {
        val message = Mf2Parser.parse(".input {\$n :integer} .match \$n one {{Один}} * {{Много}}")

        assertEquals(
            SelectMessage(
                declarations = listOf(
                    InputDeclaration("n", Expression(VariableOperand("n"), FunctionCall("integer", emptyMap()), emptyList())),
                ),
                selectors = listOf("n"),
                variants = listOf(
                    Variant(listOf(LiteralKey("one")), Pattern(listOf(TextPart("Один")))),
                    Variant(listOf(CatchAllKey), Pattern(listOf(TextPart("Много")))),
                ),
            ),
            message,
        )
    }
}
