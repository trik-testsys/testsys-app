package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class OperandRuleTests {

    @Test
    fun `should reject a function without an operand`() {
        assertEquals(listOf("RU / task.a: ':number' needs an operand"), ruErrors("task.a={:number}"))
    }

    @Test
    fun `should reject a literal operand that is not a number`() {
        assertEquals(
            listOf("RU / task.a: the literal operand '|много|' of ':integer' is not a number"),
            ruErrors("task.a={|много| :integer}"),
        )
    }

    @Test
    fun `should reject a literal operand of currency without the option currency`() {
        assertEquals(listOf("RU / task.a: a literal operand of ':currency' needs the option 'currency'"), ruErrors("task.a={5 :currency}"))
    }

    @Test
    fun `should reject a literal date operand that is not ISO 8601`() {
        assertEquals(
            listOf("RU / task.a: the literal operand '|09.10.2025|' of ':date' is not an ISO 8601 date"),
            ruErrors("task.a={|09.10.2025| :date}"),
        )
    }

    @Test
    fun `should reject a literal date operand formatted in the zone of the operand`() {
        assertEquals(
            listOf("RU / task.a: timeZone=input needs a variable operand"),
            ruErrors("task.a={|2025-10-09| :date timeZone=input}"),
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["{\$x :number}", "{5 :integer}", "{|2.5| :number}", "{|2025-10-09| :date}", "{5 :currency currency=EUR}"])
    fun `should accept a variable operand or a literal of the operand type`(message: String) {
        val problems = OperandRule.problemsIn(message)

        assertEquals(emptyList<String>(), problems)
    }
}
