package tech.testsys.web.app.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.session.SessionRegistry
import tech.testsys.web.app.PostgresTestConfiguration
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Import(PostgresTestConfiguration::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["vaadin.productionMode=true"])
class SecurityConfigurationHttpTests {
    @Value("\${local.server.port}")
    private lateinit var port: String

    @Autowired
    private lateinit var sessionRegistry: SessionRegistry

    @ParameterizedTest
    @ValueSource(strings = ["", "login", "registration", "home", "participant"])
    fun `should let a guest load any page and leave the access check to navigation`(path: String) {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port/$path"))
            .header("Accept", "text/html").GET().build()

        val status = HttpClient.newHttpClient().use { client ->
            client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
        }

        assertEquals(200, status)
    }

    @Test
    fun `should sign an expired session out and send it to the root on its next request`() {
        HttpClient.newHttpClient().use { client ->
            val first = client.send(page("login"), HttpResponse.BodyHandlers.discarding())
            val cookie = first.headers().firstValue("Set-Cookie").orElseThrow().substringBefore(';')
            val sessionId = cookie.substringAfter('=')
            sessionRegistry.registerNewSession(sessionId, CabinetPrincipal(userId = EXPIRED_USER_ID, kind = UserKind.MULTIPLE_ROLE))
            checkNotNull(sessionRegistry.getSessionInformation(sessionId)).expireNow()

            val next = client.send(page("login", cookie), HttpResponse.BodyHandlers.discarding())

            assertEquals(302, next.statusCode())
            assertEquals("/", URI(next.headers().firstValue("Location").orElseThrow()).path)
        }
    }

    private fun page(path: String, cookie: String? = null): HttpRequest = HttpRequest.newBuilder(URI("http://localhost:$port/$path"))
        .header("Accept", "text/html")
        .apply { cookie?.let { value -> header("Cookie", value) } }
        .GET()
        .build()

    private companion object {
        const val EXPIRED_USER_ID = 42L
    }
}
