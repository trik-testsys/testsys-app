package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class SelectorAnnotationRuleTests {

    @Test
    fun `should reject a selector without an annotation`() {
        assertEquals(
            listOf("RU / task.a: the selector '\$x' has no annotation; declare it with '.input {\$x :<function>}'"),
            ruErrors("task.a=.match \$x a {{a}} * {{b}}"),
        )
    }

    @Test
    fun `should reject a function that cannot select`() {
        assertEquals(
            listOf(
                "RU / task.a: ':date' cannot be used as a selector: only :string, :integer, :number, :percent, :offset " +
                    "select reliably in ICU4J 78.1",
            ),
            ruErrors("task.a=.input {\$d :date} .match \$d a {{a}} * {{b}}"),
        )
    }

    @Test
    fun `should report only the unknown function if the selector is declared with one`() {
        assertEquals(
            listOf("RU / task.a: unknown function ':interger'"),
            ruErrors("task.a=.input {\$x :interger} .match \$x a {{a}} * {{b}}"),
        )
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            ".input {\$s :string} .match \$s a {{a}} * {{b}}",
            ".input {\$n :integer} .match \$n * {{x}}",
            ".input {\$n :integer} .local \$o = {\$n :offset subtract=1} .match \$o * {{x}}",
        ],
    )
    fun `should accept a selector annotated with a function that can select`(message: String) {
        val problems = SelectorAnnotationRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }
}
