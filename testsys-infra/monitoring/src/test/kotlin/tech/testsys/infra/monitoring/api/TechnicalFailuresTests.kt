package tech.testsys.infra.monitoring.api

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException

class TechnicalFailuresTests {
    private val registry = SimpleMeterRegistry()
    private val failures = TechnicalFailures(registry)

    @Test
    fun `should count a failure by source stage and simple exception class name`() {
        failures.record(source = "grading", stage = "save", failure = IllegalStateException("Submission 42 of user 7 failed"))

        assertEquals(1.0, count(source = "grading", stage = "save", exception = "IllegalStateException"))
    }

    @Test
    fun `should not use the exception message as a label value`() {
        failures.record(source = "mail", stage = "after_commit", failure = IOException("user@example.com is unreachable"))

        val labels = registry.meters.flatMap { meter -> meter.id.tags }.map { tag -> tag.value }
        assertFalse(labels.any { value -> "example.com" in value })
    }

    @Test
    fun `should count a handled refusal without an exception as none`() {
        failures.record(source = "grading", stage = "invalid_result")

        assertEquals(1.0, count(source = "grading", stage = "invalid_result", exception = "none"))
    }

    @Test
    fun `should count the same exception once when several handlers receive it`() {
        val failure = IllegalStateException("broken")

        failures.record(source = "main", stage = "service", failure = failure)
        failures.record(source = "main", stage = "event", failure = failure)

        assertEquals(1.0, count(source = "main", stage = "service", exception = "IllegalStateException"))
        assertEquals(0.0, count(source = "main", stage = "event", exception = "IllegalStateException"))
    }

    @Test
    fun `should not count a wrapper of an exception that was already counted`() {
        val cause = IllegalStateException("broken")
        failures.record(source = "main", stage = "service", failure = cause)

        failures.record(source = "main", stage = "navigation", failure = RuntimeException("wrapped", cause))

        assertEquals(0.0, count(source = "main", stage = "navigation", exception = "RuntimeException"))
    }

    @Test
    fun `should not count the cause of an exception that was already counted`() {
        val cause = IllegalStateException("broken")
        failures.record(source = "main", stage = "navigation", failure = RuntimeException("wrapped", cause))

        failures.record(source = "main", stage = "event", failure = cause)

        assertEquals(0.0, count(source = "main", stage = "event", exception = "IllegalStateException"))
    }

    @Test
    fun `should count different exceptions of the same type separately`() {
        failures.record(source = "grading", stage = "save", failure = IllegalStateException("first"))
        failures.record(source = "grading", stage = "save", failure = IllegalStateException("second"))

        assertEquals(2.0, count(source = "grading", stage = "save", exception = "IllegalStateException"))
    }

    @Test
    fun `should count different wrappers of one shared cause separately`() {
        val cause = IOException("connection refused")
        failures.record(source = "grading", stage = "save", failure = IllegalStateException("first", cause))

        failures.record(source = "grading", stage = "save", failure = IllegalStateException("second", cause))

        assertEquals(2.0, count(source = "grading", stage = "save", exception = "IllegalStateException"))
    }

    @Test
    fun `should count every handled refusal without an exception`() {
        failures.record(source = "nodes", stage = "record_mismatch")
        failures.record(source = "nodes", stage = "record_mismatch")

        assertEquals(2.0, count(source = "nodes", stage = "record_mismatch", exception = "none"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Main", "user 7", "1main", "main-web"])
    fun `should reject a source that is not a lowercase label`(source: String) {
        assertThrows(IllegalArgumentException::class.java) { failures.record(source = source, stage = "event") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "Event", "/developer/tasks/42"])
    fun `should reject a stage that is not a lowercase label`(stage: String) {
        assertThrows(IllegalArgumentException::class.java) { failures.record(source = "main", stage = stage) }
    }

    private fun count(source: String, stage: String, exception: String): Double = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
        .tags(TestSysMetrics.SOURCE, source, TestSysMetrics.STAGE, stage, TestSysMetrics.EXCEPTION, exception)
        .counter()?.count() ?: 0.0
}
