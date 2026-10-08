package tech.testsys.web.app.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import tech.testsys.domain.contract.FileBlobStorage
import tech.testsys.infra.database.api.persistence.FileSystemBlobStorage
import tech.testsys.web.app.service.developer.DeveloperService
import tech.testsys.web.app.service.judge.JudgeService
import tech.testsys.web.app.service.participant.ParticipantService
import tech.testsys.web.app.service.student.StudentService
import tech.testsys.web.app.service.study.StudyService

@SpringBootTest
class ServicesContextTests {
    @Autowired
    private lateinit var context: ApplicationContext

    @ParameterizedTest
    @ValueSource(
        classes = [DeveloperService::class, JudgeService::class, ParticipantService::class, StudentService::class, StudyService::class],
    )
    fun `should register the service as a single transactional proxy`(type: Class<*>) {
        val services = context.getBeansOfType(type)

        assertEquals(1, services.size)
        assertTrue(AopUtils.isAopProxy(services.values.single()))
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
