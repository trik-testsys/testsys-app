package tech.testsys.web.app.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.api.taskValidationRequest
import tech.testsys.domain.builder.api.taskValidationRequestData
import tech.testsys.domain.builder.api.taskValidationSnapshot
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.operation.TaskValidationOperations
import java.time.Instant
import java.util.concurrent.TimeUnit

class OperationsConfigurationTests {
    private val configuration = OperationsConfiguration()
    private val requests = mockk<TaskValidationRequestRepository>()
    private val operations = mockk<TaskValidationOperations>()
    private val registry = SimpleMeterRegistry()
    private val executor = configuration.taskValidationExecutor()
    private val dispatcher = configuration.taskValidationDispatcher(
        requests = requests,
        operations = operations,
        grader = mockk<BalancingGrader>(),
        executor = executor,
        failures = TechnicalFailures(registry),
    )
    private val request = taskValidationRequest {
        id = 11
        createdAt = Instant.EPOCH
        version = EntityVersion(0)
        data = taskValidationRequestData {
            task(0)
            requestedBy(0)
            snapshot = taskValidationSnapshot {}
            execution.pendingDiagnostics()
        }
    }

    @Test
    fun `should count a failed processing of a validation request as a validation technical failure`() {
        every { operations.proceed(request.id) } throws IllegalStateException("Grader unavailable")
        every { requests.recordTechnicalFailure(request.id, any()) } returns request

        dispatcher.schedule(request.id)
        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)

        val failures = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
            .tags(
                TestSysMetrics.SOURCE,
                "validation",
                TestSysMetrics.STAGE,
                "processing",
                TestSysMetrics.EXCEPTION,
                "IllegalStateException",
            )
            .counter()
        assertEquals(1.0, failures?.count())
    }
}
