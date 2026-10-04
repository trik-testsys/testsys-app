package tech.testsys.infra.localization.codegen.mf2.validator.term

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.RU_TASK_TERM
import tech.testsys.infra.localization.codegen.mf2.validator.RU_LOCALE
import tech.testsys.infra.localization.codegen.ruEnErrors
import tech.testsys.infra.localization.codegen.ruErrors

class GlossaryTests {

    @Test
    fun `should reject an unused term`() {
        assertEquals(listOf("RU / glossary.task: the term 'task' is never used"), ruErrors(RU_TASK_TERM, "task.a=Текст"))
    }

    @Test
    fun `should check each region against its own case set`() {
        val en = listOf(
            "glossary.task=.input {\$case :string} .input {\$form :string} .match \$case \$form " +
                "nom one {{task}} nom other {{tasks}} nom sg {{task}} nom pl {{tasks}} * * {{task}}",
            "task.a={\$n :integer} {\$n :term name=task case=acc}",
        )

        val errors = ruEnErrors(listOf(RU_TASK_TERM, "task.a={\$n :integer} {\$n :term name=task case=acc}"), en)

        assertEquals(listOf("EN / task.a: ':term' case 'acc' is not a case of the term 'task' (known: nom)"), errors)
    }

    @Test
    fun `should accept a term that a message mentions`() {
        val glossary = Glossary(
            RU_LOCALE,
            terms = mapOf("task" to RU_TASK_DEFINITION),
            brokenTerms = emptySet(),
            patterns = emptyMap(),
        )

        val errors = glossary.unusedErrors("RU", mentioned = setOf("task"))

        assertEquals(emptyList<String>(), errors)
    }
}
