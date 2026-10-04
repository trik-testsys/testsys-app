package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class DateOptionPairRuleTests {

    @Test
    fun `should reject an incomplete date pair`() {
        assertEquals(
            listOf("RU / task.a: option 'fields' of ':date' is not honoured by ICU4J 78.1 without 'length'"),
            ruErrors("task.a={\$d :date fields=month-day}"),
        )
    }

    @Test
    fun `should reject a date length without fields`() {
        assertEquals(
            listOf("RU / task.a: option 'length' of ':date' is not honoured by ICU4J 78.1 without 'fields'"),
            ruErrors("task.a={\$d :date length=long}"),
        )
    }

    @Test
    fun `should reject an incomplete date pair of datetime`() {
        assertEquals(
            listOf("RU / task.a: option 'dateLength' of ':datetime' is not honoured by ICU4J 78.1 without 'dateFields'"),
            ruErrors("task.a={\$d :datetime dateLength=long}"),
        )
    }

    @Test
    fun `should accept a complete date pair`() {
        val problems = DateOptionPairRule.problemsIn("{\$d :date fields=month-day length=long}")

        assertEquals(emptyList<String>(), problems)
    }

    @Test
    fun `should accept a complete date pair of datetime`() {
        val problems = DateOptionPairRule.problemsIn("{\$d :datetime dateFields=month-day dateLength=long}")

        assertEquals(emptyList<String>(), problems)
    }
}
