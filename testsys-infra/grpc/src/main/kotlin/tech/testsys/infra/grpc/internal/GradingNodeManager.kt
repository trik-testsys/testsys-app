@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import org.apache.commons.logging.LogFactory
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Observes availability of the node at [address] through its own status channel, independently of grading participation. */
@InternalGrpcApi
internal class NodeMonitor(private val address: GradingNodeAddress, val client: NodeClient) {
    // A new holder per change lets a poll publish only over the observation it started from, even for equal statuses.
    private val observation = AtomicReference(Observation(GradingNodeStatus.Unknown))
    private val isPolling = AtomicBoolean(false)
    private val log = LogFactory.getLog(javaClass)

    fun status(): GradingNodeStatus = observation.get().status

    fun reset() = observation.set(Observation(GradingNodeStatus.Unknown))

    fun markUnreachable(reason: String) = observation.set(Observation(GradingNodeStatus.Unreachable(reason)))

    fun poll(timeout: Duration?) {
        if (!isPolling.compareAndSet(false, true)) {
            return
        }

        val previous = observation.get()
        try {
            val onStatus: (GradingNodeStatus) -> Unit = { status ->
                observation.compareAndSet(previous, Observation(status))
                isPolling.set(false)
            }
            if (timeout == null) client.poll(onStatus) else client.poll(timeout, onStatus)
        } catch (error: RuntimeException) {
            log.warn("Cannot poll checking node ${address.target}", error)
            observation.compareAndSet(previous, Observation(GradingNodeStatus.Unreachable(unreachableReason(error))))
            isPolling.set(false)
        }
    }

    private class Observation(val status: GradingNodeStatus)
}

/** Participation of one node in grading through its own grading channel; a new instance marks a new participation. */
@InternalGrpcApi
internal class GradingRegistration(val client: NodeClient)

@InternalGrpcApi
internal class GradingNodeManager(private val factory: NodeClientFactory) : AutoCloseable {
    private val monitors = ConcurrentHashMap<GradingNodeAddress, NodeMonitor>()
    private val registrations = ConcurrentHashMap<GradingNodeAddress, GradingRegistration>()
    private val changes = Any()
    private val log = LogFactory.getLog(javaClass)

    fun observe(address: GradingNodeAddress) = synchronized(changes) {
        requireAddress(address)
        if (!monitors.containsKey(address)) {
            monitors[address] = NodeMonitor(address, factory.create(address))
        }
    }

    fun forget(address: GradingNodeAddress) = synchronized(changes) {
        monitors.remove(address)?.client?.let(::closeQuietly)
    }

    fun register(address: GradingNodeAddress): Boolean = synchronized(changes) {
        requireAddress(address)
        if (registrations.containsKey(address)) {
            return false
        }

        val registration = GradingRegistration(factory.create(address))
        monitors[address]?.reset()
        registrations[address] = registration
        true
    }

    fun unregister(address: GradingNodeAddress): GradingRegistration? = synchronized(changes) {
        registrations.remove(address)?.also { registration -> closeQuietly(registration.client) }
    }

    fun registration(address: GradingNodeAddress): GradingRegistration? = registrations[address]

    fun participantStatuses(): Map<GradingNodeAddress, GradingNodeStatus> =
        registrations.keys.associateWith { address -> monitors[address]?.status() ?: GradingNodeStatus.Unknown }

    fun observedStatuses(): Map<GradingNodeAddress, GradingNodeStatus> = monitors.mapValues { (_, monitor) -> monitor.status() }

    fun markUnavailable(address: GradingNodeAddress, registration: GradingRegistration, reason: String) = synchronized(changes) {
        if (registrations[address] === registration) {
            monitors[address]?.markUnreachable(reason)
        }
    }

    fun poll(timeout: Duration? = null) = pollWithTimeout { timeout }

    fun poll(timeout: () -> Duration) = pollWithTimeout(timeout)

    override fun close() = synchronized(changes) {
        registrations.keys.toList().forEach(::unregister)
        monitors.keys.toList().forEach(::forget)
    }

    private fun pollWithTimeout(timeout: () -> Duration?) {
        monitors.values.forEach { monitor -> monitor.poll(timeout()) }
    }

    private fun requireAddress(address: GradingNodeAddress) =
        require(address.target.isNotBlank()) { "Checking node address must not be blank" }

    private fun closeQuietly(client: NodeClient) {
        try {
            client.close()
        } catch (error: RuntimeException) {
            log.warn("Cannot close checking node channel", error)
        }
    }
}
