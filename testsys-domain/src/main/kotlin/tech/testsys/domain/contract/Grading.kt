package tech.testsys.domain.contract

import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId

/**
 * Port to the external grading system. Grading is asynchronous: [sendToGrade] returns immediately and completion
 * is announced through the callback registered with [subscribeOnGraded].
 *
 * @since %CURRENT_VERSION%
 */
interface Grader {
    /**
     * Hands a submission over without waiting for preparation or grading; an unfinished request with the same
     * submission id is reused without replacing the accepted submission. After completion a new call starts
     * a new run, producing a new verdict on success.
     *
     * @param submission the submission to grade.
     * @return whether a new run was accepted or is already pending.
     * @since %CURRENT_VERSION%
     */
    fun sendToGrade(submission: Submission): GradingAdmission

    /**
     * Registers a callback invoked whenever grading of a submission has finished.
     *
     * @param onGraded the callback receiving the id of the graded submission.
     * @since %CURRENT_VERSION%
     */
    fun subscribeOnGraded(onGraded: (SubmissionId) -> Unit)

    /**
     * Adds a checking node; adding the same address again has no effect.
     *
     * @param address the gRPC target of the node.
     * @since %CURRENT_VERSION%
     */
    fun addNode(address: GradingNodeAddress)

    /**
     * Removes a node, closes its channel and retries unfinished work within configured limits.
     * Removing an unknown address has no effect.
     *
     * @param address the gRPC target to remove.
     * @since %CURRENT_VERSION%
     */
    fun removeNode(address: GradingNodeAddress)

    /**
     * Reads cached node statuses without network calls; newly added nodes initially have an unknown status.
     *
     * @return a snapshot of the registered nodes and their last known statuses.
     * @since %CURRENT_VERSION%
     */
    fun getNodeStatuses(): Map<GradingNodeAddress, GradingNodeStatus>
}

/**
 * Admission outcome of an asynchronous grading request.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface GradingAdmission {
    /**
     * A new run was accepted, including preparation, queueing and saving its eventual outcome.
     *
     * @since %CURRENT_VERSION%
     */
    data object Accepted : GradingAdmission

    /**
     * This submission id already has an unfinished run; the accepted submission was preserved.
     *
     * @since %CURRENT_VERSION%
     */
    data object AlreadyPending : GradingAdmission
}

/**
 * Address of a checking node.
 *
 * @property target the gRPC target understood by the adapter.
 * @since %CURRENT_VERSION%
 */
@JvmInline
value class GradingNodeAddress(val target: String)

/**
 * Cached availability of a checking node.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface GradingNodeStatus {
    /**
     * No status response has arrived yet.
     *
     * @since %CURRENT_VERSION%
     */
    data object Unknown : GradingNodeStatus

    /**
     * The node responded to its latest status poll.
     *
     * @property queued the number of submissions reported by the node.
     * @property capacity the number of concurrent checks the node supports.
     * @since %CURRENT_VERSION%
     */
    data class Available(val queued: Int, val capacity: Int) : GradingNodeStatus

    /**
     * The latest status poll failed or the node was unavailable when grading was sent.
     *
     * @property reason the technical failure description.
     * @since %CURRENT_VERSION%
     */
    data class Unreachable(val reason: String) : GradingNodeStatus
}
