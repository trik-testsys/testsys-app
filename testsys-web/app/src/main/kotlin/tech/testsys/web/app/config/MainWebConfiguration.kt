package tech.testsys.web.app.config

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.aop.Advisor
import org.springframework.aop.ClassFilter
import org.springframework.aop.Pointcut
import org.springframework.aop.support.ComposablePointcut
import org.springframework.aop.support.DefaultPointcutAdvisor
import org.springframework.aop.support.StaticMethodMatcher
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Role
import org.springframework.core.Ordered
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.http.server.observation.ServerRequestObservationConvention
import org.springframework.stereotype.Service
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor
import org.springframework.transaction.interceptor.TransactionAttributeSource
import tech.testsys.infra.database.api.DatabaseConfiguration
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.web.monitoring.WebFailures
import tech.testsys.web.monitoring.WebRequestObservationConvention
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Registers the child-local retry advisor, with the parent's policy and transaction manager, the metrics and the call log
 * of the main interface; the infrastructure auto-proxy creator accepts only local advisor definitions.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class MainWebConfiguration {
    /**
     * Labels the HTTP requests of the main interface with `main` and its route templates.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun mainRequestObservationConvention(): ServerRequestObservationConvention = WebRequestObservationConvention(MAIN)

    /**
     * Counts unexpected exceptions of the main interface pages before they are shown.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun mainWebFailures(failures: TechnicalFailures): WebFailures = WebFailures(MAIN, failures)

    companion object {
        private const val MAIN = "main"
        private const val SERVICE_PACKAGE = "tech.testsys.web.app.service"

        /**
         * Wraps the child's transaction interceptor with the parent's retry policy; flat test contexts already own this advisor.
         *
         * @since %CURRENT_VERSION%
         */
        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        @Conditional(SharedTransactionPolicyCondition::class)
        fun retryingTransactionAdvisor(
            transactionAttributeSource: TransactionAttributeSource,
            transactionRetry: TransactionRetry,
        ): BeanFactoryTransactionAttributeSourceAdvisor = DatabaseConfiguration.retryingTransactionAdvisor(
            transactionAttributeSource,
            transactionRetry,
        )

        /**
         * Counts failed operations and technical failures of proxy service calls outside the transaction repeats.
         * Its dependencies are looked up at the first call, so this early bean does not create them.
         *
         * @since %CURRENT_VERSION%
         */
        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun operationMetricsAdvisor(registry: ObjectProvider<MeterRegistry>, failures: ObjectProvider<TechnicalFailures>): Advisor {
            val interceptor = OperationMetricsInterceptor(
                registry = { registry.ifAvailable },
                failures = { failures.getIfAvailable { TechnicalFailures.NONE } },
            )
            return DefaultPointcutAdvisor(serviceOperations(), interceptor).apply {
                order = Ordered.HIGHEST_PRECEDENCE
            }
        }

        /**
         * Writes proxy service calls at DEBUG inside the metrics and outside the transaction repeats, so a repeated
         * call is written once.
         *
         * @since %CURRENT_VERSION%
         */
        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun callLoggingAdvisor(): Advisor = DefaultPointcutAdvisor(serviceOperations(), CallLoggingInterceptor()).apply {
            order = Ordered.HIGHEST_PRECEDENCE + 1
        }

        private fun serviceOperations(): Pointcut {
            val services = ClassFilter { type ->
                type.packageName.startsWith(SERVICE_PACKAGE) && type.isAnnotationPresent(Service::class.java)
            }
            val operations = object : StaticMethodMatcher() {
                override fun matches(method: Method, targetClass: Class<*>): Boolean =
                    Modifier.isPublic(method.modifiers) && method.declaringClass != Any::class.java
            }
            return ComposablePointcut(services, operations)
        }
    }
}

private class SharedTransactionPolicyCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        context.beanFactory?.parentBeanFactory?.containsBean("transactionRetry") == true
}
