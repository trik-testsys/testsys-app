@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import com.google.protobuf.Empty
import io.grpc.Context
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
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

@InternalGrpcApi
internal class GrpcNodeClient(private val channel: ManagedChannel, private val statusTimeout: Duration) : NodeClient {
    constructor(address: GradingNodeAddress, settings: GradingSettings) : this(
        ManagedChannelBuilder.forTarget(address.target).usePlaintext().maxInboundMessageSize(settings.maxMessageBytes).build(),
        settings.statusTimeout,
    )

    override fun poll(onStatus: (GradingNodeStatus) -> Unit) {
        val response = AtomicReference<Proto.Status?>()
        GradingNodeGrpc.newStub(channel).withDeadlineAfter(statusTimeout.toNanos(), TimeUnit.NANOSECONDS)
            .getStatus(
                Empty.getDefaultInstance(),
                object : StreamObserver<Proto.Status> {
                    override fun onNext(value: Proto.Status) {
                        response.set(value)
                    }
                    override fun onError(error: Throwable) = onStatus(GradingNodeStatus.Unreachable(error.message ?: "Status RPC failed"))
                    override fun onCompleted() {
                        val status = response.get()
                        onStatus(
                            if (status == null) {
                                GradingNodeStatus.Unreachable("Node completed status RPC without a response")
                            } else {
                                GradingNodeStatus.Available(queued = status.queued, capacity = status.capacity)
                            },
                        )
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
}
