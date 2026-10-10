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
import tech.testsys.infra.database.api.persistence.FileStorageCleanupSettings
import tech.testsys.infra.database.api.persistence.FileStoragePaths
import tech.testsys.infra.database.api.transaction.RetryingTransactionInterceptor
import tech.testsys.infra.database.api.transaction.RetryingTransactionOperations
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.TestsysPhysicalNamingStrategy
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.persistence.FileStorageCleanupRepository
import tech.testsys.infra.database.internal.persistence.FileSystemBlobInventory
import java.time.Clock

/**
 * Configures JPA, persistence adapters, file storage and transaction retries for the application.
 * The companion object registers [TransactionRetry] and an advisor outside the transaction interceptor.
 *
 * @since %CURRENT_VERSION%
 */
@Configuration
@EntityScan(basePackages = ["tech.testsys.infra.database.internal.jpa.entity"])
@EnableJpaRepositories(basePackages = ["tech.testsys.infra.database.internal.jpa.repository"])
@ComponentScan(basePackages = ["tech.testsys.infra.database.api"])
@PropertySource("classpath:hibernate-defaults.properties", "classpath:file-storage-defaults.properties")
@EnableConfigurationProperties(FileStoragePaths::class, FileStorageCleanupSettings::class)
class DatabaseConfiguration {

    /**
     * Dedicated UTC clock for file cleanup, selected explicitly so application clocks remain unambiguous.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean(defaultCandidate = false)
    fun fileStorageCleanupClock(): Clock = Clock.systemUTC()

    /**
     * Transactional maintenance queries for orphan file rows.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    @OptIn(InternalDatabaseApi::class)
    fun fileStorageCleanupRepository(
        fileData: FileDataJpaEntityRepository,
        transactionManager: PlatformTransactionManager,
    ): FileStorageCleanupRepository = FileStorageCleanupRepository(fileData, transactionManager)

    /**
     * Bounded filesystem traversal for maintenance cleanup.
     *
     * @since %CURRENT_VERSION%
     */
    @Bean
    @OptIn(InternalDatabaseApi::class)
    fun fileSystemBlobInventory(): FileSystemBlobInventory = FileSystemBlobInventory()

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
