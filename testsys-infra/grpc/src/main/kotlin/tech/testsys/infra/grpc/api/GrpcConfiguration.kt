package tech.testsys.infra.grpc.api

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.env.Environment
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.grpc.internal.ConfiguredGradingNodeStore
import tech.testsys.infra.grpc.internal.GradingCoordinator
import tech.testsys.infra.grpc.internal.GradingNodeManager
import tech.testsys.infra.grpc.internal.GradingNodeRegistryCoordinator
import tech.testsys.infra.grpc.internal.GradingPersistenceService
import tech.testsys.infra.grpc.internal.GradingSettings
import tech.testsys.infra.grpc.internal.GrpcNodeClient
import tech.testsys.infra.grpc.internal.InternalGrpcApi
import tech.testsys.infra.grpc.internal.JsonLogParser
import tech.testsys.infra.grpc.internal.LogParser
import tech.testsys.infra.grpc.internal.MAX_GRADING_NODE_ADDRESS_LENGTH
import tech.testsys.infra.grpc.internal.NodeClientFactory
import tech.testsys.infra.grpc.internal.resolved
import tech.testsys.infra.settings.api.GradingNodeStore
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.StoredGradingNode
import java.time.Clock
import java.time.Duration
import java.util.concurrent.Executors

/**
 * Registers the grading adapter and its defaults, then restores the stored node registry, optionally importing
 * configured nodes first, before starting background work.
 * Repository ports, `TransactionOperations` and an optional node store are supplied by the application.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalGrpcApi::class)
@Configuration
@ComponentScan(basePackages = ["tech.testsys.infra.grpc.api", "tech.testsys.infra.grpc.internal"])
@PropertySource("classpath:grading-defaults.properties")
class GrpcConfiguration {
    @Bean
    internal fun gradingSettings(environment: Environment, runtime: RuntimeSettings? = null): GradingSettings {
        if (runtime != null) {
            val defaults = GradingSettings(
                nodes = environment.nodes(),
                maxAttempts = 1,
                totalTimeout = Duration.ofSeconds(1),
                rpcTimeout = Duration.ofSeconds(1),
                statusTimeout = Duration.ofSeconds(1),
                pollInterval = Duration.ofSeconds(1),
                maxMessageBytes = 1,
                shouldRecordVideo = false,
                shouldImportNodes = environment.shouldImportNodes(),
            )
            return defaults.resolved(runtime.snapshot())
        }
        return GradingSettings(
            nodes = environment.nodes(),
            maxAttempts = environment.getRequiredProperty("testsys.grading.max-attempts", Int::class.java),
            totalTimeout = environment.duration("total-timeout"),
            rpcTimeout = environment.duration("rpc-timeout"),
            statusTimeout = environment.duration("status-timeout"),
            pollInterval = environment.duration("poll-interval"),
            maxMessageBytes = environment.getRequiredProperty("testsys.grading.max-message-bytes", Int::class.java),
            shouldRecordVideo = environment.getRequiredProperty("testsys.grading.should-record-video", Boolean::class.java),
            shouldImportNodes = environment.shouldImportNodes(),
        )
    }

    @Bean
    internal fun nodeClientFactory(settings: GradingSettings): NodeClientFactory =
        NodeClientFactory { address -> GrpcNodeClient(address, settings) }

    @Bean(initMethod = "start", destroyMethod = "close")
    internal fun gradingCoordinator(
        persistence: GradingPersistenceService,
        settings: GradingSettings,
        parser: LogParser,
        nodeClientFactory: NodeClientFactory,
        runtime: RuntimeSettings? = null,
        nodeStore: GradingNodeStore? = null,
    ): GradingCoordinator {
        val stored = nodeStore?.read()
        val imports = if (stored != null && settings.shouldImportNodes) startupImports(settings.nodes, stored) else emptyList()
        val registry = stored?.withImports(imports)
            ?: settings.nodes.distinct().map { address -> StoredGradingNode(address, isActive = true) }

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
            runtime = runtime,
        )

        try {
            registry.forEach { node -> if (node.isActive) coordinator.addNode(node.address) else coordinator.observe(node.address) }
            if (nodeStore != null) imports.forEach { change -> nodeStore.write(change) }
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
    internal fun gradingNodeRegistry(
        coordinator: GradingCoordinator,
        settings: GradingSettings,
        nodeStore: GradingNodeStore? = null,
    ): GradingNodeRegistry = GradingNodeRegistryCoordinator(
        coordinator = coordinator,
        store = nodeStore ?: ConfiguredGradingNodeStore(settings.nodes),
        configured = settings.nodes,
    )

    @Bean
    internal fun gradingQueueMonitor(coordinator: GradingCoordinator): GradingQueueMonitor = GradingQueueMonitor(coordinator::queue)

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

    private fun Environment.shouldImportNodes(): Boolean = getRequiredProperty("testsys.grading.import-nodes", Boolean::class.java)

    private fun Environment.duration(name: String): Duration = Duration.parse(getRequiredProperty("testsys.grading.$name"))

    /** Plans the registry writes that add absent [configured] nodes and enable inactive ones, in configuration order. */
    private fun startupImports(configured: List<GradingNodeAddress>, stored: List<StoredGradingNode>): List<NodeImport> {
        configured.forEach { address ->
            require(address.target.length <= MAX_GRADING_NODE_ADDRESS_LENGTH) {
                "testsys.grading.nodes contains an address longer than $MAX_GRADING_NODE_ADDRESS_LENGTH characters: ${address.target}"
            }
        }

        val participation = stored.associate { node -> node.address to node.isActive }
        return configured.distinct().mapNotNull { address ->
            when (participation[address]) {
                null -> NodeImport.Add(address)
                false -> NodeImport.Enable(address)
                true -> null
            }
        }
    }

    /** Returns the registry as it will be after [imports]: enabled nodes become active, added ones follow. */
    private fun List<StoredGradingNode>.withImports(imports: List<NodeImport>): List<StoredGradingNode> {
        val enabled = imports.filterIsInstance<NodeImport.Enable>().map { change -> change.address }.toSet()
        val added = imports.filterIsInstance<NodeImport.Add>().map { change -> StoredGradingNode(change.address, isActive = true) }
        return map { node -> if (node.address in enabled) node.copy(isActive = true) else node } + added
    }

    private fun GradingNodeStore.write(change: NodeImport) {
        val target = change.address.target
        when (change) {
            is NodeImport.Add -> {
                check(add(StoredGradingNode(change.address, isActive = true))) {
                    "Checking node $target has a different stored record; startup import aborted"
                }
                log.info("Added configured checking node $target to the registry as active")
            }
            is NodeImport.Enable -> {
                check(setActive(change.address, isActive = true)) { "Checking node $target has no stored record; startup import aborted" }
                log.info("Enabled configured checking node $target in the registry")
            }
        }
    }

    /** Registry write of a configured node imported at startup. */
    private sealed interface NodeImport {
        val address: GradingNodeAddress

        data class Add(override val address: GradingNodeAddress) : NodeImport

        data class Enable(override val address: GradingNodeAddress) : NodeImport
    }

    private companion object {
        val log: Log = LogFactory.getLog(GrpcConfiguration::class.java)
    }
}
