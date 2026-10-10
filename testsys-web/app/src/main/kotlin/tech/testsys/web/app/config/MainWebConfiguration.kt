package tech.testsys.web.app.config

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.context.annotation.Conditional
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Role
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor
import org.springframework.transaction.interceptor.TransactionAttributeSource
import tech.testsys.infra.database.api.DatabaseConfiguration
import tech.testsys.infra.database.api.transaction.TransactionRetry

/**
 * Registers a child-local retry advisor because the infrastructure auto-proxy creator accepts only local advisor definitions.
 * The retry policy and transaction manager remain owned by the parent.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration(proxyBeanMethods = false)
class MainWebConfiguration {
    companion object {
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
    }
}

private class SharedTransactionPolicyCondition : Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        context.beanFactory?.parentBeanFactory?.containsBean("transactionRetry") == true
}
