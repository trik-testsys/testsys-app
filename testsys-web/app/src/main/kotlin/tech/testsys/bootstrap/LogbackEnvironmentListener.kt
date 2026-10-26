package tech.testsys.bootstrap

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.LoggerContextListener
import ch.qos.logback.core.spi.ContextAwareBase
import ch.qos.logback.core.spi.LifeCycle
import org.springframework.core.env.Environment

/**
 * Logback listener that puts the Spring [Environment] back into the logger context when Logback rereads its
 * configuration, because the JSON console encoder of Spring Boot does not start without it.
 *
 * @since %CURRENT_VERSION%
 */
class LogbackEnvironmentListener : ContextAwareBase(), LoggerContextListener, LifeCycle {
    private var environment: Any? = null

    private var isRunning = false

    override fun start() {
        environment = context.getObject(ENVIRONMENT_KEY)
        isRunning = true
    }

    override fun stop() {
        isRunning = false
    }

    override fun isStarted(): Boolean = isRunning

    // The configuration that Logback reads after the reset declares a new listener.
    override fun isResetResistant(): Boolean = false

    // A reset clears the objects of the context before it notifies the listeners.
    override fun onReset(loggerContext: LoggerContext) {
        environment?.let { value -> loggerContext.putObject(ENVIRONMENT_KEY, value) }
    }

    override fun onStart(loggerContext: LoggerContext) = Unit
    override fun onStop(loggerContext: LoggerContext) = Unit
    override fun onLevelChange(logger: Logger, level: Level) = Unit

    private companion object {
        val ENVIRONMENT_KEY: String = Environment::class.java.name
    }
}
