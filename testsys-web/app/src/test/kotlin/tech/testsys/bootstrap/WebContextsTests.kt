package tech.testsys.bootstrap

import com.vaadin.flow.server.VaadinServlet
import com.vaadin.flow.server.VaadinServletContext
import com.vaadin.flow.server.startup.ApplicationRouteRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.aop.framework.Advised
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.web.context.WebApplicationContext
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.infra.database.api.service.ServiceUserFilter
import tech.testsys.infra.database.api.service.ServiceUserSearch
import tech.testsys.infra.grpc.api.BalancingGrader
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingQueueMonitor
import tech.testsys.operation.TaskValidationDispatcher
import tech.testsys.web.app.PostgresTestContainer
import tech.testsys.web.app.TestSysApplication
import tech.testsys.web.app.service.user.UserService
import tech.testsys.web.app.view.AuthenticationView
import tech.testsys.web.service.ServiceWebConfiguration
import tech.testsys.web.service.view.ServiceLoginView
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.sql.DataSource

/**
 * Starts the whole runtime against the shared test database, passed only by its address: a container bean in the closed
 * hierarchy would stop the container shared with later tests.
 */
class WebContextsTests {
    @Test
    fun `should start isolated Vaadin children sharing infrastructure and stop all three listeners`() {
        val runtime = TestSysRuntime.start(arguments())
        val ports = listOf(
            checkNotNull((runtime.mainWeb as? WebServerApplicationContext)?.webServer).port,
            checkNotNull((runtime.serviceWeb as? WebServerApplicationContext)?.webServer).port,
            checkNotNull((runtime.metricsWeb as? WebServerApplicationContext)?.webServer).port,
        )
        runtime.use {
            val main = runtime.mainWeb
            val service = runtime.serviceWeb
            assertTrue(runtime.infrastructure.getBeansWithAnnotation(SpringBootApplication::class.java).isEmpty())
            assertEquals(
                listOf(main.getBean(TestSysApplication::class.java)),
                main.getBeansWithAnnotation(SpringBootApplication::class.java).values.toList(),
            )
            assertEquals(
                listOf(service.getBean(ServiceWebConfiguration::class.java)),
                service.getBeansWithAnnotation(SpringBootApplication::class.java).values.toList(),
            )
            assertEquals("true", runtime.infrastructure.environment.getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY))
            assertEquals("false", main.environment.getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY))
            assertEquals("false", service.environment.getProperty(EnableAutoConfiguration.ENABLED_OVERRIDE_PROPERTY))
            val mainServlet = (main as? WebApplicationContext)?.servletContext
            val serviceServlet = (service as? WebApplicationContext)?.servletContext
            assertNotSame(mainServlet, serviceServlet)
            assertNotSame(main.environment, service.environment)
            assertNotSame(main.environment.propertySources, service.environment.propertySources)
            assertSame(runtime.infrastructure, main.parent)
            assertSame(runtime.infrastructure, service.parent)
            val sharedTypes = listOf(
                DataSource::class.java,
                BalancingGrader::class.java,
                GradingNodeRegistry::class.java,
                GradingQueueMonitor::class.java,
                TaskValidationDispatcher::class.java,
                UserRepository::class.java,
                ServiceUserSearch::class.java,
            )
            for (type in sharedTypes) {
                assertSame(runtime.infrastructure.getBean(type), main.getBean(type))
                assertSame(main.getBean(type), service.getBean(type))
                assertTrue(main.getBeansOfType(type).isEmpty())
                assertTrue(service.getBeansOfType(type).isEmpty())
            }
            val firstUser = Pagination(page = 0, size = 1)
            assertSame(firstUser, service.getBean(ServiceUserSearch::class.java).find(ServiceUserFilter(), firstUser).pagination)
            assertNotSame(main.getBean("applicationTaskExecutor"), service.getBean("applicationTaskExecutor"))
            assertTrue(main.getBean(UserService::class.java) is Advised)
            assertFalse(service.containsBean("userService"))
            val mainRegistry = ApplicationRouteRegistry.getInstance(VaadinServletContext(checkNotNull(mainServlet)))
            val serviceRegistry = ApplicationRouteRegistry.getInstance(VaadinServletContext(checkNotNull(serviceServlet)))
            assertNotSame(mainRegistry, serviceRegistry)
            val mainVaadin = vaadin(main.getBean("servletRegistrationBean"))
            val serviceVaadin = vaadin(service.getBean("servletRegistrationBean"))
            assertNotSame(mainVaadin.service, serviceVaadin.service)
            for (servlet in listOf(mainVaadin, serviceVaadin)) {
                assertEquals(300, servlet.service.deploymentConfiguration.heartbeatInterval)
                assertFalse(servlet.service.deploymentConfiguration.isCloseIdleSessions)
            }
            assertEquals(AuthenticationView::class.java, mainRegistry.getNavigationTarget("login").orElseThrow())
            assertEquals(ServiceLoginView::class.java, serviceRegistry.getNavigationTarget("login").orElseThrow())
            val mainPort = (main as? WebServerApplicationContext)?.webServer?.port
            val servicePort = (service as? WebServerApplicationContext)?.webServer?.port
            HttpClient.newHttpClient().use { client ->
                val mainResponse = get(client, mainPort, "/login")
                val serviceResponse = get(client, servicePort, "/login")
                assertEquals(200, mainResponse.statusCode(), mainResponse.body())
                assertEquals(200, serviceResponse.statusCode(), serviceResponse.body())
                assertTrue(mainResponse.headers().firstValue("Set-Cookie").orElseThrow().startsWith("TESTSYS_APP_SESSION="))
                val cookie = serviceResponse.headers().firstValue("Set-Cookie").orElseThrow()
                assertTrue(cookie.startsWith("TESTSYS_SERVICE_SESSION="), cookie)
                assertTrue(cookie.contains("HttpOnly") && cookie.contains("SameSite=Lax"), cookie)
                assertFalse(cookie.contains("Secure"), cookie)
                assertFalse(cookie.contains("Domain="), cookie)
                val denied = get(client, servicePort, "/unknown-endpoint")
                assertEquals(302, denied.statusCode())
                assertTrue(denied.headers().firstValue("Location").orElseThrow().endsWith("/login"))
            }
        }
        assertFalse(runtime.infrastructure.isActive)
        assertFalse(runtime.mainWeb.isActive)
        assertFalse(runtime.serviceWeb.isActive)
        assertFalse(runtime.metricsWeb.isActive)
        for (port in ports) {
            ServerSocket(port).use { available -> assertEquals(port, available.localPort) }
        }
    }

    @Test
    fun `should serve the main interface under its context path and keep service and metrics at the root`() {
        TestSysRuntime.start(arguments("--server.servlet.context-path=/testsys")).use { runtime ->
            val mainPort = (runtime.mainWeb as? WebServerApplicationContext)?.webServer?.port
            val servicePort = (runtime.serviceWeb as? WebServerApplicationContext)?.webServer?.port
            val metricsPort = (runtime.metricsWeb as? WebServerApplicationContext)?.webServer?.port
            HttpClient.newHttpClient().use { client ->
                val mainResponse = get(client, mainPort, "/testsys/login")
                assertEquals(200, mainResponse.statusCode(), mainResponse.body())
                val mainCookie = mainResponse.headers().firstValue("Set-Cookie").orElseThrow()
                assertTrue(mainCookie.startsWith("TESTSYS_APP_SESSION="), mainCookie)
                assertTrue("Path=/testsys" in mainCookie.split("; "), mainCookie)
                assertEquals(200, get(client, mainPort, "/testsys/testsys-ui/styles/styles.css").statusCode())
                assertEquals(404, get(client, mainPort, "/login").statusCode())
                val serviceResponse = get(client, servicePort, "/login")
                assertEquals(200, serviceResponse.statusCode(), serviceResponse.body())
                val serviceCookie = serviceResponse.headers().firstValue("Set-Cookie").orElseThrow()
                assertTrue(serviceCookie.startsWith("TESTSYS_SERVICE_SESSION="), serviceCookie)
                assertTrue("Path=/" in serviceCookie.split("; "), serviceCookie)
                val metrics = get(client, metricsPort, "/actuator/prometheus", accept = "text/plain")
                assertEquals(200, metrics.statusCode(), metrics.body())
            }
        }
    }

    private fun arguments(vararg extra: String): Array<String> {
        val database = PostgresTestContainer.instance
        return arrayOf(
            "--server.port=0",
            "--spring.boot.enableautoconfiguration=true",
            "--spring.datasource.url=${database.jdbcUrl}",
            "--spring.datasource.username=${database.username}",
            "--spring.datasource.password=${database.password}",
            "--vaadin.productionMode=true",
            "--testsys.service.address=127.0.0.1",
            "--testsys.service.port=0",
            "--testsys.service.secret=${"test-secret-".repeat(8)}",
            *extra,
        )
    }

    private fun vaadin(bean: Any): VaadinServlet {
        val registration = checkNotNull(bean as? ServletRegistrationBean<*>)
        return checkNotNull(registration.servlet as? VaadinServlet)
    }

    private fun get(client: HttpClient, port: Int?, path: String, accept: String = "text/html"): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(URI("http://127.0.0.1:$port$path")).header("Accept", accept).GET().build(),
        HttpResponse.BodyHandlers.ofString(),
    )
}
