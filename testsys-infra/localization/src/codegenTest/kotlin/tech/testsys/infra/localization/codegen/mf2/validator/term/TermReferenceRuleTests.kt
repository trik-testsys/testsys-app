package tech.testsys.infra.localization.codegen.mf2.validator.term

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.RU_TASK_TERM
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.mf2.validator.ruContext
import tech.testsys.infra.localization.codegen.ruErrors

class TermReferenceRuleTests {

    private val termHeader = "glossary.task=.input {\$case :string} .input {\$form :string} .match \$case \$form "

    @Test
    fun `should reject a counted reference if a reachable category cell is missing`() {
        assertEquals(
            listOf("RU / task.a: the term 'task' has no explicit variant 'nom few' needed by this reference"),
            ruErrors(
                termHeader + "nom one {{Задача}} nom many {{Задач}} * * {{Задача}}",
                "task.a={\$n :integer} {\$n :term name=task case=nom}",
            ),
        )
    }

    @Test
    fun `should reject a bare reference if its number cell is missing`() {
        assertEquals(
            listOf("RU / task.a: the term 'task' has no explicit variant 'nom pl' needed by this reference"),
            ruErrors(termHeader + "nom sg {{Задача}} * * {{Задача}}", "task.a={:term name=task case=nom number=pl}"),
        )
    }

    @Test
    fun `should reject an unknown case`() {
        assertEquals(
            listOf("RU / task.a: ':term' case 'gen' is not a case of the term 'task' (known: acc, nom)"),
            ruErrors(RU_TASK_TERM, "task.a={:term name=task case=gen number=sg}"),
        )
    }

    @Test
    fun `should reject an unknown term`() {
        assertEquals(
            listOf("RU / task.a: ':term' refers to the unknown term 'user'"),
            ruErrors("task.a={:term name=user case=nom number=sg}"),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\$n :integer} {\$n :term name=task case=nom}", "{:term name=task case=acc number=pl}"])
    fun `should accept a reference to a known case whose variants exist`(message: String) {
        val context = ruContext(mapOf("task" to RU_TASK_DEFINITION))

        val problems = TermReferenceRule.problemsIn(message, context)

        assertEquals(emptyList<String>(), problems)
    }
}
