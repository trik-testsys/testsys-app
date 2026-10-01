package tech.testsys.web.ui.display

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tech.testsys.web.ui.DataHandle
import tech.testsys.web.ui.MockVaadinTests
import tech.testsys.web.ui.buildTestContent
import tech.testsys.web.ui.buildTestRow
import tech.testsys.web.ui.child
import tech.testsys.web.ui.classes
import tech.testsys.web.ui.find

class VerdictTests : MockVaadinTests() {
    @Test
    fun `should render zero as a neutral numeric verdict`() {
        val content = buildTestContent { verdict(0.0) }

        val verdict = content.find("ts-verdict")
        assertEquals("0", verdict.element.textRecursively)
        assertTrue("ts-verdict--score" in verdict.classes())
    }

    @Test
    fun `should update the numeric value through configured handle preserving caption`() {
        lateinit var handle: DataHandle<Double>
        val content = buildTestContent { handle = verdict(1.0, label = "баллов") { data = 2.5 } }

        handle.data = 0.0

        assertEquals(0.0, handle.data)
        assertEquals("0баллов", content.find("ts-verdict").element.textRecursively)
        assertEquals("баллов", content.find("ts-verdict__label").element.textRecursively)
    }

    @Test
    fun `should place a verdict on requested row columns`() {
        val row = buildTestRow { verdict(0.0, size = 4) }

        assertEquals("span 4", row.child(0).element.style.get("grid-column"))
        assertEquals("0", row.find("ts-verdict").element.textRecursively)
    }

    @Test
    fun `should configure a row verdict on remaining columns`() {
        val row = buildTestRow {
            verdict(4.0, size = 4)
            verdict(0.0) { data = 5.0 }
        }

        assertEquals("span 20", row.child(1).element.style.get("grid-column"))
        assertEquals("5", row.child(1).find("ts-verdict").element.textRecursively)
    }

    @Test
    fun `should preserve a finite score outside the range of Long`() {
        val content = buildTestContent { verdict(1.0e20) }

        assertEquals("100000000000000000000", content.find("ts-verdict").element.textRecursively)
    }

    @Test
    fun `should reject a nonfinite score`() {
        assertThrows(IllegalArgumentException::class.java) { buildTestContent { verdict(Double.NaN) } }
    }
}
