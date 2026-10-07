package tech.testsys.operation

import tech.testsys.domain.builder.api.taskValidationTechnicalFailure
import tech.testsys.domain.contract.Grader
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskValidationRequestId
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor

/**
 * Drives saved task validation requests through [TaskValidationOperations] on [executor], which must run tasks one
 * at a time; this gives exclusive execution within a single application instance. Processing is scheduled when testing
 * is requested and when an author submission is graded; a technical exception records a terminal technical failure.
 *
 * @since %CURRENT_VERSION%
 */
class TaskValidationDispatcher(
    private val requests: TaskValidationRequestRepository,
    private val operations: TaskValidationOperations,
    private val grader: Grader,
    private val executor: Executor,
) {
    private val scheduled: MutableSet<TaskValidationRequestId> = ConcurrentHashMap.newKeySet()

    /**
     * Subscribes to grading results and schedules every active request after resending its unfinished submissions.
     * Called once at application startup, before requests are scheduled.
     *
     * @since %CURRENT_VERSION%
     */
    fun start() {
        grader.subscribeOnGraded { submissionId -> executor.execute { onGraded(submissionId) } }
        executor.execute {
            requests.findActive().forEach { request ->
                guarded(request.id) { operations.resendUnfinishedSubmissions(request.id) }
                schedule(request.id)
            }
        }
    }

    /**
     * Schedules processing of [requestId] without waiting for it; a request already waiting to be processed
     * is not scheduled twice.
     *
     * @since %CURRENT_VERSION%
     */
    fun schedule(requestId: TaskValidationRequestId) {
        if (scheduled.add(requestId)) executor.execute { process(requestId) }
    }

    private fun onGraded(submissionId: SubmissionId) {
        val request = requests.findBySubmissionId(submissionId) ?: return
        if (request.data.isActive) schedule(request.id)
    }

    private fun process(requestId: TaskValidationRequestId) {
        scheduled.remove(requestId)
        guarded(requestId) { operations.proceed(requestId) }
    }

    // Any exception of a processing step is a technical failure of the request, whatever its type.
    @Suppress("TooGenericExceptionCaught")
    private fun guarded(requestId: TaskValidationRequestId, action: () -> Unit) {
        try {
            action()
        } catch (exception: Exception) {
            requests.recordTechnicalFailure(
                requestId = requestId,
                failure = taskValidationTechnicalFailure {
                    description = "${exception::class.qualifiedName}: ${exception.message}"
                    occurredAt = Instant.now()
                },
            )
        }
    }
}
