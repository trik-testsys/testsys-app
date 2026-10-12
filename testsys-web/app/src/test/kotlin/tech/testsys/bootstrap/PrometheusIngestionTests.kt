package tech.testsys.bootstrap

import com.github.dockerjava.api.model.ExposedPort
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.boot.web.server.context.WebServerApplicationContext
import org.springframework.core.io.FileSystemResource
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.DockerImageName
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.web.app.PostgresTestContainer
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.time.Duration
import java.util.Properties

// Review of uncommitted changes against c15e049, Findings, Issue 1: verify ingestion, not just exported timestamps.
@Tag("regression")
class PrometheusIngestionTests {
    @Test
    fun `should include first events in dashboard totals tables and rates without counting repeated scrapes`() {
        LiveStack().use { stack ->
            stack.recordEvents()

            stack.awaitTotals(1.0)

            stack.assertFirstZero()
            stack.assertHttpTable(events = 1.0)
            stack.assertRates()
            stack.awaitSamples(3.0)
            stack.assertTotals(1.0)
            stack.recordEvents()
            stack.awaitHttpCount(2.0)
            stack.awaitTotals(2.0)
            stack.assertHttpTable(events = 2.0)
        }
    }

    @Test
    fun `should include equal counter values after a TestSys restart and preserve history after a Prometheus restart`() {
        LiveStack().use { stack ->
            stack.recordEvents()
            stack.awaitTotals(1.0)

            stack.restartRuntime()
            stack.recordEvents()

            stack.awaitReset()
            stack.awaitTotals(2.0)
            stack.assertHttpTable(events = 2.0)
            assertEquals(listOf(1.0), stack.values("resets($HTTP_SERIES[5m])"))
            stack.restartPrometheus()
            stack.awaitTotals(2.0)
            stack.awaitSamples(5.0)
            stack.assertTotals(2.0)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["without_created", "without_zero_ingestion"])
    fun `should reproduce the missing first event when a required setting is disabled`(setting: String) {
        LiveStack(hasCreatedTimestamps = setting != "without_created", zeroIngestion = setting != "without_zero_ingestion").use { stack ->
            stack.recordEvents()

            stack.awaitSamples(2.0)

            stack.assertTotals(0.0)
            assertEquals(listOf(0.0), stack.values("increase($HTTP_SERIES[5m])"))
        }
    }

    @Test
    fun `should reproduce missing HTTP creation time with the current Protobuf exporter`() {
        LiveStack(protocol = "PrometheusProto").use { stack ->
            stack.recordEvents()

            stack.awaitTotals(1.0)
            stack.awaitSamples(2.0)

            assertEquals(listOf(0.0), stack.values("increase($HTTP_SERIES[5m])"))
        }
    }

    private class LiveStack(
        private val hasCreatedTimestamps: Boolean = true,
        zeroIngestion: Boolean = true,
        protocol: String = "OpenMetricsText1.0.0",
    ) : AutoCloseable {
        private val deploy = Path.of(checkNotNull(System.getProperty("testsys.deploy.path")))
        private val mapper = JsonMapper.builder().build()
        private val client = HttpClient.newHttpClient()
        private var runtime = startRuntime(0)
        private val port = checkNotNull((runtime.metricsWeb as? WebServerApplicationContext)?.webServer).port
        private val prometheus = container(zeroIngestion = zeroIngestion, protocol = protocol)
        private var prometheusPort = 0

        init {
            Testcontainers.exposeHostPorts(port)
            try {
                prometheus.start()
                prometheusPort = prometheus.getMappedPort(9090)
            } catch (failure: RuntimeException) {
                close()
                throw failure
            }
        }

        fun recordEvents() {
            val registry = runtime.infrastructure.getBean(MeterRegistry::class.java)
            runtime.infrastructure.getBean(TechnicalFailures::class.java).record(source = "grading", stage = "save")
            registry.counter(
                TestSysMetrics.OPERATION_ERRORS,
                TestSysMetrics.OPERATION,
                "IngestionService.fail",
                TestSysMetrics.KIND,
                "not_found",
                TestSysMetrics.ERROR,
                "TaskNotExistsError",
            ).increment()
            registry.counter(TestSysMetrics.TRANSACTION_RETRIES).increment()
            registry.counter(TestSysMetrics.GRADING_CHECKS, TestSysMetrics.RESULT, "verdict").increment()
            recordHttp(registry = registry, status = "404", outcome = "CLIENT_ERROR")
            recordHttp(registry = registry, status = "500", outcome = "SERVER_ERROR")
        }

        fun awaitTotals(expected: Double) {
            await().atMost(Duration.ofSeconds(90)).untilAsserted { assertTotals(expected) }
        }

        fun assertTotals(expected: Double) {
            assertEquals(listOf(expected), panelValues(file = "testsys-failures.json", panel = 2))
            assertEquals(listOf(expected), panelValues(file = "testsys-failures.json", panel = 6))
            assertEquals(listOf(expected), panelValues(file = "testsys-failures.json", panel = 10))
            assertEquals(listOf(expected), panelValues(file = "testsys-grading.json", panel = 7))
        }

        fun assertHttpTable(events: Double) {
            assertEquals(listOf(2.0 * events), panelValues(file = "testsys-http.json", panel = 6, target = "A"))
            assertEquals(listOf(events), panelValues(file = "testsys-http.json", panel = 6, target = "B"))
            assertEquals(0.5, panelValues(file = "testsys-http.json", panel = 6, target = "C").single(), 0.001, "4xx share")
            assertEquals(listOf(events), panelValues(file = "testsys-http.json", panel = 6, target = "D"))
            assertEquals(0.5, panelValues(file = "testsys-http.json", panel = 6, target = "E").single(), 0.001, "5xx share")
            assertTrue(panelValues(file = "testsys-http.json", panel = 6, target = "F").single() > 0)
        }

        fun assertRates() {
            assertPositive(panelValues(file = "testsys-failures.json", panel = 3))
            assertPositive(panelValues(file = "testsys-failures.json", panel = 7))
            assertPositive(panelValues(file = "testsys-failures.json", panel = 11))
            assertPositive(panelValues(file = "testsys-grading.json", panel = 8))
            assertPositive(panelValues(file = "testsys-http.json", panel = 1))
            assertPositive(panelValues(file = "testsys-http.json", panel = 2))
            assertPositive(panelValues(file = "testsys-http.json", panel = 3))
            assertPositive(panelValues(file = "testsys-http.json", panel = 4))
            assertPositive(panelValues(file = "testsys-http.json", panel = 5))
            assertPositive(panelValues(file = "testsys-failures.json", panel = 4))
            assertPositive(panelValues(file = "testsys-failures.json", panel = 8))
        }

        fun assertFirstZero() {
            val points = query("$HTTP_SERIES[5m]").single().path("values")
            assertEquals(0.0, points.first().path(1).asString().toDouble())
            assertEquals(1.0, points.last().path(1).asString().toDouble())
        }

        fun awaitSamples(expected: Double) {
            await().atMost(Duration.ofSeconds(90)).untilAsserted {
                val samples = values("count_over_time($HTTP_SERIES[5m])")
                assertTrue(samples.size == 1 && samples.single() >= expected)
            }
        }

        fun awaitHttpCount(expected: Double) {
            await().atMost(Duration.ofSeconds(90)).untilAsserted {
                assertEquals(listOf(expected), values(HTTP_SERIES))
            }
        }

        fun awaitReset() {
            await().atMost(Duration.ofSeconds(90)).untilAsserted {
                assertEquals(listOf(1.0), values("resets($HTTP_SERIES[5m])"))
            }
        }

        fun restartRuntime() {
            runtime.close()
            runtime = startRuntime(port)
        }

        fun restartPrometheus() {
            prometheus.dockerClient.restartContainerCmd(prometheus.containerId).exec()
            // Docker assigns another dynamic host port on restart; Testcontainers keeps its original inspection.
            val ports = prometheus.dockerClient.inspectContainerCmd(prometheus.containerId).exec().networkSettings.ports
            prometheusPort = checkNotNull(ports.bindings[ExposedPort.tcp(9090)]).first().hostPortSpec.toInt()
            await().atMost(Duration.ofSeconds(90)).ignoreExceptionsMatching { failure -> failure is IOException }.untilAsserted {
                assertEquals(listOf(1.0), values("up{job=\"testsys\"}"))
            }
        }

        fun values(expression: String): List<Double> = query(expression).toList().map { result ->
            result.path("value").path(1).asString().toDouble()
        }

        override fun close() {
            try {
                prometheus.close()
            } finally {
                runtime.close()
                client.close()
            }
        }

        private fun recordHttp(registry: MeterRegistry, status: String, outcome: String) {
            Timer.builder("http.server.requests")
                .tags("interface", "main", "uri", "/monitoring-first-event", "method", "GET", "status", status, "outcome", outcome)
                .tag("exception", "none")
                .register(registry)
                .record(Duration.ofMillis(250))
        }

        private fun assertPositive(result: List<Double>) {
            assertTrue(result.isNotEmpty() && result.all { value -> value > 0 }, result.toString())
        }

        private fun panelValues(file: String, panel: Int, target: String = "A"): List<Double> {
            val dashboard = mapper.readTree(deploy.resolve("config/grafana/dashboards/$file").toFile())
            val definition = dashboard.path("panels").single { entry -> entry.path("id").asInt() == panel }
                .path("targets").single { entry -> entry.path("refId").asString() == target }
            val expression = definition.path("expr").asString()
                .replace("\$__range", "5m")
                .replace("\${smoothing:raw}", "5m")
                .replace("\$interface", "main")
            return values(expression)
        }

        private fun query(expression: String): JsonNode {
            val encoded = URLEncoder.encode(expression, UTF_8)
            val uri = URI("http://${prometheus.host}:$prometheusPort/api/v1/query?query=$encoded")
            val request = HttpRequest.newBuilder(uri)
                .GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            assertEquals(200, response.statusCode(), response.body())
            val body = mapper.readTree(response.body())
            assertEquals("success", body.path("status").asString(), response.body())
            return body.path("data").path("result")
        }

        private fun container(zeroIngestion: Boolean, protocol: String): GenericContainer<*> {
            val compose = YamlPropertiesFactoryBean().apply { setResources(FileSystemResource(deploy.resolve("compose.monitoring.yml"))) }
            val properties = checkNotNull(compose.getObject())
            val command = command(properties).filter { argument -> zeroIngestion || argument != ZERO_INGESTION }
            val config = deploy.resolve("config/prometheus.yml").toFile().readText()
                .replace("app:8082", "host.testcontainers.internal:$port")
                .replace("[OpenMetricsText1.0.0]", "[$protocol]")
            return PrometheusContainer(DockerImageName.parse(properties.getProperty("services.prometheus.image")))
                .withExposedPorts(9090)
                .withCopyToContainer(Transferable.of(config), "/etc/prometheus/prometheus.yml")
                .withCommand(*command.toTypedArray())
                .waitingFor(Wait.forHttp("/-/ready"))
        }

        private fun command(properties: Properties): List<String> = generateSequence(0) { index -> index + 1 }
            .map { index -> properties.getProperty("services.prometheus.command[$index]") }
            .takeWhile { value -> value != null }
            .filterNotNull()
            .toList()

        private fun startRuntime(metricsPort: Int): TestSysRuntime {
            val database = PostgresTestContainer.instance
            return TestSysRuntime.start(
                arrayOf(
                    "--server.port=0",
                    "--server.address=127.0.0.1",
                    "--spring.datasource.url=${database.jdbcUrl}",
                    "--spring.datasource.username=${database.username}",
                    "--spring.datasource.password=${database.password}",
                    "--vaadin.productionMode=true",
                    "--testsys.service.address=127.0.0.1",
                    "--testsys.service.port=0",
                    "--testsys.service.secret=${"ingestion-secret-".repeat(5)}",
                    "--testsys.metrics.address=127.0.0.1",
                    "--testsys.metrics.port=$metricsPort",
                ) + if (hasCreatedTimestamps) {
                    emptyArray()
                } else {
                    arrayOf("--management.prometheus.metrics.export.properties[io.prometheus.exporter.include_created_timestamps]=false")
                },
            )
        }
    }

    private class PrometheusContainer(image: DockerImageName) : GenericContainer<PrometheusContainer>(image)

    private companion object {
        const val ZERO_INGESTION = "--enable-feature=created-timestamp-zero-ingestion"
        const val HTTP_SERIES = "http_server_requests_seconds_count{job=\"testsys\",uri=\"/monitoring-first-event\",status=\"500\"}"
    }
}
