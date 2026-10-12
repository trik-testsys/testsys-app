@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.infra.grpc.api.GradingNodeEntry
import tech.testsys.infra.grpc.api.GradingNodeRegistry
import tech.testsys.infra.grpc.api.GradingNodeRegistryOverview
import tech.testsys.infra.grpc.api.GradingQueueMonitor
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.infra.settings.api.SettingsPersistence
import tech.testsys.infra.settings.api.StoredGradingNode

internal class GradingMeterBinderTests {
    private val meters = SimpleMeterRegistry()
    private var snapshot = GradingQueueSnapshot(preparing = 1, waiting = 2, grading = 3, saving = 4)
    private val nodes = mockk<GradingNodeRegistry>()

    @BeforeEach
    fun bind() {
        every { nodes.overview() } returns GradingNodeRegistryOverview(
            listOf(
                entry("grader-1:50051", isActive = true, GradingNodeStatus.Available(queued = 2, capacity = 4)),
                entry("grader-2:50051", isActive = true, GradingNodeStatus.Available(queued = 1, capacity = 3)),
                entry("grader-3:50051", isActive = true, GradingNodeStatus.Unreachable("UNAVAILABLE")),
                entry("grader-4:50051", isActive = false, GradingNodeStatus.Available(queued = 5, capacity = 8)),
                entry("grader-5:50051", isActive = false, GradingNodeStatus.Unknown),
            ),
            SettingsPersistence.Known,
        )
        GradingMeterBinder(GradingQueueMonitor { snapshot }, nodes).bindTo(meters)
    }

    @Test
    fun `should report unfinished runs by state`() {
        assertEquals(
            mapOf("preparing" to 1.0, "waiting" to 2.0, "grading" to 3.0, "saving" to 4.0),
            listOf("preparing", "waiting", "grading", "saving").associateWith { state ->
                meters.get(TestSysMetrics.GRADING_QUEUE).tag(TestSysMetrics.STATE, state).gauge().value()
            },
        )
    }

    @Test
    fun `should read the queue at each scrape`() {
        snapshot = GradingQueueSnapshot(preparing = 0, waiting = 7, grading = 0, saving = 0)

        assertEquals(7.0, meters.get(TestSysMetrics.GRADING_QUEUE).tag(TestSysMetrics.STATE, "waiting").gauge().value())
    }

    @Test
    fun `should count nodes by participation and status`() {
        assertEquals(2.0, nodes("active", "available"))
        assertEquals(1.0, nodes("active", "unavailable"))
        assertEquals(0.0, nodes("active", "unknown"))
        assertEquals(1.0, nodes("inactive", "available"))
        assertEquals(1.0, nodes("inactive", "unknown"))
    }

    @Test
    fun `should sum the queue and capacity of available participating nodes only`() {
        assertEquals(3.0, meters.get(TestSysMetrics.GRADING_NODES_QUEUED).gauge().value())
        assertEquals(7.0, meters.get(TestSysMetrics.GRADING_NODES_CAPACITY).gauge().value())
    }

    @Test
    fun `should not use node addresses as label values`() {
        val labels = meters.meters.flatMap { meter -> meter.id.tags }.map { tag -> tag.value }

        assertFalse(labels.any { value -> "grader" in value })
    }

    private fun nodes(participation: String, status: String): Double = meters.get(TestSysMetrics.GRADING_NODES)
        .tags(TestSysMetrics.PARTICIPATION, participation, TestSysMetrics.STATUS, status)
        .gauge().value()

    private fun entry(address: String, isActive: Boolean, status: GradingNodeStatus) =
        GradingNodeEntry(StoredGradingNode(GradingNodeAddress(address), isActive), status)
}
