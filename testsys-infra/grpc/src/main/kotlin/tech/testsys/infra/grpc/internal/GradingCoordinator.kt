@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.grpc.Status
import org.apache.commons.logging.LogFactory
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@InternalGrpcApi
internal class GradingCoordinator(
    private val persistence: GradingPersistenceService,
    private val nodes: GradingNodeManager,
    private val settings: GradingSettings,
    private val executor: ScheduledExecutorService,
    private val senderExecutor: ExecutorService,
    private val pollingExecutor: ScheduledExecutorService,
    private val clock: Clock,
    private val parser: LogParser,
) : AutoCloseable {
    private val pending = ConcurrentHashMap<SubmissionId, Pending>()
    private val submissions = LinkedBlockingQueue<Pending.Queued>()
    private val senderWakeup = Semaphore(0)
    private val subscribers = CopyOnWriteArrayList<(SubmissionId) -> Unit>()
    private val isClosed = AtomicBoolean(false)
    private val log = LogFactory.getLog(javaClass)

    fun start() {
        senderExecutor.execute(::sendLoop)
        pollingExecutor.scheduleWithFixedDelay(
            {
                nodes.poll()
                senderWakeup.release()
            },
            0,
            settings.pollInterval.toNanos(),
            TimeUnit.NANOSECONDS,
        )
    }

    fun submit(submission: Submission): GradingAdmission = synchronized(isClosed) {
        check(!isClosed.get()) { "Grader is closed" }
        val context = RunContext(submission = submission, acceptedAt = clock.instant())
        val newRun = Pending.Preparing(context)
        val existing = pending.putIfAbsent(submission.id, newRun)
        if (existing == null) {
            dispatch {
                context.deadline.set(
                    executor.schedule({ expire(context) }, remaining(context).coerceAtLeast(Duration.ZERO).toNanos(), TimeUnit.NANOSECONDS),
                )
                prepare(newRun)
            }
            GradingAdmission.Accepted
        } else {
            GradingAdmission.AlreadyPending
        }
    }

    fun subscribe(onGraded: (SubmissionId) -> Unit) {
        subscribers.add(onGraded)
    }

    fun addNode(address: GradingNodeAddress) = synchronized(isClosed) {
        check(!isClosed.get()) { "Grader is closed" }
        nodes.add(address)
        senderWakeup.release()
    }

    fun removeNode(address: GradingNodeAddress) = synchronized(isClosed) {
        val removed = nodes.find(address)
        nodes.remove(address)
        dispatch {
            pending.values.forEach { run ->
                when (run) {
                    is Pending.InFlight -> if (run.attempt.node === removed) {
                        retry(run, CheckedResult.Failure("Checking node was removed"))
                    }
                    is Pending.Preparing, is Pending.Queued, is Pending.Saving -> Unit
                }
            }
            senderWakeup.release()
        }
    }

    fun statuses(): Map<GradingNodeAddress, GradingNodeStatus> = nodes.statuses()

    override fun close() = synchronized(isClosed) {
        if (isClosed.compareAndSet(false, true)) {
            pending.values.forEach { run ->
                run.context.deadline.get()?.cancel(false)
                when (run) {
                    is Pending.InFlight -> run.attempt.cancel.get()?.close()
                    is Pending.Saving -> run.saveRetry.get()?.cancel(false)
                    is Pending.Preparing, is Pending.Queued -> Unit
                }
            }
            senderExecutor.shutdownNow()
            executor.shutdownNow()
            pollingExecutor.shutdownNow()
            nodes.close()
        }
    }

    private fun sendLoop() {
        try {
            while (!isClosed.get()) {
                sendWhenAvailable(submissions.take())
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    private fun sendWhenAvailable(run: Pending.Queued) {
        while (!isClosed.get()) {
            senderWakeup.drainPermits()
            val completed = CompletableFuture<Duration?>()
            dispatch {
                try {
                    completed.complete(send(run))
                } catch (error: Exception) {
                    log.error("Failed to send submission ${run.context.submission.id.value}", error)
                    completed.complete(minOf(settings.pollInterval, remaining(run.context)).coerceAtLeast(Duration.ZERO))
                }
            }
            val wait = completed.get() ?: return
            senderWakeup.tryAcquire(wait.toNanos(), TimeUnit.NANOSECONDS)
        }
    }

    private fun expire(context: RunContext) {
        val current = pending[context.submission.id]
        if (current != null && current.context === context && !isClosed.get()) {
            when (current) {
                is Pending.Active -> finish(current, CheckedResult.Timeout)
                is Pending.Saving -> Unit
            }
        }
    }

    private fun prepare(run: Pending.Preparing) {
        try {
            val prepared = persistence.prepare(run.context.submission, settings.shouldRecordVideo)
            val waiting = Pending.Queued(context = run.context, prepared = prepared, attempts = 0)
            pending[run.context.submission.id] = waiting
            submissions.add(waiting)
        } catch (error: RuntimeException) {
            finish(run, CheckedResult.Failure("Cannot prepare submission ${run.context.submission.id.value}: ${error.message}"))
        }
    }

    private fun send(run: Pending.Queued): Duration? {
        if (!isCurrent(run)) {
            return null
        }
        if (remaining(run.context) <= Duration.ZERO) {
            finish(run, CheckedResult.Timeout)
            return null
        }
        val outstanding = pending.values.mapNotNull { entry ->
            when (entry) {
                is Pending.InFlight -> entry.attempt.address
                is Pending.Preparing, is Pending.Queued, is Pending.Saving -> null
            }
        }.groupingBy { address -> address }.eachCount()
        val address = selectNode(nodes.statuses(), outstanding) ?: return minOf(settings.pollInterval, remaining(run.context))
        val node = nodes.find(address) ?: return minOf(settings.pollInterval, remaining(run.context))
        persistence.markInProgress(run.context.submission.id)
        val attempt = Attempt(address = address, node = node)
        val sending = Pending.InFlight(context = run.context, prepared = run.prepared, attempt = attempt, attempts = run.attempts + 1)
        pending[run.context.submission.id] = sending
        val remaining = remaining(run.context)
        if (remaining <= Duration.ZERO) {
            finish(sending, CheckedResult.Timeout)
            return null
        }
        try {
            val cancellation = node.client.grade(
                submission = run.prepared.message,
                timeout = minOf(settings.rpcTimeout, remaining),
                onResult = { result ->
                    dispatch {
                        if (isCurrent(sending)) {
                            if (nodes.find(address) !== node) {
                                retry(sending, CheckedResult.Failure("Checking node was removed"))
                            } else {
                                val checked = checkResult(
                                    result,
                                    run.context.submission.id,
                                    run.prepared.testIds,
                                    parser,
                                )
                                finish(sending, checked)
                            }
                        }
                    }
                },
                onError = { error -> dispatch { handleRpcError(sending, error) } },
            )
            attempt.cancel.set(cancellation)
        } catch (error: Exception) {
            handleRpcError(sending, error)
        }
        return null
    }

    private fun handleRpcError(run: Pending.InFlight, error: Throwable) {
        if (!isCurrent(run)) {
            return
        }
        when (Status.fromThrowable(error).code) {
            Status.Code.UNAVAILABLE -> {
                val attempt = run.attempt
                val description = "Checking node unavailable: ${error.message}"
                nodes.markUnavailable(address = attempt.address, node = attempt.node, reason = description)
                retry(run, CheckedResult.Failure(description))
            }
            Status.Code.DEADLINE_EXCEEDED -> retry(run, CheckedResult.Timeout)
            else -> if (nodes.find(run.attempt.address) !== run.attempt.node) {
                retry(run, CheckedResult.Failure("Checking node was removed"))
            } else {
                finish(run, CheckedResult.Failure("Grading RPC failed: ${error.message}"))
            }
        }
    }

    private fun retry(run: Pending.InFlight, exhaustedResult: CheckedResult) {
        val waiting = Pending.Queued(context = run.context, prepared = run.prepared, attempts = run.attempts)
        pending[run.context.submission.id] = waiting
        run.attempt.cancel.get()?.close()
        if (remaining(run.context) <= Duration.ZERO) {
            finish(waiting, CheckedResult.Timeout)
        } else if (run.attempts >= settings.maxAttempts) {
            finish(waiting, exhaustedResult)
        } else {
            submissions.add(waiting)
            senderWakeup.release()
        }
    }

    private fun finish(run: Pending.Active, result: CheckedResult) {
        val finishing = Pending.Saving(context = run.context, result = result)
        pending[run.context.submission.id] = finishing
        run.context.deadline.get()?.cancel(false)
        when (run) {
            is Pending.InFlight -> run.attempt.cancel.get()?.close()
            is Pending.Preparing, is Pending.Queued -> Unit
        }
        senderWakeup.release()
        save(finishing)
    }

    private fun save(run: Pending.Saving) {
        if (!isCurrent(run)) {
            return
        }
        try {
            persistence.saveResult(run.context.submission, run.result)
        } catch (error: RuntimeException) {
            log.error("Failed to save submission ${run.context.submission.id.value}", error)
            run.saveRetry.set(executor.schedule({ save(run) }, settings.pollInterval.toNanos(), TimeUnit.NANOSECONDS))
            return
        }
        run.saveRetry.get()?.cancel(false)
        pending.remove(run.context.submission.id, run)
        subscribers.forEach { subscriber ->
            try {
                subscriber(run.context.submission.id)
            } catch (error: Exception) {
                log.error("Grading subscriber failed for ${run.context.submission.id.value}", error)
            }
        }
    }

    private fun isCurrent(run: Pending): Boolean = !isClosed.get() && pending[run.context.submission.id] === run

    private fun remaining(context: RunContext): Duration =
        settings.totalTimeout.minus(Duration.between(context.acceptedAt, clock.instant()))

    private fun dispatch(action: () -> Unit) {
        if (!isClosed.get()) {
            try {
                executor.execute {
                    try {
                        action()
                    } catch (error: RuntimeException) {
                        log.error("Failed to handle grading work", error)
                    }
                }
            } catch (error: RejectedExecutionException) {
                if (!isClosed.get()) {
                    log.error("Grading executor rejected work", error)
                }
            }
        }
    }

    /** Keeps one run's identity across phase changes so its deadline cannot expire a later regrade. */
    private class RunContext(
        val submission: Submission,
        val acceptedAt: Instant,
        val deadline: AtomicReference<ScheduledFuture<*>?> = AtomicReference(),
    )

    private sealed interface Pending {
        val context: RunContext

        sealed interface Active : Pending

        class Preparing(override val context: RunContext) : Active

        class Queued(override val context: RunContext, val prepared: PreparedSubmission, val attempts: Int) : Active

        class InFlight(
            override val context: RunContext,
            val prepared: PreparedSubmission,
            val attempt: Attempt,
            val attempts: Int,
        ) : Active

        class Saving(override val context: RunContext, val result: CheckedResult) : Pending {
            val saveRetry = AtomicReference<ScheduledFuture<*>?>()
        }
    }

    private class Attempt(val address: GradingNodeAddress, val node: RegisteredNode) {
        val cancel = AtomicReference<AutoCloseable?>()
    }
}
