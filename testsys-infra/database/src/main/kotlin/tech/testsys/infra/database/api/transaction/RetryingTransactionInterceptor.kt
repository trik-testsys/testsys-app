package tech.testsys.infra.database.api.transaction

import org.aopalliance.intercept.MethodInterceptor
import org.aopalliance.intercept.MethodInvocation
import org.springframework.aop.ProxyMethodInvocation

/**
 * Advice placed outside the transaction interceptor of a `@Transactional` method: each attempt proceeds through a
 * clone of the invocation, so it opens and commits a new transaction, and [retry] repeats it on a conflict, including
 * one reported at commit.
 *
 * @property retry the policy that repeats conflicting transactions.
 * @since %CURRENT_VERSION%
 */
class RetryingTransactionInterceptor(private val retry: TransactionRetry) : MethodInterceptor {

    override fun invoke(invocation: MethodInvocation): Any? {
        val proxyInvocation = invocation as? ProxyMethodInvocation
            ?: error("Retrying transactions need a proxy invocation, got ${invocation.javaClass.name}")
        val name = "${invocation.method.declaringClass.simpleName}.${invocation.method.name}"
        return retry.run(name) { proxyInvocation.invocableClone().proceed() }
    }
}
