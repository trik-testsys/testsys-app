package tech.testsys.web.service.security

import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.context.annotation.Bean
import tech.testsys.web.service.ServiceTestConfiguration
import tech.testsys.web.service.ServiceWebConfiguration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(
    classes = [ServiceWebConfiguration::class, ServiceTestConfiguration::class, ProxyProbeConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["vaadin.productionMode=true", "server.address=127.0.0.1", "server.forward-headers-strategy=none"],
)
class ServiceProxyHttpTests : ProxyHttpFixture() {
    @Test
    fun `should ignore forged forwarding headers from an untrusted peer`() {
        assertEquals("127.0.0.1|false", probe("198.51.100.5"))
    }
}

@SpringBootTest(
    classes = [ServiceWebConfiguration::class, ServiceTestConfiguration::class, ProxyProbeConfiguration::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "vaadin.productionMode=true",
        "server.address=127.0.0.1",
        "server.forward-headers-strategy=none",
        "testsys.service.trusted-proxies=127\\.0\\.0\\.1|10\\.0\\.0\\.1",
    ],
)
class TrustedServiceProxyHttpTests : ProxyHttpFixture() {
    @Test
    fun `should use the first untrusted address from the right of a verified proxy chain`() {
        assertEquals("198.51.100.5|true", probe("192.0.2.99, 198.51.100.5, 10.0.0.1"))
    }
}

abstract class ProxyHttpFixture {
    @Value("\${local.server.port}")
    private var port = 0

    protected fun probe(forwardedFor: String): String = HttpClient.newHttpClient().use { client ->
        client.send(
            HttpRequest.newBuilder(URI("http://127.0.0.1:$port/testsys-ui/proxy-probe"))
                .header("X-Forwarded-For", forwardedFor).header("X-Forwarded-Proto", "https")
                .header("Forwarded", "for=192.0.2.77;proto=https").GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        ).body()
    }
}

@TestConfiguration(proxyBeanMethods = false)
class ProxyProbeConfiguration {
    @Bean
    fun proxyProbe(): ServletRegistrationBean<HttpServlet> = ServletRegistrationBean(
        object : HttpServlet() {
            override fun doGet(request: HttpServletRequest, response: HttpServletResponse) {
                response.writer.print("${request.remoteAddr}|${request.isSecure}")
            }
        },
        "/testsys-ui/proxy-probe",
    )
}
