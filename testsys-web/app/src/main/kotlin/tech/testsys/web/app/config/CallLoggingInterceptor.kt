package tech.testsys.web.app.config

import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import tech.testsys.operation.error.OperationException
import java.util.concurrent.TimeUnit

/**
 * Writes each call at DEBUG to the logger of the target class: its arguments, then its result or failure with the
 * duration, rendered by [LoggedValues]. A failed operation is written with its error, any other exception only with
 * its type; every exception is rethrown unchanged.
 */
internal class CallLoggingInterceptor : MethodInterceptor {
    // Any exception leaving the call is written and rethrown unchanged, whatever its type.
    @Suppress("TooGenericExceptionCaught")
    override fun invoke(invocation: MethodInvocation): Any? {
        val target = invocation.getThis()?.let(AopUtils::getTargetClass) ?: invocation.method.declaringClass
        val logger = LoggerFactory.getLogger(target)
        if (!logger.isDebugEnabled) return invocation.proceed()

        val call = "${target.simpleName}.${invocation.method.name.substringBefore('-')}"
        logger.debug("call {}({})", call, rendered { LoggedValues.arguments(invocation.method, invocation.arguments) })
        val start = System.nanoTime()
        try {
            val result = invocation.proceed()
            logger.debug("done {} in {} ms: {}", call, elapsed(start), rendered { LoggedValues.result(result) })
            return result
        } catch (failure: OperationException) {
            logger.debug("fail {} in {} ms: {}", call, elapsed(start), rendered { LoggedValues.value(failure.error) })
            throw failure
        } catch (failure: Throwable) {
            logger.debug("fail {} in {} ms: {}", call, elapsed(start), failure.javaClass.name)
            throw failure
        }
    }

    private fun elapsed(start: Long): Long = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)

    // Rendering must never change the outcome of the call it describes.
    private fun rendered(render: () -> String): String =
        runCatching(render).getOrElse { failure -> "<unrenderable: ${failure.javaClass.simpleName}>" }
}
