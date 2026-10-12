package tech.testsys.bootstrap

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.context.ConfigurableApplicationContext
import tech.testsys.web.app.PostgresTestContainer
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Checks HTTP metrics of both interfaces and their export on the real context hierarchy. The database is passed by
 * its address: a container bean in the closed hierarchy would stop the container shared with later tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MetricsHttpTests {
    private lateinit var runtime: TestSysRuntime
    private val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()

    @BeforeAll
    fun start() {
        val database = PostgresTestContainer.instance
        runtime = TestSysRuntime.start(
            arrayOf(
                "--server.port=0",
                "--server.address=127.0.0.1",
                "--spring.datasource.url=${database.jdbcUrl}",
                "--spring.datasource.username=${database.username}",
                "--spring.datasource.password=${database.password}",
                "--vaadin.productionMode=true",
                "--testsys.service.address=127.0.0.1",
                "--testsys.service.port=0",
                "--testsys.service.secret=${"metrics-secret-".repeat(5)}",
                "--testsys.metrics.address=127.0.0.1",
                "--testsys.metrics.port=0",
            ),
        )
    }

    @AfterAll
    fun stop() {
        runtime.close()
        client.close()
    }

    @Test
    fun `should count the first navigation to a page by interface and path`() {
        val response = get(runtime.mainWeb, "/login")

        assertEquals(200, response.statusCode())
        assertTrue(awaitSeries("interface" to "main", "uri" to "/login", "status" to "200", "outcome" to "SUCCESS"))
    }

    @Test
    fun `should export latency buckets of a request`() {
        get(runtime.mainWeb, "/login")

        assertTrue(awaitSeries("interface" to "main", "uri" to "/login", series = BUCKET_SERIES, bound = "0.25"))
    }

    @Test
    fun `should name a page with a parameter by its route template without the value`() {
        get(runtime.mainWeb, "/developer/tasks/918273645")

        assertTrue(awaitSeries("interface" to "main", "uri" to "/developer/tasks/:taskId"))
        assertFalse("918273645" in scrape().body())
    }

    @Test
    fun `should name an optional section parameter by its template`() {
        get(runtime.mainWeb, "/admin/users/736251/judge")

        assertTrue(awaitSeries("interface" to "main", "uri" to "/admin/users/:userId/:section?"))
        assertFalse("736251" in scrape().body())
    }

    @Test
    fun `should count a missing page as not found without its path`() {
        val response = get(runtime.mainWeb, "/no-such/564738291")

        assertEquals(404, response.statusCode())
        assertTrue(awaitSeries("interface" to "main", "uri" to "NOT_FOUND", "status" to "404", "outcome" to "CLIENT_ERROR"))
        assertFalse("564738291" in scrape().body())
    }

    @ParameterizedTest
    @CsvSource(
        "/?v-r=heartbeat&v-uiId=0, /?v-r=heartbeat",
        "/?v-r=uidl&v-uiId=0, /?v-r=uidl",
        "/?v-r=unexpected-82736, /?v-r=other",
    )
    fun `should count framework requests by their kind`(path: String, endpoint: String) {
        post(runtime.mainWeb, path)

        assertTrue(awaitSeries("interface" to "main", "uri" to endpoint))
        assertFalse("82736" in scrape().body())
    }

    @ParameterizedTest
    @CsvSource(
        "/VAADIN/push/6151, /VAADIN/push, 6151",
        "/VAADIN/dynamic/resource/0/6152/report.csv, /VAADIN/dynamic/**, 6152",
        "/VAADIN/static/missing-6153.js, /VAADIN/**, 6153",
        "/testsys-ui/missing-6154.css, /testsys-ui/**, 6154",
    )
    fun `should count resource requests by their prefix`(path: String, endpoint: String, token: String) {
        get(runtime.mainWeb, path)

        assertTrue(awaitSeries("interface" to "main", "uri" to endpoint))
        assertFalse(token in scrape().body())
    }

    @Test
    fun `should name a service page by its route template`() {
        get(runtime.serviceWeb, "/users/4242")

        assertTrue(awaitSeries("interface" to "service", "uri" to "/users/:userId"))
        assertFalse("4242" in scrape().body())
    }

    @Test
    fun `should count a redirected service request without its path`() {
        val response = get(runtime.serviceWeb, "/unknown-endpoint")

        assertEquals(302, response.statusCode())
        assertTrue(awaitSeries("interface" to "service", "uri" to "REDIRECTION", "status" to "302"))
        assertFalse("unknown-endpoint" in scrape().body())
    }

    @Test
    fun `should export metrics without a browser session`() {
        val response = scrape()

        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/plain"))
        assertTrue(response.headers().allValues("Set-Cookie").isEmpty())
        assertTrue("testsys_grading_queue{state=\"waiting\"}" in response.body())
    }

    @Test
    fun `should not observe requests of the metrics server`() {
        scrape()

        assertFalse("uri=\"/actuator/prometheus\"" in scrape().body())
    }

    @Test
    fun `should not expose metrics on the main interface`() {
        val response = get(runtime.mainWeb, "/actuator/prometheus")

        assertEquals(404, response.statusCode())
        assertFalse("testsys_grading_queue" in response.body())
    }

    @Test
    fun `should not expose metrics on the service interface`() {
        val response = get(runtime.serviceWeb, "/actuator/prometheus")

        assertNotEquals(200, response.statusCode())
        assertFalse("testsys_grading_queue" in response.body())
    }

    private fun get(context: ConfigurableApplicationContext, path: String): HttpResponse<String> =
        client.send(request(context, path).GET().build(), HttpResponse.BodyHandlers.ofString())

    private fun post(context: ConfigurableApplicationContext, path: String): HttpResponse<String> = client.send(
        request(context, path).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString("{}")).build(),
        HttpResponse.BodyHandlers.ofString(),
    )

    private fun scrape(): HttpResponse<String> = client.send(
        HttpRequest.newBuilder(uri(runtime.metricsWeb, "/actuator/prometheus")).GET().build(),
        HttpResponse.BodyHandlers.ofString(),
    )

    private fun request(context: ConfigurableApplicationContext, path: String): HttpRequest.Builder =
        HttpRequest.newBuilder(uri(context, path)).header("Accept", "text/html")

    private fun uri(context: ConfigurableApplicationContext, path: String): URI =
        URI("http://127.0.0.1:${checkNotNull((context as? WebServerApplicationContext)?.webServer).port}$path")

    /**
     * Waits until the request timer with [labels] is registered, which happens when an observation stops after the
     * response may already be sent, then looks for its [series] in the export, with the upper [bound] of a bucket if set.
     */
    private fun awaitSeries(vararg labels: Pair<String, String>, series: String = COUNT_SERIES, bound: String? = null): Boolean {
        val registry = runtime.infrastructure.getBean(MeterRegistry::class.java)
        val tags = labels.map { (name, value) -> Tag.of(name, value) }
        val added = CountDownLatch(1)
        registry.config().onMeterAdded { meter ->
            if (meter.id.name == HTTP_SERVER_REQUESTS && meter.id.tags.containsAll(tags)) added.countDown()
        }
        val isRegistered = registry.find(HTTP_SERVER_REQUESTS).tags(tags).timer() != null || added.await(5, TimeUnit.SECONDS)

        val expected = labels.map { (name, value) -> "$name=\"$value\"" } + listOfNotNull(bound?.let { value -> "le=\"$value\"" })
        return isRegistered &&
            scrape().body().lines().any { line -> line.startsWith("$series{") && expected.all { label -> label in line } }
    }

    private companion object {
        const val HTTP_SERVER_REQUESTS = "http.server.requests"
        const val COUNT_SERIES = "http_server_requests_seconds_count"
        const val BUCKET_SERIES = "http_server_requests_seconds_bucket"
    }
}
