package tech.testsys.web.app.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import tech.testsys.web.app.PostgresTestConfiguration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Import(PostgresTestConfiguration::class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "vaadin.productionMode=true",
        "server.forward-headers-strategy=native",
        "testsys.web.rate-limit.requests=3",
        "testsys.web.rate-limit.duration=PT1H",
    ],
)
class RateLimitConfigurationHttpTests {
    @Value("\${local.server.port}")
    private lateinit var port: String

    @Test
    fun `should reject the request over the limit with 429`() {
        val client = "192.0.2.1"

        val statuses = List(4) { send(client) }

        assertEquals(listOf(200, 200, 200, 429), statuses)
    }

    @Test
    fun `should count clients behind the proxy by their forwarded address`() {
        repeat(4) { send("198.51.100.1") }

        val status = send("198.51.100.2")

        assertEquals(200, status)
    }

    private fun send(client: String): Int {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port/"))
            .header("Accept", "text/html")
            .header("X-Forwarded-For", client)
            .GET().build()

        return HttpClient.newHttpClient().use { it.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() }
    }
}
