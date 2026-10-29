package tech.testsys.web.app.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.aop.framework.Advised
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.transaction.interceptor.TransactionInterceptor
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.UserMailSender
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.infra.database.api.persistence.FileSystemBlobStorage
import tech.testsys.infra.database.api.service.ServiceTrikStudioVersions
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.infra.database.api.transaction.RetryingTransactionInterceptor
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.settings.api.registry.GradingNodeStore
import tech.testsys.infra.settings.api.service.SettingsService
import tech.testsys.infra.settings.api.store.SettingsStore
import tech.testsys.web.app.PostgresTestConfiguration
import tech.testsys.web.app.config.CallLoggingInterceptor
import tech.testsys.web.app.config.OperationMetricsInterceptor
import tech.testsys.web.app.service.administrator.AdministratorService
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.judge.JudgeService
import tech.testsys.web.app.service.manager.ManagerService
import tech.testsys.web.app.service.multi.MultipleRoleUserService
import tech.testsys.web.app.service.observer.ObserverService
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.StudyService
import tech.testsys.web.app.service.supervisor.SupervisorService
import tech.testsys.web.app.service.user.UserService

@Import(PostgresTestConfiguration::class)
@SpringBootTest
class ServicesContextTests {
    @Autowired
    private lateinit var context: ApplicationContext

    @ParameterizedTest
    @ValueSource(
        classes = [
            AdministratorService::class,
            DeveloperService::class,
            JudgeService::class,
            ManagerService::class,
            MultipleRoleUserService::class,
            ParticipantService::class,
            StudentService::class,
            StudyService::class,
            UserService::class,
        ],
    )
    fun `should register the service as a single transactional proxy`(type: Class<*>) {
        val services = context.getBeansOfType(type)

        assertEquals(1, services.size)
        assertTrue(AopUtils.isAopProxy(services.values.single()))
    }

    @ParameterizedTest
    @ValueSource(
        classes = [DeveloperService::class, JudgeService::class, ParticipantService::class, StudentService::class, StudyService::class],
    )
    fun `should apply the retrying advisor outside the transaction interceptor of the service`(type: Class<*>) {
        val service = assertInstanceOf(Advised::class.java, context.getBean(type))

        val advice = service.advisors.map { advisor -> advisor.advice }
        val retryIndex = advice.indexOfFirst { interceptor -> interceptor is RetryingTransactionInterceptor }
        val transactionIndex = advice.indexOfFirst { interceptor -> interceptor is TransactionInterceptor }

        assertTrue(retryIndex in 0 until transactionIndex, "advice: $advice")
    }

    @ParameterizedTest
    @ValueSource(
        classes = [
            AdministratorService::class,
            DeveloperService::class,
            JudgeService::class,
            ManagerService::class,
            MultipleRoleUserService::class,
            ParticipantService::class,
            StudentService::class,
            StudyService::class,
            UserService::class,
        ],
    )
    fun `should apply the operation metrics advisor outside the retrying advisor of the service`(type: Class<*>) {
        val service = assertInstanceOf(Advised::class.java, context.getBean(type))

        val advice = service.advisors.map { advisor -> advisor.advice }
        val metricsIndex = advice.indexOfFirst { interceptor -> interceptor is OperationMetricsInterceptor }
        val retryIndex = advice.indexOfFirst { interceptor -> interceptor is RetryingTransactionInterceptor }

        assertTrue(metricsIndex in 0 until retryIndex, "advice: $advice")
    }

    @ParameterizedTest
    @ValueSource(
        classes = [
            AdministratorService::class,
            DeveloperService::class,
            JudgeService::class,
            ManagerService::class,
            MultipleRoleUserService::class,
            ObserverService::class,
            ParticipantService::class,
            StudentService::class,
            StudyService::class,
            SupervisorService::class,
            UserService::class,
        ],
    )
    fun `should apply the call logging advisor inside the metrics and outside the retrying advisor of the service`(type: Class<*>) {
        val service = assertInstanceOf(Advised::class.java, context.getBean(type))

        val advice = service.advisors.map { advisor -> advisor.advice }
        val metricsIndex = advice.indexOfFirst { interceptor -> interceptor is OperationMetricsInterceptor }
        val loggingIndex = advice.indexOfFirst { interceptor -> interceptor is CallLoggingInterceptor }
        val retryIndex = advice.indexOfFirst { interceptor -> interceptor is RetryingTransactionInterceptor }

        assertTrue(metricsIndex in 0 until loggingIndex && loggingIndex < retryIndex, "advice: $advice")
    }

    @ParameterizedTest
    @ValueSource(
        classes = [
            BalancingGrader::class,
            UserMailSender::class,
            SettingsService::class,
            SettingsStore::class,
            GradingNodeStore::class,
            GradingNodeRegistry::class,
            ServiceUserSearch::class,
            ServiceTrikStudioVersions::class,
            TaskRepository::class,
            FileBlobStorage::class,
            PolygonDiagnostics::class,
        ],
    )
    fun `should apply the call logging advisor outside the transactions of the infrastructure bean`(type: Class<*>) {
        val bean = assertInstanceOf(Advised::class.java, context.getBean(type))

        val advice = bean.advisors.map { advisor -> advisor.advice }
        val loggingIndex = advice.indexOfFirst { interceptor -> interceptor is CallLoggingInterceptor }
        val retryIndex = advice.indexOfFirst { interceptor -> interceptor is RetryingTransactionInterceptor }

        assertTrue(loggingIndex == 0 && (retryIndex == -1 || loggingIndex < retryIndex), "advice: $advice")
    }

    @ParameterizedTest
    @ValueSource(classes = [SettingsService::class, GradingNodeRegistry::class])
    fun `should wrap a final infrastructure class through its interface`(type: Class<*>) {
        assertTrue(AopUtils.isJdkDynamicProxy(context.getBean(type)))
    }

    @Test
    fun `should use the filesystem blob storage of the database module`() {
        val storages = context.getBeansOfType(FileBlobStorage::class.java)

        assertEquals(1, storages.size)
        assertInstanceOf(FileSystemBlobStorage::class.java, storages.values.single())
    }

    @Test
    fun `should keep the entity manager closed outside transactions when the database defaults apply`() {
        val hasOpenInView = context.containsBean("openEntityManagerInViewInterceptor")

        assertFalse(hasOpenInView)
    }
}
