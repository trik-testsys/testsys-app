package tech.testsys.infra.database.api

import io.micrometer.core.instrument.MeterRegistry
import org.hibernate.boot.model.naming.PhysicalNamingStrategy
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.context.annotation.Role
import org.springframework.core.Ordered
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.interceptor.BeanFactoryTransactionAttributeSourceAdvisor
import org.springframework.transaction.interceptor.TransactionAttributeSource
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.model.user.AccessTokenHasher
import tech.testsys.infra.database.api.persistence.FileStorageCleanupProperties
import tech.testsys.infra.database.api.persistence.FileStoragePaths
import tech.testsys.infra.database.api.transaction.RetryingTransactionInterceptor
import tech.testsys.infra.database.api.transaction.RetryingTransactionOperations
import tech.testsys.infra.database.api.transaction.TransactionRetry
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.TestsysPhysicalNamingStrategy
import tech.testsys.infra.database.internal.jpa.repository.task.FileDataJpaEntityRepository
import tech.testsys.infra.database.internal.persistence.FileStorageCleanupRepository
import tech.testsys.infra.database.internal.persistence.FileSystemBlobInventory
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.Settings
import tech.testsys.infra.settings.api.settingsOf
import tech.testsys.infra.settings.api.strictBinder
import java.time.Clock

/**
 * Configures JPA, persistence adapters, the access-code hasher, file storage and transaction retries for the application.
 * The companion object registers [TransactionRetry] and an advisor outside the transaction interceptor.
 *
 * @since 1.0.0-beta.1
 */
@Configuration
@EntityScan(basePackages = ["tech.testsys.infra.database.internal.jpa.entity"])
@EnableJpaRepositories(basePackages = ["tech.testsys.infra.database.internal.jpa.repository"])
@ComponentScan(basePackages = ["tech.testsys.infra.database.api"])
@PropertySource("classpath:hibernate-defaults.properties", "classpath:file-storage-defaults.properties")
class DatabaseConfiguration {

    /**
     * Binds the cleanup settings at creation, then reads them from the current snapshot of the shared holder,
     * or from [environment] without the holder.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun fileStorageCleanupSettings(environment: Environment, runtime: RuntimeSettings? = null): Settings<FileStorageCleanupProperties> =
        settingsOf(FileStorageCleanupProperties::class.java, environment, runtime)

    /**
     * Dedicated UTC clock for file cleanup, selected explicitly so application clocks remain unambiguous.
     *
     * @since 1.0.0-beta.1
     */
    @Bean(defaultCandidate = false)
    fun fileStorageCleanupClock(): Clock = Clock.systemUTC()

    /**
     * Transactional maintenance queries for orphan file rows.
     *
     * @since 1.0.0-beta.1
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
     * @since 1.0.0-beta.1
     */
    @Bean
    @OptIn(InternalDatabaseApi::class)
    fun fileSystemBlobInventory(): FileSystemBlobInventory = FileSystemBlobInventory()

    /**
     * Naming strategy applied to every entity of the module; Spring Boot passes it to Hibernate.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    @OptIn(InternalDatabaseApi::class)
    fun physicalNamingStrategy(): PhysicalNamingStrategy = TestsysPhysicalNamingStrategy()

    /**
     * Programmatic transactions repeated on a conflict; replaces the `TransactionTemplate` of Spring Boot.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun transactionOperations(transactionManager: PlatformTransactionManager, transactionRetry: TransactionRetry): TransactionOperations =
        RetryingTransactionOperations(transactionManager, transactionRetry)

    /**
     * Hasher of access codes keyed with the required `testsys.access-token.secret` property; changing the secret
     * invalidates every stored access code.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun accessTokenHasher(environment: Environment): AccessTokenHasher {
        val secret = strictBinder(environment).bind(ACCESS_TOKEN_SECRET, String::class.java).orElse(null)
        return AccessTokenHasher(checkNotNull(secret) { "$ACCESS_TOKEN_SECRET is required" })
    }

    /**
     * Absolute paths of the stored files, bound once from `testsys.file-storage.paths`; a missing path fails startup.
     *
     * @since 1.0.0-beta.1
     */
    @Bean
    fun fileStoragePaths(environment: Environment): FileStoragePaths =
        Binder.get(environment).bindOrCreate("testsys.file-storage.paths", FileStoragePaths::class.java)

    /**
     * Static infrastructure beans: they are created before the auto-proxy creator wraps the application beans.
     *
     * @since 1.0.0-beta.1
     */
    companion object {

        /**
         * The policy shared by all repeated transactions; each repeat is counted in the metrics registry, if any.
         * The registry is looked up at the first repeat, so this early bean does not create it.
         *
         * @since 1.0.0-beta.1
         */
        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun transactionRetry(registry: ObjectProvider<MeterRegistry>): TransactionRetry = TransactionRetry(
            onRetry = { registry.ifAvailable?.counter(TestSysMetrics.TRANSACTION_RETRIES)?.increment() },
        )

        /**
         * Advisor of the methods the transaction advisor matches, ordered just outside it, so that each repeat
         * of a conflicting method runs in a new transaction.
         *
         * @since 1.0.0-beta.1
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

private const val ACCESS_TOKEN_SECRET = "testsys.access-token.secret"
