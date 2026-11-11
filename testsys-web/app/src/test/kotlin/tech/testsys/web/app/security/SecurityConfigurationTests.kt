package tech.testsys.web.app.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import tech.testsys.web.app.MockSpringVaadinTests
import tech.testsys.web.app.service.CurrentUser
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

@SpringBootTest
class SecurityConfigurationTests : MockSpringVaadinTests() {
    @Autowired
    private lateinit var currentUser: CurrentUser

    @Autowired
    @Qualifier(TaskExecutionAutoConfiguration.APPLICATION_TASK_EXECUTOR_BEAN_NAME)
    private lateinit var executor: Executor

    @Test
    fun `should give background work of a page the signed-in user`() {
        val user = fixtures.userOf(UserKind.MULTIPLE_ROLE)
        signIn(user)
        val testThread = Thread.currentThread()

        val (thread, result) = CompletableFuture.supplyAsync({ Thread.currentThread() to currentUser.user() }, executor).get()

        assertNotEquals(testThread, thread)
        assertEquals(user.id, result.id)
    }
}
