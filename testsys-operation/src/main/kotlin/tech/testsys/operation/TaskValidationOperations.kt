package tech.testsys.operation

import tech.testsys.domain.contract.PolygonDiagnostics
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TaskValidationRequestId

/**
 * Application-invoked diagnostic processing of saved task validation requests.
 * Scheduling, exclusive execution and technical exception handling belong to the external caller.
 *
 * @since %CURRENT_VERSION%
 */
class TaskValidationOperations(
    private val requests: TaskValidationRequestRepository,
    private val tests: TestRepository,
    private val diagnostics: PolygonDiagnostics,
) {
    /**
     * Processes the caller-selected [requestId], skipping saved polygon results and completing the stage.
     * Error messages do not interrupt other polygons; technical exceptions propagate with saved progress retained.
     *
     * @return the completed request, or `null` when the request is missing or no longer eligible.
     * @since %CURRENT_VERSION%
     */
    fun processDiagnostics(requestId: TaskValidationRequestId): TaskValidationRequest? {
        val request = requests.startDiagnostics(requestId) ?: return null
        val savedIds = requests.findDiagnosticProgress(requestId).map { it.testId }.toSet()
        request.data.snapshot.tests.ids.filterNot { it in savedIds }.forEach { testId ->
            val test = requireNotNull(tests.findById(testId)) {
                "Polygon id=${testId.value} of request id=${requestId.value} does not exist"
            }
            val result = diagnostics.diagnose(test)
            check(result.testId == testId) {
                "Diagnostics returned polygon id=${result.testId.value} for requested polygon id=${testId.value}"
            }
            requests.saveDiagnosticProgress(requestId = requestId, result = result)
        }
        return requests.completeDiagnostics(requestId)
    }
}
