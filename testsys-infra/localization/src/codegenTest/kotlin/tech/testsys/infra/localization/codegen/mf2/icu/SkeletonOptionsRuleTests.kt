package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class SkeletonOptionsRuleTests {

    @Test
    fun `should reject standard date options together with icu skeleton`() {
        assertEquals(
            listOf(
                "RU / task.a: option 'fields' of ':date' is not honoured by ICU4J 78.1 together with 'icu:skeleton'",
                "RU / task.a: option 'length' of ':date' is not honoured by ICU4J 78.1 together with 'icu:skeleton'",
            ),
            ruErrors("task.a={\$d :date fields=month-day length=long icu:skeleton=yMMMM}"),
        )
    }

    @Test
    fun `should accept icu skeleton without standard date options`() {
        val problems = SkeletonOptionsRule.problemsIn("{\$d :date icu:skeleton=yMMMM}")

        assertEquals(emptyList<String>(), problems)
    }
}
