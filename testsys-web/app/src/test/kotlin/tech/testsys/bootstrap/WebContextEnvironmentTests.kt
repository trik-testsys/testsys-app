package tech.testsys.bootstrap

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.StandardEnvironment
import org.springframework.core.env.SystemEnvironmentPropertySource
import org.springframework.mock.env.MockEnvironment
import java.nio.file.Path
import kotlin.io.path.writeText

class WebContextEnvironmentTests {
    @Test
    fun `should override main listener and cookie settings only in the service environment`() {
        val main = WebContextEnvironment.create()
        val service = WebContextEnvironment.create()
        val mainProperties = mapOf<String, Any>(
            "server.port" to 8080,
            "server.servlet.session.cookie.name" to "TESTSYS_APP_SESSION",
            "server.servlet.session.cookie.domain" to "example.test",
            "server.forward-headers-strategy" to "framework",
        )
        main.propertySources.addFirst(MapPropertySource("mainProperties", mainProperties))
        service.propertySources.addFirst(MapPropertySource("mainProperties", mainProperties))
        val settings = ServiceWebSettings.from(
            MockEnvironment().withProperty("testsys.service.secret", "X".repeat(64))
                .withProperty("testsys.service.address", "127.0.0.1").withProperty("testsys.service.port", "9090"),
        )

        WebContextEnvironment.configureService(service, settings)

        assertNotSame(main.propertySources, service.propertySources)
        assertEquals("8080", main.getProperty("server.port"))
        assertEquals("9090", service.getProperty("server.port"))
        assertEquals("TESTSYS_APP_SESSION", main.getProperty("server.servlet.session.cookie.name"))
        assertEquals("TESTSYS_SERVICE_SESSION", service.getProperty("server.servlet.session.cookie.name"))
        assertEquals("", service.getProperty("server.servlet.session.cookie.domain"))
        assertEquals("none", service.getProperty("server.forward-headers-strategy"))
    }

    @Test
    fun `should apply command line before environment before file configuration`(@TempDir directory: Path) {
        val file = directory.resolve("application.properties")
        file.writeText(
            "testsys.service.secret=${"F".repeat(64)}\n" +
                "testsys.service.address=127.0.0.1\n" +
                "testsys.service.port=1010\n",
        )
        val environment = StandardEnvironment().apply {
            propertySources.replace(
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                SystemEnvironmentPropertySource(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                    mapOf("TESTSYS_SERVICE_SECRET" to "E".repeat(64), "TESTSYS_SERVICE_PORT" to "2020"),
                ),
            )
        }
        val application = SpringApplication(EnvironmentProbeConfiguration::class.java).apply {
            setWebApplicationType(WebApplicationType.NONE)
            setRegisterShutdownHook(false)
            setEnvironment(environment)
        }

        application.run("--spring.config.location=${file.toUri()}", "--testsys.service.port=3030").use { context ->
            val settings = ServiceWebSettings.from(context.environment)
            assertEquals(3030, settings.port)
            assertEquals("127.0.0.1", settings.address)
            assertEquals(true, settings.matches("E".repeat(64)))
        }
    }
}

@Configuration(proxyBeanMethods = false)
class EnvironmentProbeConfiguration
