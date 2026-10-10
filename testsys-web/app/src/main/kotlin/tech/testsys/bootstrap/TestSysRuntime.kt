package tech.testsys.bootstrap

import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import tech.testsys.web.app.TestSysApplication
import tech.testsys.web.service.ServiceWebConfiguration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns one non-web infrastructure parent and two servlet children; closing stops children before their shared resources.
 *
 * @property infrastructure the shared non-web parent.
 * @property mainWeb the main user interface.
 * @property serviceWeb the isolated service interface.
 * @since %CURRENT_VERSION%
 */
class TestSysRuntime private constructor(
    val infrastructure: ConfigurableApplicationContext,
    val mainWeb: ConfigurableApplicationContext,
    val serviceWeb: ConfigurableApplicationContext,
) : AutoCloseable {
    private val closed = AtomicBoolean()

    /**
     * Installs the single process hook after all contexts have started successfully.
     *
     * @since %CURRENT_VERSION%
     */
    fun installShutdownHook() {
        Runtime.getRuntime().addShutdownHook(Thread({ close() }, "testsys-shutdown"))
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            try {
                serviceWeb.close()
            } finally {
                try {
                    mainWeb.close()
                } finally {
                    infrastructure.close()
                }
            }
        }
    }

    companion object {
        /**
         * Starts the validated runtime; any failure closes already started contexts before propagating the exception.
         *
         * @since %CURRENT_VERSION%
         */
        fun start(args: Array<String>): TestSysRuntime = start(args, emptyArray())

        internal fun start(args: Array<String>, infrastructureSources: Array<Class<*>>): TestSysRuntime {
            val parentApplication = application(
                WebApplicationType.NONE,
                SharedInfrastructureConfiguration::class.java,
                *infrastructureSources,
            )
            parentApplication.addInitializers(
                ApplicationContextInitializer<ConfigurableApplicationContext> { context ->
                    context.beanFactory.registerSingleton("serviceWebSettings", ServiceWebSettings.from(context.environment))
                },
            )
            val parent = parentApplication.run(*args)
            var isStarted = false
            try {
                val main = child(parent, TestSysApplication::class.java, args, service = false)
                try {
                    val service = child(parent, ServiceWebConfiguration::class.java, args, service = true)
                    val runtime = TestSysRuntime(infrastructure = parent, mainWeb = main, serviceWeb = service)
                    isStarted = true
                    return runtime
                } finally {
                    if (!isStarted) main.close()
                }
            } finally {
                if (!isStarted) parent.close()
            }
        }

        private fun child(
            parent: ConfigurableApplicationContext,
            source: Class<*>,
            args: Array<String>,
            service: Boolean,
        ): ConfigurableApplicationContext {
            val application = application(WebApplicationType.SERVLET, source)
            val environment = WebContextEnvironment.create()
            application.setEnvironment(environment)
            application.addInitializers(
                ApplicationContextInitializer<ConfigurableApplicationContext> { context ->
                    context.parent = parent
                    environment.propertySources.addFirst(
                        MapPropertySource(
                            "testsysWebAutoConfiguration",
                            mapOf(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY to false),
                        ),
                    )
                    if (service) {
                        WebContextEnvironment.configureService(environment, parent.getBean(ServiceWebSettings::class.java))
                    }
                },
            )
            return application.run(*args)
        }

        private fun application(type: WebApplicationType, vararg sources: Class<*>): SpringApplication = SpringApplication(*sources).apply {
            setWebApplicationType(type)
            setRegisterShutdownHook(false)
        }
    }
}
