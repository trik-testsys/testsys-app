package tech.testsys.bootstrap

import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.web.context.support.StandardServletEnvironment
import tech.testsys.web.app.TestSysApplication
import tech.testsys.web.service.ServiceWebConfiguration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Owns one non-web infrastructure parent and three servlet children; closing stops children before their shared resources.
 *
 * @property infrastructure the shared non-web parent.
 * @property mainWeb the main user interface.
 * @property serviceWeb the isolated service interface.
 * @property metricsWeb the metrics server read by Prometheus.
 * @since %CURRENT_VERSION%
 */
class TestSysRuntime private constructor(
    val infrastructure: ConfigurableApplicationContext,
    val mainWeb: ConfigurableApplicationContext,
    val serviceWeb: ConfigurableApplicationContext,
    val metricsWeb: ConfigurableApplicationContext,
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
            closeInOrder(listOf(metricsWeb, serviceWeb, mainWeb, infrastructure))
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
                    context.beanFactory.registerSingleton("metricsWebSettings", MetricsWebSettings.from(context.environment))
                },
            )
            val parent = parentApplication.run(*args)
            val children = mutableListOf<ConfigurableApplicationContext>()
            var isStarted = false
            try {
                children.add(child(parent, TestSysApplication::class.java, args))
                children.add(
                    child(parent, ServiceWebConfiguration::class.java, args) { environment ->
                        WebContextEnvironment.configureService(environment, parent.getBean(ServiceWebSettings::class.java))
                    },
                )
                children.add(
                    child(parent, MetricsWebConfiguration::class.java, args) { environment ->
                        WebContextEnvironment.configureMetrics(environment, parent.getBean(MetricsWebSettings::class.java))
                    },
                )
                val runtime = TestSysRuntime(
                    infrastructure = parent,
                    mainWeb = children[0],
                    serviceWeb = children[1],
                    metricsWeb = children[2],
                )
                isStarted = true
                return runtime
            } finally {
                if (!isStarted) closeInOrder(children.asReversed() + parent)
            }
        }

        private fun child(
            parent: ConfigurableApplicationContext,
            source: Class<*>,
            args: Array<String>,
            configure: (StandardServletEnvironment) -> Unit = {},
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
                    configure(environment)
                },
            )
            return application.run(*args)
        }

        private fun application(type: WebApplicationType, vararg sources: Class<*>): SpringApplication = SpringApplication(*sources).apply {
            setWebApplicationType(type)
            setRegisterShutdownHook(false)
        }

        /** Closes [contexts] in the given order; a failing close still lets the following contexts close. */
        private fun closeInOrder(contexts: List<ConfigurableApplicationContext>) {
            if (contexts.isEmpty()) return

            try {
                contexts.first().close()
            } finally {
                closeInOrder(contexts.drop(1))
            }
        }
    }
}
