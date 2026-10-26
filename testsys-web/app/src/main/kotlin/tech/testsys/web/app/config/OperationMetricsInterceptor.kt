package tech.testsys.web.app.config

import io.micrometer.core.instrument.MeterRegistry
import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.operation.error.AccessDeniedError
import tech.testsys.operation.error.EntityNotExistsError
import tech.testsys.operation.error.MissedRequiredRoleError
import tech.testsys.operation.error.OperationError
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.ResourceAccessError

/**
 * Counts the outcome of the outermost proxy service call after all transaction repeats: a failed operation in
 * [registry] by operation, general kind and error class, any other exception as a technical failure in [failures].
 * Successful calls and calls nested into another service call are not counted; every exception is rethrown unchanged.
 */
internal class OperationMetricsInterceptor(
    private val registry: () -> MeterRegistry?,
    private val failures: () -> TechnicalFailures,
) : MethodInterceptor {
    private val depth = ThreadLocal.withInitial { 0 }

    // Any exception leaving a service is either a failed operation or a technical failure, whatever its type.
    @Suppress("TooGenericExceptionCaught")
    override fun invoke(invocation: MethodInvocation): Any? {
        val level = depth.get()
        depth.set(level + 1)
        try {
            return invocation.proceed()
        } catch (failure: OperationException) {
            if (level == 0) countOperationError(invocation, failure.error)
            throw failure
        } catch (failure: Throwable) {
            if (level == 0) failures().record(source = SOURCE, stage = STAGE, failure = failure)
            throw failure
        } finally {
            if (level == 0) depth.remove() else depth.set(level)
        }
    }

    private fun countOperationError(invocation: MethodInvocation, error: OperationError) {
        val operation = "${invocation.method.declaringClass.simpleName}.${invocation.method.name.substringBefore('-')}"
        registry()?.counter(
            TestSysMetrics.OPERATION_ERRORS,
            TestSysMetrics.OPERATION,
            operation,
            TestSysMetrics.KIND,
            kind(error),
            TestSysMetrics.ERROR,
            error::class.simpleName.orEmpty(),
        )?.increment()
    }

    private fun kind(error: OperationError): String = when (error) {
        is EntityNotExistsError -> "not_found"
        is AccessDeniedError -> "access_denied"
        is MissedRequiredRoleError -> "missing_role"
        is ResourceAccessError -> "resource_access"
        else -> "other"
    }

    private companion object {
        const val SOURCE = "main"
        const val STAGE = "service"
    }
}
