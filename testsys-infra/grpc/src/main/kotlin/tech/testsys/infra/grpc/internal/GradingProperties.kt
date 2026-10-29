package tech.testsys.infra.grpc.internal

import org.springframework.boot.context.properties.bind.Name
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.settings.api.SettingsProperties
import java.time.Duration

/** Grading and node polling configuration, bound from `testsys.grading`; [addresses] are parsed from [nodes]. */
@InternalGrpcApi
@SettingsProperties("testsys.grading")
internal data class GradingProperties(
    val nodes: String,
    @param:Name("import-nodes") val shouldImportNodes: Boolean,
    val maxAttempts: Int,
    val totalTimeout: Duration,
    val rpcTimeout: Duration,
    val statusTimeout: Duration,
    val pollInterval: Duration,
    val maxMessageBytes: Int,
    val shouldRecordVideo: Boolean,
) {
    val addresses: List<GradingNodeAddress> = if (nodes.isBlank()) {
        emptyList()
    } else {
        nodes.split(',').map { target ->
            val address = target.trim()
            require(address.isNotEmpty()) { "testsys.grading.nodes must contain nonblank addresses separated by commas" }
            GradingNodeAddress(address)
        }
    }

    init {
        require(maxAttempts > 0) { "Grading maxAttempts must be positive: $maxAttempts" }
        require(listOf(totalTimeout, rpcTimeout, statusTimeout, pollInterval).all { duration -> duration > Duration.ZERO }) {
            "Grading durations must be positive"
        }
        require(maxMessageBytes > 0) { "Grading maxMessageBytes must be positive: $maxMessageBytes" }
    }
}
