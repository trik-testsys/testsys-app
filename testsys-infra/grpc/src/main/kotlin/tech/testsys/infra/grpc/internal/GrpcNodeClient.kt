@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import com.google.protobuf.Empty
import io.grpc.Context
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.stub.StreamObserver
import org.apache.commons.logging.LogFactory
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import trik.testsys.grading.GradingNodeGrpc
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import trik.testsys.grading.GradingNodeOuterClass as Proto

@InternalGrpcApi
internal interface NodeClient : AutoCloseable {
    fun poll(onStatus: (GradingNodeStatus) -> Unit)
    fun poll(timeout: Duration, onStatus: (GradingNodeStatus) -> Unit) = poll(onStatus)
    fun grade(
        submission: Proto.Submission,
        timeout: Duration,
        onResult: (Proto.Result) -> Unit,
        onError: (Throwable) -> Unit,
    ): AutoCloseable
}

@InternalGrpcApi
internal fun interface NodeClientFactory {
    fun create(address: GradingNodeAddress): NodeClient
}

/**
 * Reason why a node is unreachable after [error]: the name of its gRPC status code, `UNKNOWN` for an error without
 * a gRPC status. Callers log the full description.
 */
@InternalGrpcApi
internal fun unreachableReason(error: Throwable): String = Status.fromThrowable(error).code.name

@InternalGrpcApi
internal class GrpcNodeClient(private val channel: ManagedChannel, private val statusTimeout: Duration) : NodeClient {
    private val log = LogFactory.getLog(javaClass)

    // Status code of the latest logged poll failure: the same code logs at DEBUG until a poll succeeds.
    private val loggedPollFailure = AtomicReference<Status.Code?>()

    constructor(address: GradingNodeAddress, settings: GradingSettings) : this(
        ManagedChannelBuilder.forTarget(address.target).usePlaintext().maxInboundMessageSize(settings.maxMessageBytes).build(),
        settings.statusTimeout,
    )

    override fun poll(onStatus: (GradingNodeStatus) -> Unit) = poll(statusTimeout, onStatus)

    override fun poll(timeout: Duration, onStatus: (GradingNodeStatus) -> Unit) {
        val response = AtomicReference<Proto.Status?>()
        GradingNodeGrpc.newStub(channel).withDeadlineAfter(timeout.toNanos(), TimeUnit.NANOSECONDS)
            .getStatus(
                Empty.getDefaultInstance(),
                object : StreamObserver<Proto.Status> {
                    override fun onNext(value: Proto.Status) {
                        response.set(value)
                    }
                    override fun onError(error: Throwable) = onStatus(pollFailure(error))
                    override fun onCompleted() {
                        val status = response.get()
                        if (status == null) {
                            val error = Status.INTERNAL.withDescription("Node completed status RPC without a response").asException()
                            onStatus(pollFailure(error))
                        } else {
                            loggedPollFailure.set(null)
                            onStatus(GradingNodeStatus.Available(queued = status.queued, capacity = status.capacity))
                        }
                    }
                },
            )
    }

    override fun grade(
        submission: Proto.Submission,
        timeout: Duration,
        onResult: (Proto.Result) -> Unit,
        onError: (Throwable) -> Unit,
    ): AutoCloseable {
        val context = Context.current().withCancellation()
        val response = AtomicReference<Proto.Result?>()
        context.run {
            GradingNodeGrpc.newStub(channel).withDeadlineAfter(timeout.toNanos(), TimeUnit.NANOSECONDS)
                .grade(
                    submission,
                    object : StreamObserver<Proto.Result> {
                        override fun onNext(value: Proto.Result) {
                            if (!response.compareAndSet(null, value)) {
                                onError(IllegalStateException("Node returned multiple grading responses"))
                                context.cancel(null)
                            }
                        }
                        override fun onError(error: Throwable) = onError(error)
                        override fun onCompleted() {
                            val result = response.get()
                            if (result == null) {
                                onError(IllegalStateException("Node completed grading without a response"))
                            } else {
                                onResult(result)
                            }
                        }
                    },
                )
        }
        return AutoCloseable { context.cancel(null) }
    }

    override fun close() {
        channel.shutdownNow()
    }

    private fun pollFailure(error: Throwable): GradingNodeStatus.Unreachable {
        val code = Status.fromThrowable(error).code
        val message = "Checking node ${channel.authority()} is unreachable"
        if (loggedPollFailure.getAndSet(code) != code) {
            log.warn(message, error)
        } else {
            log.debug("$message: ${error.message}")
        }

        return GradingNodeStatus.Unreachable(unreachableReason(error))
    }
}
