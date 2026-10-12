package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.boot.web.server.context.WebServerInitializedEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import tech.testsys.web.app.PostgresTestContainer
import java.net.InetAddress
import java.net.ServerSocket

class TestSysRuntimeTests {
    @BeforeEach
    fun reset() {
        RuntimeProbeConfiguration.started.clear()
        RuntimeProbeConfiguration.infrastructure.clear()
        RuntimeProbeConfiguration.ports.clear()
    }

    @Test
    fun `should fail before starting infrastructure when the secret is missing`() {
        assertThrows(IllegalArgumentException::class.java) {
            TestSysRuntime.start(arrayOf("--testsys.service.secret="), arrayOf(RuntimeProbeConfiguration::class.java))
        }

        assertTrue(RuntimeProbeConfiguration.started.isEmpty())
        assertTrue(RuntimeProbeConfiguration.infrastructure.isEmpty())
    }

    @Test
    fun `should fail before starting infrastructure when the Grafana address is missing`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            TestSysRuntime.start(
                arrayOf(
                    "--testsys.service.secret=${"runtime-test-".repeat(8)}",
                    "--testsys.service.address=127.0.0.1",
                    "--testsys.service.port=0",
                    "--testsys.service.grafana-url=",
                ),
                arrayOf(RuntimeProbeConfiguration::class.java),
            )
        }

        assertTrue(checkNotNull(failure.message).startsWith("testsys.service.grafana-url"))
        assertTrue(RuntimeProbeConfiguration.started.isEmpty())
        assertTrue(RuntimeProbeConfiguration.infrastructure.isEmpty())
    }

    @Test
    fun `should close the main listener and parent when the service port is occupied`() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { occupied ->
            val failure = assertThrows(RuntimeException::class.java) {
                start(mainPort = 0, servicePort = occupied.localPort).close()
            }
            assertTrue(RuntimeProbeConfiguration.started.isNotEmpty(), failure.stackTraceToString())
        }

        val main = RuntimeProbeConfiguration.started.single()
        assertFalse(main.isActive)
        assertFalse((main.parent as? ConfigurableApplicationContext)?.isActive != false)
        ServerSocket(RuntimeProbeConfiguration.mainPort).use { available ->
            assertEquals(RuntimeProbeConfiguration.mainPort, available.localPort)
        }
    }

    @Test
    fun `should close both interfaces and the parent when the metrics port is occupied`() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { occupied ->
            assertThrows(RuntimeException::class.java) {
                start(mainPort = 0, servicePort = 0, metricsPort = occupied.localPort).close()
            }
        }

        assertEquals(2, RuntimeProbeConfiguration.started.size)
        assertTrue(RuntimeProbeConfiguration.started.none { context -> context.isActive })
        assertFalse(RuntimeProbeConfiguration.infrastructure.single().isActive)
        val (mainPort, servicePort) = RuntimeProbeConfiguration.ports
        ServerSocket(mainPort).use { available -> assertEquals(mainPort, available.localPort) }
        ServerSocket(servicePort).use { available -> assertEquals(servicePort, available.localPort) }
    }

    @Test
    fun `should close infrastructure when the main port is occupied`() {
        ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { occupied ->
            assertThrows(RuntimeException::class.java) { start(mainPort = occupied.localPort, servicePort = 0).close() }
        }

        assertTrue(RuntimeProbeConfiguration.started.isEmpty())
        assertFalse(RuntimeProbeConfiguration.infrastructure.single().isActive)
    }

    private fun start(mainPort: Int, servicePort: Int, metricsPort: Int = 0): TestSysRuntime {
        val database = PostgresTestContainer.instance
        return TestSysRuntime.start(
            arrayOf(
                "--server.port=$mainPort",
                "--server.address=127.0.0.1",
                "--vaadin.productionMode=true",
                "--spring.datasource.url=${database.jdbcUrl}",
                "--spring.datasource.username=${database.username}",
                "--spring.datasource.password=${database.password}",
                "--testsys.service.secret=${"runtime-test-".repeat(8)}",
                "--testsys.service.address=127.0.0.1",
                "--testsys.service.port=$servicePort",
                "--testsys.metrics.address=127.0.0.1",
                "--testsys.metrics.port=$metricsPort",
            ),
            arrayOf(RuntimeProbeConfiguration::class.java),
        )
    }
}

@TestConfiguration(proxyBeanMethods = false)
class RuntimeProbeConfiguration {
    @Bean
    fun serverStarts(context: ConfigurableApplicationContext): ApplicationListener<WebServerInitializedEvent> {
        infrastructure.add(context)
        return ApplicationListener { event ->
            val webContext = event.applicationContext
            if (webContext is ConfigurableApplicationContext) {
                started.add(webContext)
                mainPort = (webContext as? WebServerApplicationContext)?.webServer?.port ?: error("Missing test web server")
                ports.add(mainPort)
            }
        }
    }

    companion object {
        val started = mutableListOf<ConfigurableApplicationContext>()
        val infrastructure = mutableListOf<ConfigurableApplicationContext>()
        val ports = mutableListOf<Int>()
        var mainPort = 0
    }
}
