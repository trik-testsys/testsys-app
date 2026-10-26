package tech.testsys.bootstrap

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.core.OutputStreamAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment
import org.springframework.mock.env.MockEnvironment
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets.UTF_8

class LogbackEnvironmentListenerTests {
    private val context = LoggerContext()
    private val configuration = checkNotNull(javaClass.getResource("/logback.xml"))
    private val mapper = JsonMapper.builder().build()

    @AfterEach
    fun tearDown() {
        context.stop()
        System.clearProperty(FORMAT_PROPERTY)
    }

    @Test
    fun `should write user and route from MDC as JSON fields when the structured format is set`() {
        System.setProperty(FORMAT_PROPERTY, "logstash")
        context.putObject(Environment::class.java.name, MockEnvironment())
        configure()

        val line = encode()

        val json = mapper.readTree(line)
        assertEquals("Opened /developer/tasks", json.path("message").asString())
        assertEquals("INFO", json.path("level").asString())
        assertEquals("42", json.path("user").asString())
        assertEquals("/developer/tasks", json.path("route").asString())
    }

    @Test
    fun `should keep JSON output after Logback rereads its configuration`() {
        System.setProperty(FORMAT_PROPERTY, "logstash")
        context.putObject(Environment::class.java.name, MockEnvironment())
        configure()

        context.reset()
        configure()

        assertEquals("42", mapper.readTree(encode()).path("user").asString())
    }

    @Test
    fun `should write text lines without a Spring environment when the structured format is not set`() {
        configure()

        val line = encode()

        assertTrue(line.endsWith(": Opened /developer/tasks${System.lineSeparator()}"), line)
    }

    private fun configure() {
        val configurator = JoranConfigurator()
        configurator.context = context
        configurator.doConfigure(configuration)
    }

    // Encodes with the encoder of the console appender, without writing to the console of the test.
    private fun encode(): String {
        val appender = context.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("CONSOLE")
        val encoder = checkNotNull((appender as? OutputStreamAppender<ILoggingEvent>)?.encoder)
        val event = LoggingEvent(
            Logger::class.java.name,
            context.getLogger("tech.testsys.web.app.config.RequestLogContext"),
            Level.INFO,
            "Opened /developer/tasks",
            null,
            null,
        )
        event.setMDCPropertyMap(mapOf("user" to "42", "route" to "/developer/tasks"))
        return String(encoder.encode(event), UTF_8)
    }

    private companion object {
        const val FORMAT_PROPERTY = "CONSOLE_LOG_STRUCTURED_FORMAT"
    }
}
