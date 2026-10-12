package tech.testsys.infra.grpc.api

/**
 * Numbers of unfinished grading runs by coordinator state; each run is counted once in its current state.
 *
 * @property preparing runs loading submission data.
 * @property waiting prepared runs waiting to be sent to a checking node.
 * @property grading runs with an active grading request on a checking node.
 * @property saving runs with an accepted result waiting to be saved.
 * @property total all unfinished runs, the sum of the four states.
 * @since %CURRENT_VERSION%
 */
data class GradingQueueSnapshot(val preparing: Int, val waiting: Int, val grading: Int, val saving: Int) {
    val total: Int
        get() = preparing + waiting + grading + saving
}

/**
 * Reads the in-memory grading queue of this process without blocking grading.
 *
 * @since %CURRENT_VERSION%
 */
fun interface GradingQueueMonitor {
    /**
     * Returns the current numbers of unfinished runs.
     *
     * @since %CURRENT_VERSION%
     */
    fun snapshot(): GradingQueueSnapshot
}
