package tech.testsys.infra.database.api

import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Role
import org.springframework.core.Ordered
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor
import org.springframework.transaction.interceptor.TransactionAttributeSource
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.infra.database.api.persistence.FileStoragePaths
import tech.testsys.infra.database.api.transaction.RetryingTransactionInterceptor
import tech.testsys.infra.database.api.transaction.RetryingTransactionOperations
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.TestsysPhysicalNamingStrategy

/**
 * Spring configuration of the JPA layer, imported by the application: Hibernate defaults and the REPEATABLE READ
 * isolation from `classpath:hibernate-defaults.properties`, the [TestsysPhysicalNamingStrategy] bean, scanning of
 * the module's entities, Spring Data repositories, persistence adapters and file storage with its
 * [FileStoragePaths], and the repetition of conflicting transactions. The companion object registers
 * [TransactionRetry] and the advisor that repeats every `@Transactional` method outside its transaction interceptor.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EntityScan(basePackages = ["tech.testsys.infra.database.internal.jpa.entity"])
@EnableJpaRepositories(basePackages = ["tech.testsys.infra.database.internal.jpa.repository"])
@ComponentScan(basePackages = ["tech.testsys.infra.database.api"])
@PropertySource("classpath:hibernate-defaults.properties")
@EnableConfigurationProperties(FileStoragePaths::class)
class DatabaseConfiguration {

    /**
     * Naming strategy applied to every entity of the module; Spring Boot passes it to Hibernate.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    @OptIn(InternalDatabaseApi::class)
    fun physicalNamingStrategy(): PhysicalNamingStrategy = TestsysPhysicalNamingStrategy()

    /**
     * Programmatic transactions repeated on a conflict; replaces the `TransactionTemplate` of Spring Boot.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    fun transactionOperations(transactionManager: PlatformTransactionManager, transactionRetry: TransactionRetry): TransactionOperations =
        RetryingTransactionOperations(transactionManager, transactionRetry)

    /**
     * Static infrastructure beans: they are created before the auto-proxy creator wraps the application beans.
     *
     * @since %CURRENT_VERSION%
     */
    companion object {

        /**
         * The policy shared by all repeated transactions.
         *
         * @since %CURRENT_VERSION%
         */
        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun transactionRetry(): TransactionRetry = TransactionRetry()

        /**
         * Advisor of the methods the transaction advisor matches, ordered just outside it, so that each repeat
         * of a conflicting method runs in a new transaction.
         *
         * @since %CURRENT_VERSION%
         */
        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun retryingTransactionAdvisor(
            transactionAttributeSource: TransactionAttributeSource,
            transactionRetry: TransactionRetry,
        ): BeanFactoryTransactionAttributeSourceAdvisor = BeanFactoryTransactionAttributeSourceAdvisor().apply {
            setTransactionAttributeSource(transactionAttributeSource)
            advice = RetryingTransactionInterceptor(transactionRetry)
            order = Ordered.LOWEST_PRECEDENCE - 1
        }
    }
}
