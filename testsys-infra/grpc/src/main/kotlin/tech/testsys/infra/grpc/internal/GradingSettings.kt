package tech.testsys.infra.grpc.internal

import tech.testsys.domain.contract.GradingNodeAddress
import java.time.Duration

@InternalGrpcApi
internal data class GradingSettings(
    val nodes: List<GradingNodeAddress>,
    val maxAttempts: Int,
    val totalTimeout: Duration,
    val rpcTimeout: Duration,
    val statusTimeout: Duration,
    val pollInterval: Duration,
    val maxMessageBytes: Int,
    val shouldRecordVideo: Boolean,
) {
    init {
        require(maxAttempts > 0) { "Grading maxAttempts must be positive: $maxAttempts" }
        require(listOf(totalTimeout, rpcTimeout, statusTimeout, pollInterval).all { duration -> duration > Duration.ZERO }) {
            "Grading durations must be positive"
        }
        require(maxMessageBytes > 0) { "Grading maxMessageBytes must be positive: $maxMessageBytes" }
    }
}
