package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.mf2.validator.problemsIn
import tech.testsys.infra.localization.codegen.ruErrors

class UnitUsageRuleTests {

    @Test
    fun `should reject a usage that the unit category does not define`() {
        assertEquals(
            listOf(
                "RU / task.a: usage 'banana' is not defined for the unit category 'length' (known: default, focal-length, person, " +
                    "person-height, rainfall, road, snowfall, vehicle, visiblty)",
            ),
            ruErrors("task.a={\$m :unit unit=meter usage=banana}"),
        )
    }

    @Test
    fun `should accept a usage that the unit category defines`() {
        val problems = UnitUsageRule.problemsIn("{\$m :unit unit=meter usage=road}")

        assertEquals(emptyList<String>(), problems)
    }
}
