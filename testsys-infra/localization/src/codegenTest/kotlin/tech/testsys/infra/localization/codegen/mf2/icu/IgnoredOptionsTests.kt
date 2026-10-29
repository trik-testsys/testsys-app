package tech.testsys.infra.localization.codegen.mf2.icu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.infra.localization.codegen.ruErrors

class IgnoredOptionsTests {

    @Test
    fun `should reject minimumIntegerDigits as not honoured`() {
        assertEquals(
            listOf("RU / task.a: option 'minimumIntegerDigits' of ':number' is not honoured by ICU4J 78.1"),
            ruErrors("task.a={\$x :number minimumIntegerDigits=3}"),
        )
    }

    @Test
    fun `should reject fraction digits on percent as not honoured`() {
        assertEquals(
            listOf("RU / task.a: option 'maximumFractionDigits' of ':percent' is not honoured by ICU4J 78.1"),
            ruErrors("task.a={\$x :percent maximumFractionDigits=0}"),
        )
    }

    @Test
    fun `should reject icu skeleton on percent as not honoured`() {
        assertEquals(
            listOf("RU / task.a: option 'icu:skeleton' of ':percent' is not honoured by ICU4J 78.1"),
            ruErrors("task.a={\$x :percent icu:skeleton=percent}"),
        )
    }
}
