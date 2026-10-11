package tech.testsys.infra.grpc.internal

import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.infra.settings.api.SettingKey
import tech.testsys.infra.settings.api.SettingsSnapshot
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
    val shouldImportNodes: Boolean,
) {
    init {
        require(maxAttempts > 0) { "Grading maxAttempts must be positive: $maxAttempts" }
        require(listOf(totalTimeout, rpcTimeout, statusTimeout, pollInterval).all { duration -> duration > Duration.ZERO }) {
            "Grading durations must be positive"
        }
        require(maxMessageBytes > 0) { "Grading maxMessageBytes must be positive: $maxMessageBytes" }
    }
}

@InternalGrpcApi
internal fun GradingSettings.resolved(snapshot: SettingsSnapshot): GradingSettings = copy(
    maxAttempts = snapshot.values.int(SettingKey.GradingMaxAttempts),
    totalTimeout = snapshot.values.duration(SettingKey.TotalTimeout),
    rpcTimeout = snapshot.values.duration(SettingKey.RpcTimeout),
    statusTimeout = snapshot.values.duration(SettingKey.StatusTimeout),
    pollInterval = snapshot.values.duration(SettingKey.PollInterval),
    maxMessageBytes = snapshot.activeMaxMessageBytes,
    shouldRecordVideo = snapshot.values.boolean(SettingKey.ShouldRecordVideo),
)
