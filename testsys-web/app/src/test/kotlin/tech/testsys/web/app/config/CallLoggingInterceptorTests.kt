package tech.testsys.web.app.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.aop.framework.ProxyFactory
import tech.testsys.domain.model.task.TaskId
import tech.testsys.operation.error.OperationException
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.web.app.error.operationFailure

class CallLoggingInterceptorTests {
    private val logger = assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(InnerService::class.java))
    private val outerLogger = assertInstanceOf(Logger::class.java, LoggerFactory.getLogger(OuterService::class.java))
    private val appender = ListAppender<ILoggingEvent>()
    private val inner = proxy(InnerService())
    private val outer = proxy(OuterService(inner))

    @BeforeEach
    fun attachAppender() {
        appender.start()
        listOf(logger, outerLogger).forEach { target ->
            target.addAppender(appender)
            target.level = Level.DEBUG
        }
    }

    @AfterEach
    fun detachAppender() {
        listOf(logger, outerLogger).forEach { target ->
            target.detachAppender(appender)
            target.level = null
        }
    }

    @Test
    fun `should write the arguments and the result of a call`() {
        inner.succeed(name = "Задача", accessToken = "K7mQ2xP9")

        val messages = messages()
        assertEquals("call InnerService.succeed(name=Задача, accessToken=***)", messages[0])
        assertTrue(Regex("""done InnerService\.succeed in \d+ ms: \[Задача]""").matches(messages[1]), messages[1])
    }

    @Test
    fun `should write nothing when DEBUG is disabled`() {
        logger.level = Level.INFO

        inner.succeed(name = "Задача", accessToken = "K7mQ2xP9")

        assertEquals(emptyList<String>(), messages())
    }

    @Test
    fun `should write the error of a failed operation and rethrow it unchanged`() {
        val failure = operationFailure(TaskNotExistsError(TaskId(918)))

        val thrown = assertThrows(OperationException::class.java) { inner.fail(failure) }

        assertSame(failure, thrown)
        assertTrue(Regex("""fail InnerService\.fail in \d+ ms: TaskNotExistsError\(.*918.*\)""").matches(messages()[1]), messages()[1])
    }

    @Test
    fun `should write only the type of another exception`() {
        val failure = IllegalStateException("Storage at /secret/path unavailable")

        val thrown = assertThrows(IllegalStateException::class.java) { inner.fail(failure) }

        assertSame(failure, thrown)
        assertTrue(Regex("""fail InnerService\.fail in \d+ ms: java\.lang\.IllegalStateException""").matches(messages()[1]), messages()[1])
    }

    @Test
    fun `should write only the type of an error leaving the call`() {
        val failure = StackOverflowError()

        val thrown = assertThrows(StackOverflowError::class.java) { inner.fail(failure) }

        assertSame(failure, thrown)
        assertTrue(Regex("""fail InnerService\.fail in \d+ ms: java\.lang\.StackOverflowError""").matches(messages()[1]), messages()[1])
    }

    @Test
    fun `should strip the suffix of a mangled method name`() {
        inner.view(TaskId(1))

        assertEquals("call InnerService.view(taskId=1)", messages()[0])
    }

    @Test
    fun `should write a nested call separately`() {
        outer.delegate("Задача")

        val calls = messages().filter { message -> message.startsWith("call ") }
        assertEquals(listOf("call OuterService.delegate(name=Задача)", "call InnerService.succeed(name=Задача, accessToken=***)"), calls)
    }

    @Test
    fun `should return the result when an argument cannot be rendered`() {
        val unrenderable = object {
            // The broken rendering is the scenario under test.
            @Suppress("ExceptionRaisedInUnexpectedLocation")
            override fun toString(): String = throw IllegalStateException("Broken toString")
        }

        val result = inner.accept(unrenderable)

        assertEquals(1, result)
        assertEquals("call InnerService.accept(<unrenderable: IllegalStateException>)", messages()[0])
    }

    private fun messages(): List<String> = appender.list.map { event -> event.formattedMessage }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> proxy(target: T): T = ProxyFactory(target).apply {
        isProxyTargetClass = true
        addAdvice(CallLoggingInterceptor())
    }.proxy as T

    open class InnerService {
        open fun succeed(name: String, accessToken: String): List<String> = listOf(name)

        open fun fail(failure: Throwable): Nothing = throw failure

        open fun view(taskId: TaskId): Long = taskId.value

        open fun accept(item: Any): Int = 1
    }

    open class OuterService(private val inner: InnerService) {
        open fun delegate(name: String): List<String> = inner.succeed(name = name, accessToken = "K7mQ2xP9")
    }
}
