package tech.testsys.infra.monitoring.api

/**
 * Names and label keys of the metrics TestSys records itself; allowed label values are listed in the module README.
 *
 * @property TECHNICAL_FAILURES the counter of technical failures by [SOURCE], [STAGE] and [EXCEPTION].
 * @property OPERATION_ERRORS the counter of expected operation refusals by [OPERATION], [KIND] and [ERROR].
 * @property TRANSACTION_RETRIES the counter of transaction repeats after a conflict, without labels.
 * @property GRADING_CHECKS the counter of finished grading runs by [RESULT].
 * @property GRADING_QUEUE the gauge of unfinished grading runs by [STATE].
 * @property GRADING_NODES the gauge of registry checking nodes by [PARTICIPATION] and [STATUS].
 * @property GRADING_NODES_QUEUED the gauge of the total queue of available participating checking nodes.
 * @property GRADING_NODES_CAPACITY the gauge of the total capacity of available participating checking nodes.
 * @property SOURCE the label of the component where a technical failure happened.
 * @property STAGE the label of the processing step where a technical failure happened.
 * @property EXCEPTION the label with the simple class name of a failure, or [NO_EXCEPTION].
 * @property NO_EXCEPTION the value of [EXCEPTION] for a handled failure without an exception.
 * @property OPERATION the label of the refused operation.
 * @property KIND the label of the general kind of an operation refusal.
 * @property ERROR the label with the simple class name of an operation error.
 * @property RESULT the label of the outcome of a finished grading run.
 * @property STATE the label of the state of an unfinished grading run.
 * @property PARTICIPATION the label of the participation of a checking node in grading.
 * @property STATUS the label of the last known availability of a checking node.
 * @since %CURRENT_VERSION%
 */
object TestSysMetrics {
    const val TECHNICAL_FAILURES: String = "testsys.technical.failures"

    const val OPERATION_ERRORS: String = "testsys.operation.errors"

    const val TRANSACTION_RETRIES: String = "testsys.transaction.retries"

    const val GRADING_CHECKS: String = "testsys.grading.checks"

    const val GRADING_QUEUE: String = "testsys.grading.queue"

    const val GRADING_NODES: String = "testsys.grading.nodes"

    const val GRADING_NODES_QUEUED: String = "testsys.grading.nodes.queued"

    const val GRADING_NODES_CAPACITY: String = "testsys.grading.nodes.capacity"

    const val SOURCE: String = "source"

    const val STAGE: String = "stage"

    const val EXCEPTION: String = "exception"

    const val NO_EXCEPTION: String = "none"

    const val OPERATION: String = "operation"

    const val KIND: String = "kind"

    const val ERROR: String = "error"

    const val RESULT: String = "result"

    const val STATE: String = "state"

    const val PARTICIPATION: String = "participation"

    const val STATUS: String = "status"
}
