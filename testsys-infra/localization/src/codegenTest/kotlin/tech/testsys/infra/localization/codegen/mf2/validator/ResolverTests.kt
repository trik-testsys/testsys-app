package tech.testsys.infra.localization.codegen.mf2.validator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.function.ArgumentType
import tech.testsys.infra.localization.codegen.mf2.function.NumberFunction
import tech.testsys.infra.localization.codegen.mf2.function.OffsetFunction
import tech.testsys.infra.localization.codegen.mf2.function.Selection
import tech.testsys.infra.localization.codegen.mf2.model.EffectiveZone
import tech.testsys.infra.localization.codegen.mf2.model.Expression
import tech.testsys.infra.localization.codegen.mf2.model.FunctionCall
import tech.testsys.infra.localization.codegen.mf2.model.FunctionRef
import tech.testsys.infra.localization.codegen.mf2.model.InputDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.LiteralOperand
import tech.testsys.infra.localization.codegen.mf2.model.LocalDeclaration
import tech.testsys.infra.localization.codegen.mf2.model.MessageArgument
import tech.testsys.infra.localization.codegen.mf2.model.Pattern
import tech.testsys.infra.localization.codegen.mf2.model.PatternMessage
import tech.testsys.infra.localization.codegen.mf2.model.Reference
import tech.testsys.infra.localization.codegen.mf2.model.ReferenceKind
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedMessage
import tech.testsys.infra.localization.codegen.mf2.model.ResolvedOption
import tech.testsys.infra.localization.codegen.mf2.model.Root
import tech.testsys.infra.localization.codegen.mf2.model.Value
import tech.testsys.infra.localization.codegen.mf2.model.VariableOperand
import tech.testsys.infra.localization.codegen.mf2.parser.Mf2Parser

// The messages are the messages of the example set with the same key.
class ResolverTests {

    private fun resolve(source: String): ResolvedMessage = Resolver.resolve(Mf2Parser.parse(source))

    @Test
    fun `should resolve an option value to a message argument after the operand in task score average`() {
        val message = resolve("Средний балл: {\$average :number maximumFractionDigits=\$precision}")

        val expression = message.expressions.single()
        assertEquals(listOf("average", "precision"), message.arguments)
        assertEquals(
            Value(Root.Argument("average"), NumberFunction, mapOf("maximumFractionDigits" to VariableOperand("precision"))),
            expression.value,
        )
        assertEquals(MessageArgument("precision", ArgumentType.INT), expression.options.single().argument)
    }

    @Test
    fun `should give the offset selector the declared integer operand and its shift in contest participants`() {
        val message = resolve(
            ".input {\$count :integer} .local \$others = {\$count :offset subtract=1} .match \$others " +
                "0 {{Только вы}} one {{Вы и ещё {\$others} участник}} few {{Вы и ещё {\$others} участника}} " +
                "many {{Вы и ещё {\$others} участников}} * {{Вы и ещё {\$others} участника}}",
        )

        val selector = message.selectors.single()
        assertEquals(listOf("count"), message.arguments)
        assertEquals(
            Value(Root.Argument("count"), OffsetFunction, mapOf("subtract" to LiteralOperand("1")), isIntegerOperand = true),
            selector.value,
        )
        assertEquals(Selection.NUMERIC, selector.selection)
    }

    @Test
    fun `should format the date in the zone of the operand in tour start venue`() {
        val message = resolve("Начало тура по времени площадки: {\$start :datetime timeZone=input}")

        assertEquals(mapOf("start" to setOf(EffectiveZone.Input)), message.argumentZones)
    }

    @Test
    fun `should bind a local literal without making it an argument in resource download`() {
        val message = resolve(
            ".local \$brand = {|TRIK Studio| @translate=no} {{Скачайте {\$brand}, чтобы открыть решение}}",
        )

        assertEquals(emptyList<String>(), message.arguments)
        assertEquals(
            listOf(Reference("brand", ReferenceKind.DECLARATION), Reference("brand", ReferenceKind.OPERAND, isBound = true)),
            message.references,
        )
        assertEquals(Value(Root.Literal("TRIK Studio")), message.expressions.last().value)
    }

    @Test
    fun `should list the arguments in first-use order across declarations and variants in user solved`() {
        val message = resolve(
            ".input {\$gender :string} .input {\$count :integer} .match \$gender " +
                "female {{{\$name} решила {\$count} {\$count :term name=task case=acc}}} " +
                "male {{{\$name} решил {\$count} {\$count :term name=task case=acc}}} " +
                "* {{Пользователь {\$name} решил {\$count} {\$count :term name=task case=acc}}}",
        )

        assertEquals(listOf("gender", "count", "name"), message.arguments)
    }

    @Test
    fun `should let an option of the expression override the same option of the declaration`() {
        val message = resolve(".input {\$x :number maximumFractionDigits=1} {{{\$x :number maximumFractionDigits=2}}}")

        assertEquals(mapOf("maximumFractionDigits" to LiteralOperand("2")), message.expressions.last().value.options)
    }

    @Test
    fun `should resolve an unknown function to the value of its operand`() {
        val message = resolve("{\$x :money foo=bar}")

        val expression = message.expressions.single()
        assertEquals(FunctionRef.Unknown("money"), expression.function)
        assertEquals(Value(Root.Argument("x")), expression.value)
        assertEquals(emptyList<ResolvedOption>(), expression.options)
    }

    // ICU's parser rejects such a message, so the model is built directly.
    @Test
    fun `should mark an operand that a later declaration declares as used before its declaration`() {
        val message = PatternMessage(
            declarations = listOf(
                LocalDeclaration("a", Expression(VariableOperand("b"), null, emptyList())),
                InputDeclaration("b", Expression(VariableOperand("b"), FunctionCall("integer", emptyMap()), emptyList())),
            ),
            pattern = Pattern(listOf(Expression(VariableOperand("a"), null, emptyList()))),
        )

        val resolved = Resolver.resolve(message)

        assertEquals(
            Reference("b", ReferenceKind.OPERAND, isUsedBeforeDeclaration = true),
            resolved.expressions.first().operandReference,
        )
    }
}
