package tech.testsys.infra.localization.codegen.mf2.validator.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class DateZoneRuleTests {

    @Test
    fun `should reject one date argument formatted in different zones`() {
        assertEquals(
            listOf(
                "RU / task.a: the date argument '\$d' is formatted in different time zones (the context zone, timeZone=|UTC|); " +
                    "use one zone per argument",
            ),
            ruErrors("task.a={\$d :datetime} {\$d :time precision=minute timeZone=UTC}"),
        )
    }

    @Test
    fun `should accept one date argument formatted twice in the same zone`() {
        val problems = DateZoneRule.problemsIn("{\$d :datetime timeZone=UTC} {\$d :time precision=minute timeZone=UTC}")

        assertEquals(emptyList<String>(), problems)
    }
}
