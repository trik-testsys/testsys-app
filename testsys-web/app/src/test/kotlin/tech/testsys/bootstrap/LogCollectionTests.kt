package tech.testsys.bootstrap

import com.github.dockerjava.api.model.AccessMode
import com.github.dockerjava.api.model.Bind
import com.github.dockerjava.api.model.Volume
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean
import org.springframework.core.io.FileSystemResource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Path
import java.time.Duration
import java.util.Properties

class LogCollectionTests {
    @Test
    fun `should store JSON lines only of the app service of its own project`() {
        LiveStack().use { stack ->
            await().atMost(Duration.ofMinutes(3)).untilAsserted {
                val streams = stack.query("{project=\"$PROJECT\",service=\"app\",level=\"INFO\"} | json | user=\"42\"")
                assertEquals(listOf("/developer/tasks"), streams.map { stream -> stream.path("stream").path("route").asString() })
            }

            val sources = stack.query("{project=~\".+\"}").map { stream ->
                stream.path("stream").path("project").asString() to stream.path("stream").path("service").asString()
            }

            assertEquals(listOf(PROJECT to "app"), sources)
        }
    }

    @Test
    fun `should run the queries of the logs dashboard against the collected lines`() {
        LiveStack().use { stack ->
            await().atMost(Duration.ofMinutes(3)).untilAsserted {
                val lines = stack.query(stack.panelExpression(panel = 5)).flatMap { stream -> stream.path("values").toList() }

                assertEquals(DASHBOARD_LINE, lines.firstOrNull()?.get(1)?.asString())
            }

            stack.query(stack.panelExpression(panel = 1))
            stack.query(stack.panelExpression(panel = 2))
            stack.query(stack.panelExpression(panel = 3))
            stack.query(stack.panelExpression(panel = 4))
        }
    }

    private class LiveStack : AutoCloseable {
        private val deploy = Path.of(checkNotNull(System.getProperty("testsys.deploy.path")))
        private val compose = compose()
        private val mapper = JsonMapper.builder().build()
        private val client = HttpClient.newHttpClient()
        private val network = Network.newNetwork()
        private val sources = listOf(source(PROJECT, "app"), source(OTHER_PROJECT, "app"), source(PROJECT, "postgres"))
        private val loki = container("loki")
            .withNetwork(network)
            .withNetworkAliases("loki")
            .withCopyToContainer(Transferable.of(config("loki.yml")), "/etc/loki/loki.yml")
            .withExposedPorts(3100)
            // Loki reports readiness about 15 seconds after its ring is ready.
            .waitingFor(Wait.forHttp("/ready").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)))
        private val alloy = container("alloy")
            .withNetwork(network)
            .withEnv("TESTSYS_COMPOSE_PROJECT", PROJECT)
            .withCopyToContainer(Transferable.of(config("alloy.alloy")), "/etc/alloy/config.alloy")
            .withCreateContainerCmdModifier { command ->
                val socket = DockerClientFactory.instance().remoteDockerUnixSocketPath
                checkNotNull(command.hostConfig).withBinds(Bind(socket, Volume("/var/run/docker.sock"), AccessMode.ro))
            }
            .withExposedPorts(12_345)
            .waitingFor(Wait.forHttp("/-/ready").forStatusCode(200))

        init {
            try {
                // The sources start first, so the first discovery of Alloy finds them without waiting for a refresh.
                sources.forEach { source -> source.start() }
                loki.start()
                alloy.start()
            } catch (failure: RuntimeException) {
                close()
                throw failure
            }
        }

        fun query(expression: String): List<JsonNode> {
            val encoded = URLEncoder.encode(expression, UTF_8)
            val uri = URI("http://${loki.host}:${loki.getMappedPort(3100)}/loki/api/v1/query_range?query=$encoded")
            val response = client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString())
            assertEquals(200, response.statusCode(), response.body())
            return mapper.readTree(response.body()).path("data").path("result").toList()
        }

        override fun close() {
            try {
                (listOf(alloy, loki) + sources).forEach { container -> container.close() }
                network.close()
            } finally {
                client.close()
            }
        }

        // The expression of a panel of the logs dashboard with the default variables that Grafana substitutes.
        fun panelExpression(panel: Int): String = mapper.readTree(deploy.resolve("config/grafana/dashboards/testsys-logs.json").toFile())
            .path("panels").single { entry -> entry.path("id").asInt() == panel }
            .path("targets").single().path("expr").asString()
            .replace("\${level:regex}", "(ERROR|WARN|INFO)")
            .replace(Regex("\\$\\{(search|user|route|logger):regex}"), "")
            .replace("\$__range", "1h")
            .replace("\$__auto", "1m")

        private fun compose(): Properties {
            val factory = YamlPropertiesFactoryBean()
            factory.setResources(FileSystemResource(deploy.resolve("compose.monitoring.yml")))
            return checkNotNull(factory.getObject())
        }

        private fun config(name: String): String = deploy.resolve("config/$name").toFile().readText()

        private fun container(service: String): LogContainer {
            val command = generateSequence(0) { index -> index + 1 }
                .map { index -> compose.getProperty("services.$service.command[$index]") }
                .takeWhile { value -> value != null }
                .filterNotNull()
                .toList()
            return LogContainer(DockerImageName.parse(compose.getProperty("services.$service.image")))
                .withCommand(*command.toTypedArray())
        }

        // A container labelled like a service of a Compose project that writes a logstash JSON line every second.
        private fun source(project: String, service: String): LogContainer = LogContainer(DockerImageName.parse("postgres:17-alpine"))
            .withLabel("com.docker.compose.project", project)
            .withLabel("com.docker.compose.service", service)
            .withCommand("sh", "-c", "while true; do echo '$LINE'; sleep 1; done")
    }

    private class LogContainer(image: DockerImageName) : GenericContainer<LogContainer>(image)

    private companion object {
        const val PROJECT = "testsys-logtest"
        const val OTHER_PROJECT = "testsys-logtest-other"
        const val LINE = """{"@timestamp":"2026-10-13T12:00:00.000Z","@version":"1","message":"Opened /developer/tasks",""" +
            """"logger_name":"tech.testsys.web.app.config.RequestLogContext","thread_name":"http-nio-8080-exec-1",""" +
            """"level":"INFO","level_value":20000,"user":"42","route":"/developer/tasks"}"""
        const val DASHBOARD_LINE = "INFO  RequestLogContext  Opened /developer/tasks  [user 42]  [/developer/tasks]"
    }
}
