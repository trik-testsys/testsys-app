package tech.testsys.web.error

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = ["vaadin.productionMode=true"])
class NotFoundHttpTests {
    @Value("\${local.server.port}")
    private lateinit var port: String

    @ParameterizedTest
    @ValueSource(strings = ["unknown-route", "dev/showcase", "dev/showcase/states/accessibility"])
    fun `should return HTTP 404 on initial request to missing or unavailable page`(path: String) {
        val request = HttpRequest.newBuilder(URI("http://localhost:$port/$path"))
            .header("Accept", "text/html").GET().build()

        val status = HttpClient.newHttpClient().use { client ->
            client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode()
        }

        assertEquals(404, status)
    }
}
