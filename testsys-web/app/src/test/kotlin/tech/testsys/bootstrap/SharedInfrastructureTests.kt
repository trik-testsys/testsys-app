package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.Advised
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.interceptor.TransactionInterceptor
import tech.testsys.domain.builder.api.multipleRoleUserData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.infra.database.api.transaction.RetryingTransactionInterceptor
import tech.testsys.web.app.PostgresTestContainer
import java.util.UUID
import java.util.function.Supplier

class SharedInfrastructureTests {
    @Test
    fun `should commit and roll back service transactions in the main child using the parent manager`() {
        val database = PostgresTestContainer.instance
        TestSysRuntime.start(
            arrayOf(
                "--server.port=0",
                "--vaadin.productionMode=true",
                "--spring.datasource.url=${database.jdbcUrl}",
                "--spring.datasource.username=${database.username}",
                "--spring.datasource.password=${database.password}",
                "--testsys.service.secret=${"transaction-test-".repeat(8)}",
                "--testsys.service.address=127.0.0.1",
                "--testsys.service.port=0",
            ),
        ).use { runtime ->
            val repository = runtime.infrastructure.getBean(MultipleRoleUserRepository::class.java)
            val definition = RootBeanDefinition(TransactionalProbe::class.java).apply {
                instanceSupplier = Supplier { TransactionalProbe(repository) }
            }
            val registry = checkNotNull(runtime.mainWeb as? BeanDefinitionRegistry)
            registry.registerBeanDefinition("transactionalProbe", definition)
            val probe = runtime.mainWeb.getBean(TransactionalProbe::class.java)
            val advised = checkNotNull(probe as? Advised)
            val interceptors = advised.advisors.map { advisor -> advisor.advice }
            val retryIndex = interceptors.indexOfFirst { it is RetryingTransactionInterceptor }
            val transactionIndex = interceptors.indexOfFirst { it is TransactionInterceptor }
            assertTrue(retryIndex in 0 until transactionIndex, interceptors.toString())
            assertEquals(1, interceptors.count { it is TransactionInterceptor })
            val committedCode = UUID.randomUUID().toString()
            val rolledBackCode = UUID.randomUUID().toString()

            probe.store(code = committedCode, shouldFail = false)
            assertThrows(IllegalStateException::class.java) { probe.store(code = rolledBackCode, shouldFail = true) }

            assertNotNull(repository.findByAccessToken(committedCode))
            assertNull(repository.findByAccessToken(rolledBackCode))
        }
    }
}

@Transactional
open class TransactionalProbe(private val repository: MultipleRoleUserRepository) {
    open fun store(code: String, shouldFail: Boolean) {
        repository.save(
            multipleRoleUserData {
                name = "Transaction probe"
                email = "$code@example.test"
                accessToken(code, HashAlgorithm.Identity)
            },
        )
        check(!shouldFail) { "Requested test rollback" }
    }
}
