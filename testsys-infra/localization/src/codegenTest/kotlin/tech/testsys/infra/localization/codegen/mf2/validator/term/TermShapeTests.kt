package tech.testsys.infra.localization.codegen.mf2.validator.term

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.RU_TASK_TERM
import tech.testsys.infra.localization.codegen.mf2.parser.Mf2Parser
import tech.testsys.infra.localization.codegen.mf2.validator.RU_LOCALE
import tech.testsys.infra.localization.codegen.ruErrors

class TermShapeTests {

    private val termHeader = "glossary.task=.input {\$case :string} .input {\$form :string} .match \$case \$form "

    @Test
    fun `should reject a term that does not select on case and form`() {
        assertEquals(
            listOf(
                "RU / glossary.task: a term must declare exactly '.input {\$case :string}' and '.input {\$form :string}' " +
                    "and select with '.match \$case \$form'",
            ),
            ruErrors(
                "glossary.task=.input {\$case :string} .match \$case nom {{Задача}} * {{Задача}}",
                "task.a={:term name=task case=nom number=sg}",
            ),
        )
    }

    @Test
    fun `should reject a form key that is neither a category nor sg or pl`() {
        assertEquals(
            listOf("RU / glossary.task: form key 'dual' is not one of few, many, one, other, pl, sg"),
            ruErrors(termHeader + "nom sg {{Задача}} nom dual {{Задачи}} * * {{Задача}}", "task.a={:term name=task case=nom number=sg}"),
        )
    }

    @Test
    fun `should reject a case key that is not an identifier`() {
        assertEquals(
            listOf("RU / glossary.task: case key 'Nom' must match [a-z][a-z0-9_]*"),
            ruErrors(termHeader + "Nom sg {{Задача}} * * {{Задача}}", "task.a={:term name=task case=nom number=sg}"),
        )
    }

    @Test
    fun `should reject a placeholder in a term variant`() {
        assertEquals(
            listOf("RU / glossary.task: the variant 'nom sg' must be plain text: a term has no placeholders or markup"),
            ruErrors(termHeader + "nom sg {{Задача {\$case}}} * * {{Задача}}", "task.a={:term name=task case=nom number=sg}"),
        )
    }

    @Test
    fun `should reject a glossary key with more than one segment`() {
        assertEquals(
            listOf("RU / glossary.task.extra: a glossary key must be 'glossary.<name>' with a name matching [a-z][a-z0-9_]*"),
            ruErrors("glossary.task.extra={{Задача}}"),
        )
    }

    @Test
    fun `should read the cases and cells of a well-shaped term`() {
        val message = Mf2Parser.parse(RU_TASK_TERM.substringAfter('='))

        val result = TermShape(RU_LOCALE).parseDefinition("glossary.task", message)

        assertEquals(RU_TASK_DEFINITION to emptyList<String>(), result)
    }
}
