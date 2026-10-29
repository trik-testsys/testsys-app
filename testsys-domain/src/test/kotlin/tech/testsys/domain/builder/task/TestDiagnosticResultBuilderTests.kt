package tech.testsys.domain.builder.task

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.model.task.*

class TestDiagnosticResultBuilderTests {
    @Test
    fun `should reject a result without its polygon identifier`() {
        assertThrows(IllegalArgumentException::class.java) { testDiagnosticResult {} }
    }

    @Test
    fun `should retain a typed reason and the concrete indexed attribute path`() {
        val result = testDiagnosticResult {
            testId(3)
            reports += diagnosticReport {
                severity = DiagnosticSeverity.Error
                data.invalidEventId { id = "missing" }
                location = diagnosticLocation {
                    path += diagnosticPathSegment {
                        tag = "event"
                        index = 2
                    }
                    attribute = "id"
                }
            }
        }

        assertEquals(TestId(3), result.testId)
        assertEquals(DiagnosticData.InvalidEventId("missing"), result.reports.single().data)
        assertEquals(2, result.reports.single().location?.path?.single()?.index)
        assertEquals("id", result.reports.single().location?.attribute)
    }

    @Test
    fun `should reject a message without a chosen reason`() {
        assertThrows(IllegalArgumentException::class.java) {
            diagnosticReport { severity = DiagnosticSeverity.Error }
        }
    }
}
