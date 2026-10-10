package tech.testsys.infra.grpc.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.grpc.internal.GradingCoordinator
import tech.testsys.infra.grpc.internal.GradingNodeManager
import tech.testsys.infra.grpc.internal.GradingPersistenceService
import tech.testsys.infra.grpc.internal.GradingSettings
import tech.testsys.infra.grpc.internal.GrpcNodeClient
import tech.testsys.infra.grpc.internal.InternalGrpcApi
import tech.testsys.infra.grpc.internal.JsonLogParser
import tech.testsys.infra.grpc.internal.LogParser
import tech.testsys.infra.grpc.internal.NodeClientFactory
import java.time.Clock
import java.time.Duration
import java.util.concurrent.Executors

/**
 * Registers the grading adapter, its defaults and configured nodes before starting background work.
 * Repository ports and `TransactionOperations` are supplied by the application.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalGrpcApi::class)
@Configuration
@ComponentScan(basePackages = ["tech.testsys.infra.grpc.api", "tech.testsys.infra.grpc.internal"])
@PropertySource("classpath:grading-defaults.properties")
class GrpcConfiguration {
    @Bean
    internal fun gradingSettings(environment: Environment): GradingSettings = GradingSettings(
        nodes = environment.nodes(),
        maxAttempts = environment.getRequiredProperty("testsys.grading.max-attempts", Int::class.java),
        totalTimeout = environment.duration("total-timeout"),
        rpcTimeout = environment.duration("rpc-timeout"),
        statusTimeout = environment.duration("status-timeout"),
        pollInterval = environment.duration("poll-interval"),
        maxMessageBytes = environment.getRequiredProperty("testsys.grading.max-message-bytes", Int::class.java),
        shouldRecordVideo = environment.getRequiredProperty("testsys.grading.should-record-video", Boolean::class.java),
    )

    @Bean
    internal fun nodeClientFactory(settings: GradingSettings): NodeClientFactory =
        NodeClientFactory { address -> GrpcNodeClient(address, settings) }

    @Bean(initMethod = "start", destroyMethod = "close")
    internal fun gradingCoordinator(
        persistence: GradingPersistenceService,
        settings: GradingSettings,
        parser: LogParser,
        nodeClientFactory: NodeClientFactory,
    ): GradingCoordinator {
        val nodes = GradingNodeManager(nodeClientFactory)
        val coordinator = GradingCoordinator(
            persistence = persistence,
            nodes = nodes,
            settings = settings,
            executor = Executors.newSingleThreadScheduledExecutor(),
            senderExecutor = Executors.newSingleThreadExecutor(),
            pollingExecutor = Executors.newSingleThreadScheduledExecutor(),
            clock = Clock.systemUTC(),
            parser = parser,
        )

        try {
            settings.nodes.forEach(coordinator::addNode)
        } catch (error: RuntimeException) {
            try {
                coordinator.close()
            } catch (closeError: RuntimeException) {
                error.addSuppressed(closeError)
            }

            throw error
        }

        return coordinator
    }

    @Bean
    internal fun logParser(): LogParser = JsonLogParser()

    private fun Environment.nodes(): List<GradingNodeAddress> {
        val configured = getRequiredProperty("testsys.grading.nodes")
        if (configured.isBlank()) {
            return emptyList()
        }

        return configured.split(',').map { target ->
            val address = target.trim()
            require(address.isNotEmpty()) { "testsys.grading.nodes must contain nonblank addresses separated by commas" }
            GradingNodeAddress(address)
        }
    }

    private fun Environment.duration(name: String): Duration = Duration.parse(getRequiredProperty("testsys.grading.$name"))
}
