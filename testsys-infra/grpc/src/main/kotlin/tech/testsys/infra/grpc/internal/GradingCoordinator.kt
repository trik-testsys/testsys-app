@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import io.grpc.Status
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import org.apache.commons.logging.LogFactory
import org.springframework.core.env.Environment
import tech.testsys.domain.contract.GradingAdmission
import tech.testsys.domain.contract.GradingNodeAddress
import tech.testsys.domain.contract.GradingNodeStatus
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.infra.grpc.api.GradingQueueSnapshot
import tech.testsys.infra.monitoring.api.TechnicalFailures
import tech.testsys.infra.monitoring.api.TestSysMetrics
import tech.testsys.infra.settings.api.RuntimeSettings
import tech.testsys.infra.settings.api.Settings
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
    private val settings: Settings<GradingProperties>,
    private val executor: ScheduledExecutorService,
    private val senderExecutor: ExecutorService,
    private val pollingExecutor: ScheduledExecutorService,
    private val clock: Clock,
    private val parser: LogParser,
    private val environment: Environment,
    private val runtime: RuntimeSettings? = null,
    private val failures: TechnicalFailures = TechnicalFailures.NONE,
    private val metrics: MeterRegistry = CompositeMeterRegistry(),
) : AutoCloseable {
    private val pending = ConcurrentHashMap<SubmissionId, Pending>()
    private val submissions = LinkedBlockingQueue<Pending.Queued>()
    private val senderWakeup = Semaphore(0)
    private val subscribers = CopyOnWriteArrayList<(SubmissionId) -> Unit>()
    private val isClosed = AtomicBoolean(false)
    private var pollingRegistration: AutoCloseable? = null
    private val log = LogFactory.getLog(javaClass)

    fun start() {
        senderExecutor.execute(::sendLoop)
        pollingRegistration = if (runtime == null) {
            schedulePolling(settings.current().pollInterval) { true }
        } else {
            runtime.register { candidate ->
                schedulePolling(candidate.get(GradingProperties::class.java, environment).pollInterval) {
                    runtime.snapshot() === candidate
                }
            }
        }
    }

    fun submit(submission: Submission): GradingAdmission = synchronized(isClosed) {
        check(!isClosed.get()) { "Grader is closed" }
        val context = RunContext(submission = submission, acceptedAt = clock.instant(), settings = settings.current())
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

    fun observe(address: GradingNodeAddress) = synchronized(isClosed) {
        check(!isClosed.get()) { "Grader is closed" }
        nodes.observe(address)
    }

    fun forget(address: GradingNodeAddress) = synchronized(isClosed) {
        removeNode(address)
        nodes.forget(address)
    }

    fun addNode(address: GradingNodeAddress) = synchronized(isClosed) {
        check(!isClosed.get()) { "Grader is closed" }
        nodes.observe(address)
        nodes.register(address)
        senderWakeup.release()
    }

    fun removeNode(address: GradingNodeAddress) = synchronized(isClosed) {
        val removed = nodes.unregister(address) ?: return@synchronized
        dispatch {
            pending.values.forEach { run ->
                when (run) {
                    is Pending.InFlight -> if (run.attempt.registration === removed) {
                        retry(run, CheckedResult.Failure("Checking node was removed"))
                    }
                    is Pending.Preparing, is Pending.Queued, is Pending.Saving -> Unit
                }
            }
            senderWakeup.release()
        }
    }

    fun statuses(): Map<GradingNodeAddress, GradingNodeStatus> = nodes.participantStatuses()

    fun observedStatuses(): Map<GradingNodeAddress, GradingNodeStatus> = nodes.observedStatuses()

    fun queue(): GradingQueueSnapshot {
        var preparing = 0
        var waiting = 0
        var grading = 0
        var saving = 0
        pending.values.forEach { run ->
            when (run) {
                is Pending.Preparing -> preparing++
                is Pending.Queued -> waiting++
                is Pending.InFlight -> grading++
                is Pending.Saving -> saving++
            }
        }
        return GradingQueueSnapshot(preparing = preparing, waiting = waiting, grading = grading, saving = saving)
    }

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
            pollingRegistration?.close()
            senderExecutor.shutdownNow()
            executor.shutdownNow()
            pollingExecutor.shutdownNow()
            nodes.close()
        }
    }

    private fun schedulePolling(interval: Duration, isCurrent: () -> Boolean): AutoCloseable {
        val scheduled = pollingExecutor.scheduleWithFixedDelay(
            {
                if (isCurrent()) {
                    nodes.poll { settings.current().statusTimeout }
                    senderWakeup.release()
                }
            },
            0,
            interval.toNanos(),
            TimeUnit.NANOSECONDS,
        )
        return AutoCloseable { scheduled.cancel(false) }
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
                    failures.record(source = SOURCE, stage = "send", failure = error)
                    completed.complete(minOf(settings.current().pollInterval, remaining(run.context)).coerceAtLeast(Duration.ZERO))
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
            val prepared = persistence.prepare(run.context.submission, run.context.settings.shouldRecordVideo)
            val waiting = Pending.Queued(context = run.context, prepared = prepared, attempts = 0)
            pending[run.context.submission.id] = waiting
            submissions.add(waiting)
        } catch (error: RuntimeException) {
            failures.record(source = SOURCE, stage = "prepare", failure = error)
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
        val address = selectNode(nodes.participantStatuses(), outstanding)
            ?: return minOf(settings.current().pollInterval, remaining(run.context))
        val registration = nodes.registration(address) ?: return minOf(settings.current().pollInterval, remaining(run.context))
        persistence.markInProgress(run.context.submission.id)
        val attempt = Attempt(address = address, registration = registration)
        val sending = Pending.InFlight(context = run.context, prepared = run.prepared, attempt = attempt, attempts = run.attempts + 1)
        pending[run.context.submission.id] = sending
        val remaining = remaining(run.context)
        if (remaining <= Duration.ZERO) {
            finish(sending, CheckedResult.Timeout)
            return null
        }
        try {
            val cancellation = registration.client.grade(
                submission = run.prepared.message,
                timeout = minOf(run.context.settings.rpcTimeout, remaining),
                onResult = { result ->
                    dispatch {
                        if (isCurrent(sending)) {
                            if (nodes.registration(address) !== registration) {
                                retry(sending, CheckedResult.Failure("Checking node was removed"))
                            } else {
                                val checked = checkResult(
                                    result,
                                    run.context.submission.id,
                                    run.prepared.testIds,
                                    parser,
                                )
                                if (checked is CheckedResult.Failure) failures.record(source = SOURCE, stage = "invalid_result")
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
                log.warn("Checking node ${attempt.address.target} is unavailable for submission ${run.context.submission.id.value}", error)
                failures.record(source = SOURCE, stage = "node_unavailable", failure = error)
                nodes.markUnavailable(address = attempt.address, registration = attempt.registration, reason = unreachableReason(error))
                retry(run, CheckedResult.Failure("Checking node unavailable: ${error.message}"))
            }
            Status.Code.DEADLINE_EXCEEDED -> {
                // An exhausted total deadline is a timeout of the run, not a failure of the node.
                if (remaining(run.context) > Duration.ZERO) failures.record(source = SOURCE, stage = "node_deadline", failure = error)
                retry(run, CheckedResult.Timeout)
            }
            else -> if (nodes.registration(run.attempt.address) !== run.attempt.registration) {
                retry(run, CheckedResult.Failure("Checking node was removed"))
            } else {
                failures.record(source = SOURCE, stage = "node_error", failure = error)
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
        } else if (run.attempts >= run.context.settings.maxAttempts) {
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
            failures.record(source = SOURCE, stage = "save", failure = error)
            run.saveRetry.set(executor.schedule({ save(run) }, settings.current().pollInterval.toNanos(), TimeUnit.NANOSECONDS))
            return
        }
        run.saveRetry.get()?.cancel(false)
        pending.remove(run.context.submission.id, run)
        metrics.counter(TestSysMetrics.GRADING_CHECKS, TestSysMetrics.RESULT, outcome(run.result)).increment()
        subscribers.forEach { subscriber ->
            try {
                subscriber(run.context.submission.id)
            } catch (error: Exception) {
                log.error("Grading subscriber failed for ${run.context.submission.id.value}", error)
                failures.record(source = SOURCE, stage = "subscriber", failure = error)
            }
        }
    }

    private fun outcome(result: CheckedResult): String = when (result) {
        is CheckedResult.Success -> "verdict"
        is CheckedResult.Failure -> "failure"
        CheckedResult.Timeout -> "timeout"
    }

    private fun isCurrent(run: Pending): Boolean = !isClosed.get() && pending[run.context.submission.id] === run

    private fun remaining(context: RunContext): Duration =
        context.settings.totalTimeout.minus(Duration.between(context.acceptedAt, clock.instant()))

    private fun dispatch(action: () -> Unit) {
        if (!isClosed.get()) {
            try {
                executor.execute {
                    try {
                        action()
                    } catch (error: RuntimeException) {
                        log.error("Failed to handle grading work", error)
                        failures.record(source = SOURCE, stage = "work", failure = error)
                    }
                }
            } catch (error: RejectedExecutionException) {
                if (!isClosed.get()) {
                    log.error("Grading executor rejected work", error)
                    failures.record(source = SOURCE, stage = "rejected", failure = error)
                }
            }
        }
    }

    /** Keeps one run's identity across phase changes so its deadline cannot expire a later regrade. */
    private class RunContext(
        val submission: Submission,
        val acceptedAt: Instant,
        val settings: GradingProperties,
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

    private class Attempt(val address: GradingNodeAddress, val registration: GradingRegistration) {
        val cancel = AtomicReference<AutoCloseable?>()
    }

    private companion object {
        const val SOURCE = "grading"
    }
}
