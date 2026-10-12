package tech.testsys.web.app.config

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.aop.framework.ProxyFactory
import org.springframework.dao.ConcurrencyFailureException
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.infra.database.api.transaction.RetryingTransactionInterceptor
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.operation.error.BlankJudgmentReasonError
import tech.testsys.operation.error.MissedJudgeRoleError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.StatementNotUploadedToTaskError
import tech.testsys.operation.error.SubmissionAccessDeniedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.web.app.error.operationFailure
import java.time.Duration

class OperationMetricsInterceptorTests {
    private val registry = SimpleMeterRegistry()
    private val failures = TechnicalFailures(registry)
    private val interceptor = OperationMetricsInterceptor(registry = { registry }, failures = { failures })
    private val inner = proxy(InnerService())
    private val outer = proxy(OuterService(inner))

    @ParameterizedTest
    @MethodSource("errors")
    fun `should count a failed operation by operation kind and error class`(error: OperationError, kind: String, name: String) {
        assertThrows(OperationException::class.java) { inner.fail(operationFailure(error)) }

        assertEquals(1.0, operationErrors(operation = "InnerService.fail", kind = kind, error = name))
    }

    @Test
    fun `should rethrow the failed operation unchanged`() {
        val failure = operationFailure(TaskNotExistsError(TaskId(1)))

        val thrown = assertThrows(OperationException::class.java) { inner.fail(failure) }

        assertSame(failure, thrown)
    }

    @Test
    fun `should not use the error values as label values`() {
        assertThrows(OperationException::class.java) { inner.fail(operationFailure(TaskNotExistsError(TaskId(918_273)))) }

        val labels = registry.meters.flatMap { meter -> meter.id.tags }.map { tag -> tag.value }
        assertEquals(emptyList<String>(), labels.filter { value -> "918273" in value })
    }

    @Test
    fun `should strip the suffix of a mangled method name`() {
        assertThrows(OperationException::class.java) { inner.view(TaskId(1)) }

        assertEquals(1.0, operationErrors(operation = "InnerService.view", kind = "not_found", error = "TaskNotExistsError"))
    }

    @Test
    fun `should count a failed operation once at the outermost service call`() {
        assertThrows(OperationException::class.java) { outer.delegate(operationFailure(MissedJudgeRoleError)) }

        assertEquals(1.0, operationErrors(operation = "OuterService.delegate", kind = "missing_role", error = "MissedJudgeRoleError"))
        assertNull(registry.find(TestSysMetrics.OPERATION_ERRORS).tag(TestSysMetrics.OPERATION, "InnerService.fail").counter())
    }

    @Test
    fun `should not count a failed operation handled by the outer service`() {
        outer.recover(operationFailure(MissedJudgeRoleError))

        assertNull(registry.find(TestSysMetrics.OPERATION_ERRORS).counter())
    }

    @Test
    fun `should count another exception as a technical failure of the main interface`() {
        val failure = IllegalStateException("Storage unavailable")

        val thrown = assertThrows(IllegalStateException::class.java) { inner.fail(failure) }

        assertSame(failure, thrown)
        assertEquals(1.0, technicalFailures(exception = "IllegalStateException"))
        assertNull(registry.find(TestSysMetrics.OPERATION_ERRORS).counter())
    }

    @Test
    fun `should count a technical failure once when it passes through nested services`() {
        assertThrows(IllegalStateException::class.java) { outer.delegate(IllegalStateException("Storage unavailable")) }

        assertEquals(1.0, technicalFailures(exception = "IllegalStateException"))
    }

    @Test
    fun `should count nothing for a successful call`() {
        inner.succeed()

        assertEquals(emptyList<Any>(), registry.meters)
    }

    @Test
    fun `should count only the repeat when a conflicting transaction succeeds on the next attempt`() {
        val retry = TransactionRetry(
            delay = Duration.ZERO,
            jitter = Duration.ZERO,
            onRetry = { registry.counter(TestSysMetrics.TRANSACTION_RETRIES).increment() },
        )
        val factory = ProxyFactory(ConflictingService()).apply {
            isProxyTargetClass = true
            addAdvice(interceptor)
            addAdvice(RetryingTransactionInterceptor(retry))
        }
        val service = assertInstanceOf(ConflictingService::class.java, factory.proxy)

        val result = service.save()

        assertEquals("saved", result)
        assertEquals(1.0, registry.find(TestSysMetrics.TRANSACTION_RETRIES).counter()?.count())
        assertNull(registry.find(TestSysMetrics.TECHNICAL_FAILURES).counter())
        assertNull(registry.find(TestSysMetrics.OPERATION_ERRORS).counter())
    }

    private fun operationErrors(operation: String, kind: String, error: String): Double = registry.find(TestSysMetrics.OPERATION_ERRORS)
        .tags(TestSysMetrics.OPERATION, operation, TestSysMetrics.KIND, kind, TestSysMetrics.ERROR, error)
        .counter()?.count() ?: 0.0

    private fun technicalFailures(exception: String): Double = registry.find(TestSysMetrics.TECHNICAL_FAILURES)
        .tags(TestSysMetrics.SOURCE, "main", TestSysMetrics.STAGE, "service", TestSysMetrics.EXCEPTION, exception)
        .counter()?.count() ?: 0.0

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> proxy(target: T): T = ProxyFactory(target).apply {
        isProxyTargetClass = true
        addAdvice(interceptor)
    }.proxy as T

    open class InnerService {
        open fun fail(failure: Exception): Nothing = throw failure

        open fun view(taskId: TaskId): Nothing = throw operationFailure(TaskNotExistsError(taskId))

        open fun succeed(): String = "done"
    }

    open class OuterService(private val inner: InnerService) {
        open fun delegate(failure: Exception): Nothing = inner.fail(failure)

        open fun recover(failure: Exception): String = runCatching { inner.fail(failure) }.fold({ "unreachable" }, { "recovered" })
    }

    open class ConflictingService {
        private var attempts = 0

        open fun save(): String {
            attempts++
            if (attempts == 1) throw ConcurrencyFailureException("Concurrent update")
            return "saved"
        }
    }

    companion object {
        @JvmStatic
        fun errors(): List<Arguments> = listOf(
            Arguments.of(TaskNotExistsError(TaskId(1)), "not_found", "TaskNotExistsError"),
            Arguments.of(SubmissionAccessDeniedError(SubmissionId(1)), "access_denied", "SubmissionAccessDeniedError"),
            Arguments.of(MissedJudgeRoleError, "missing_role", "MissedJudgeRoleError"),
            Arguments.of(StatementNotUploadedToTaskError(TaskId(1), StatementId(1)), "resource_access", "StatementNotUploadedToTaskError"),
            Arguments.of(BlankJudgmentReasonError, "other", "BlankJudgmentReasonError"),
        )
    }
}
