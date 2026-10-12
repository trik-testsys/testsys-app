package tech.testsys.web.monitoring

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.apache.catalina.connector.ClientAbortException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.task.TaskId
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.web.app.error.operationFailure

class WebFailuresTests {
    private val registry = SimpleMeterRegistry()
    private val failures = WebFailures("service", TechnicalFailures(registry))

    @Test
    fun `should count an unexpected exception with the interface and stage`() {
        failures.record(WebFailures.EVENT, IllegalStateException("broken"))

        assertEquals(1.0, count(stage = "event", exception = "IllegalStateException"))
    }

    @Test
    fun `should not count a failed operation`() {
        failures.record(WebFailures.NAVIGATION, operationFailure(TaskNotExistsError(TaskId(1))))

        assertEquals(0.0, total())
    }

    @Test
    fun `should not count an exception caused by a failed operation`() {
        failures.record(WebFailures.EVENT, IllegalStateException("listener failed", operationFailure(TaskNotExistsError(TaskId(1)))))

        assertEquals(0.0, total())
    }

    @Test
    fun `should not count a connection closed by the client`() {
        failures.record(WebFailures.EVENT, ClientAbortException("Connection reset by peer"))

        assertEquals(0.0, total())
    }

    private fun count(stage: String, exception: String): Double = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
        .tags(TestSysMetrics.SOURCE, "service", TestSysMetrics.STAGE, stage, TestSysMetrics.EXCEPTION, exception)
        .counter()?.count() ?: 0.0

    private fun total(): Double = registry.find(TestSysMetrics.TECHNICAL_FAILURES).counters().sumOf { counter -> counter.count() }
}
