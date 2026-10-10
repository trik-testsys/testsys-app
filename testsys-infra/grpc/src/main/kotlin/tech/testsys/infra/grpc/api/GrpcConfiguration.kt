package tech.testsys.infra.grpc.api

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
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
 * Registers the grading adapter and its defaults; repository ports and `TransactionOperations` are supplied by the
 * application.
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
        maxAttempts = environment.getRequiredProperty("testsys.grading.max-attempts", Int::class.java),
        totalTimeout = environment.duration("total-timeout"),
        rpcTimeout = environment.duration("rpc-timeout"),
        statusTimeout = environment.duration("status-timeout"),
        pollInterval = environment.duration("poll-interval"),
        maxMessageBytes = environment.getRequiredProperty("testsys.grading.max-message-bytes", Int::class.java),
        shouldRecordVideo = environment.getRequiredProperty("testsys.grading.should-record-video", Boolean::class.java),
    )

    @Bean(initMethod = "start", destroyMethod = "close")
    internal fun gradingCoordinator(
        persistence: GradingPersistenceService,
        settings: GradingSettings,
        parser: LogParser,
    ): GradingCoordinator {
        val nodes = GradingNodeManager(NodeClientFactory { address -> GrpcNodeClient(address, settings) })
        return GradingCoordinator(
            persistence = persistence,
            nodes = nodes,
            settings = settings,
            executor = Executors.newSingleThreadScheduledExecutor(),
            senderExecutor = Executors.newSingleThreadExecutor(),
            pollingExecutor = Executors.newSingleThreadScheduledExecutor(),
            clock = Clock.systemUTC(),
            parser = parser,
        )
    }

    @Bean
    internal fun logParser(): LogParser = JsonLogParser()

    private fun Environment.duration(name: String): Duration = Duration.parse(getRequiredProperty("testsys.grading.$name"))
}
