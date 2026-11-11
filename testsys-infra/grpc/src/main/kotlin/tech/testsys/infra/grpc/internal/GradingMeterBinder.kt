@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.MeterBinder
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingQueueMonitor
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.monitoring.api.TestSysMetrics

/**
 * Exposes the grading queue by state and the registry nodes by participation and status as gauges read at each scrape.
 * Node addresses never become labels: nodes are only counted and their queues and capacities summed.
 */
@InternalGrpcApi
internal class GradingMeterBinder(private val queue: GradingQueueMonitor, private val nodes: GradingNodeRegistry) : MeterBinder {
    override fun bindTo(registry: MeterRegistry) {
        QUEUE_STATES.forEach { (state, count) ->
            Gauge.builder(TestSysMetrics.GRADING_QUEUE, queue) { monitor -> count(monitor.snapshot()).toDouble() }
                .tag(TestSysMetrics.STATE, state)
                .strongReference(true)
                .register(registry)
        }

        PARTICIPATIONS.forEach { (participation, isActive) ->
            NODE_STATUSES.forEach { status ->
                Gauge.builder(TestSysMetrics.GRADING_NODES, nodes) { source -> count(source, isActive, status).toDouble() }
                    .tags(TestSysMetrics.PARTICIPATION, participation, TestSysMetrics.STATUS, status)
                    .strongReference(true)
                    .register(registry)
            }
        }

        Gauge.builder(TestSysMetrics.GRADING_NODES_QUEUED, nodes) { source -> available(source).sumOf { node -> node.queued }.toDouble() }
            .strongReference(true)
            .register(registry)
        Gauge.builder(TestSysMetrics.GRADING_NODES_CAPACITY, nodes) { source -> capacity(source).toDouble() }
            .strongReference(true)
            .register(registry)
    }

    private companion object {
        val QUEUE_STATES: List<Pair<String, (GradingQueueSnapshot) -> Int>> = listOf(
            "preparing" to GradingQueueSnapshot::preparing,
            "waiting" to GradingQueueSnapshot::waiting,
            "grading" to GradingQueueSnapshot::grading,
            "saving" to GradingQueueSnapshot::saving,
        )

        val PARTICIPATIONS: List<Pair<String, Boolean>> = listOf("active" to true, "inactive" to false)

        val NODE_STATUSES: List<String> = listOf("unknown", "available", "unavailable")

        fun count(source: GradingNodeRegistry, isActive: Boolean, status: String): Int =
            source.overview().nodes.count { entry -> entry.node.isActive == isActive && statusOf(entry.status) == status }

        fun statusOf(status: GradingNodeStatus): String = when (status) {
            GradingNodeStatus.Unknown -> "unknown"
            is GradingNodeStatus.Available -> "available"
            is GradingNodeStatus.Unreachable -> "unavailable"
        }

        fun capacity(source: GradingNodeRegistry): Int = available(source).sumOf { node -> node.capacity }

        fun available(source: GradingNodeRegistry): List<GradingNodeStatus.Available> = source.overview().nodes
            .filter { entry -> entry.node.isActive }
            .mapNotNull { entry -> entry.status as? GradingNodeStatus.Available }
    }
}
