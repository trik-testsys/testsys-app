@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import com.google.protobuf.Empty
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerServiceDefinition
import io.grpc.Status
import io.grpc.inprocess.InProcessChannelBuilder
import io.grpc.inprocess.InProcessServerBuilder
import io.grpc.stub.StreamObserver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.core.env.StandardEnvironment
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.infra.grpc.api.BalancingGrader
import trik.testsys.grading.GradingNodeGrpc
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import trik.testsys.grading.GradingNodeOuterClass as Proto

class GrpcNodeClientTests {
    @Test
    @Tag("regression")
    fun `should preserve ids above Int MAX through send reply persistence and callback`() {
        val repository = RepositoryFixture(testSubmission(id = 2_147_483_648L))
        val received = AtomicReference<Proto.Submission>()
        val serverName = "long-id"
        val server = InProcessServerBuilder.forName(serverName).directExecutor().addService(object : GradingNodeGrpc.GradingNodeImplBase() {
            override fun getStatus(request: Empty, responseObserver: StreamObserver<Proto.Status>) {
                responseObserver.onNext(Proto.Status.newBuilder().setCapacity(1).build())
                responseObserver.onCompleted()
            }
            override fun grade(request: Proto.Submission, responseObserver: StreamObserver<Proto.Result>) {
                received.set(request)
                responseObserver.onNext(result(id = request.id))
                responseObserver.onCompleted()
            }
        }).build().start()
        val channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        val queue = ManualExecutor()
        val polling = ManualExecutor()
        val sender = Executors.newSingleThreadExecutor()
        val client = GrpcNodeClient(channel, Duration.ofSeconds(1))
        val manager = GradingNodeManager(NodeClientFactory { client })
        val coordinator = GradingCoordinator(
            persistence = repository.persistence,
            nodes = manager,
            settings = settings,
            executor = queue.service,
            senderExecutor = sender,
            pollingExecutor = polling.service,
            clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC),
            parser = JsonLogParser(),
            environment = StandardEnvironment(),
        )
        val grader = BalancingGrader(coordinator)
        val callbacks = mutableListOf<Long>()
        grader.subscribeOnGraded { id ->
            assertTrue(repository.hasCommitted.get())
            callbacks.add(id.value)
        }
        coordinator.start()
        grader.addNode(GradingNodeAddress(serverName))
        polling.tick()
        try {
            val admission = grader.sendToGrade(repository.initial)
            queue.drain()
            queue.await { callbacks.size == 1 }

            assertEquals(GradingAdmission.Accepted, admission)
            assertEquals(2_147_483_648L, received.get().id)
            assertEquals("4", received.get().task.fieldsList.single().name)
            assertEquals("2025.1", received.get().options.dockerImage)
            assertEquals(listOf(2_147_483_648L), callbacks)
            val saved = repository.savedVerdicts.single()
            assertEquals(2_147_483_648L, saved.submission.id.value)
            assertEquals(4L, saved.testVerdicts.single().test.id.value)
            assertEquals(17, saved.testVerdicts.single().score.value)
            assertEquals(5L, saved.testVerdicts.single().logs.id.value)
            assertEquals(null, saved.testVerdicts.single().recording)
            assertTrue((repository.current.get().data.status as? SubmissionStatus.Graded)?.grade is GradingResult.Success)
        } finally {
            coordinator.close()
            assertTrue(sender.awaitTermination(5, TimeUnit.SECONDS))
            server.shutdownNow()
        }
    }

    @Test
    fun `should enforce real RPC deadlines on a silent node`() {
        val serverName = "deadline"
        val server = InProcessServerBuilder.forName(serverName).directExecutor().addService(object : GradingNodeGrpc.GradingNodeImplBase() {
            override fun grade(request: Proto.Submission, responseObserver: StreamObserver<Proto.Result>) = Unit
        }).build().start()
        val channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        val client = GrpcNodeClient(channel, Duration.ofMillis(20))
        val failure = AtomicReference<Throwable>()
        val completed = CountDownLatch(1)
        try {
            client.grade(
                submission = Proto.Submission.getDefaultInstance(),
                timeout = Duration.ofMillis(20),
                onResult = { throw AssertionError("Silent node produced a result") },
                onError = { error ->
                    failure.set(error)
                    completed.countDown()
                },
            )

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(Status.Code.DEADLINE_EXCEEDED, Status.fromThrowable(failure.get()).code)
        } finally {
            client.close()
            server.shutdownNow()
        }
    }

    @Test
    fun `should report only the status code of a failed status poll`() {
        val service = statusService { observer ->
            observer.onError(Status.UNAVAILABLE.withDescription("Node is draining").asRuntimeException())
        }

        val status = pollNode("draining", service)

        assertEquals(GradingNodeStatus.Unreachable("UNAVAILABLE"), status)
    }

    @Test
    fun `should report a status poll without a response by the INTERNAL code`() {
        // A unary server call rejects a completion without a response; a streaming one lets the node close with OK.
        val service = ServerServiceDefinition.builder(GradingNodeGrpc.SERVICE_NAME)
            .addMethod(
                GradingNodeGrpc.getGetStatusMethod().toBuilder().setType(MethodDescriptor.MethodType.SERVER_STREAMING).build(),
                ServerCallHandler<Empty, Proto.Status> { call, _ ->
                    call.close(Status.OK, Metadata())
                    object : ServerCall.Listener<Empty>() {}
                },
            )
            .build()

        val status = pollNode("no-status-response", service)

        assertEquals(GradingNodeStatus.Unreachable("INTERNAL"), status)
    }

    @Test
    fun `should report a timed-out status poll by its code`() {
        val service = statusService { _ -> }

        val status = pollNode("silent-status", service, statusTimeout = Duration.ofMillis(20))

        assertEquals(GradingNodeStatus.Unreachable("DEADLINE_EXCEEDED"), status)
    }

    private fun statusService(respond: (StreamObserver<Proto.Status>) -> Unit): ServerServiceDefinition =
        object : GradingNodeGrpc.GradingNodeImplBase() {
            override fun getStatus(request: Empty, responseObserver: StreamObserver<Proto.Status>) = respond(responseObserver)
        }.bindService()

    private fun pollNode(
        serverName: String,
        service: ServerServiceDefinition,
        statusTimeout: Duration = Duration.ofSeconds(1),
    ): GradingNodeStatus? {
        val server = InProcessServerBuilder.forName(serverName).directExecutor().addService(service).build().start()
        val channel = InProcessChannelBuilder.forName(serverName).directExecutor().build()
        val client = GrpcNodeClient(channel, statusTimeout)
        val received = AtomicReference<GradingNodeStatus>()
        val completed = CountDownLatch(1)
        try {
            client.poll { status ->
                received.set(status)
                completed.countDown()
            }
            assertTrue(completed.await(5, TimeUnit.SECONDS))
            return received.get()
        } finally {
            client.close()
            server.shutdownNow()
        }
    }
}
