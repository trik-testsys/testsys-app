package tech.testsys.domain.contract

import tech.testsys.domain.model.task.Test
import tech.testsys.domain.model.task.TestDiagnosticResult

/**
 * Synchronous analysis port for one polygon, without persistence or task execution scheduling.
 *
 * @since %CURRENT_VERSION%
 */
interface PolygonDiagnostics {
    /**
     * Analyses the immutable file of [test]; repeated calls are independent and use the current adapter configuration.
     * XML errors are returned as diagnostic messages; technical exceptions propagate to the external caller.
     *
     * @param test the concrete polygon version to analyse.
     * @return the messages associated with the identifier of [test], including an empty list.
     * @since %CURRENT_VERSION%
     */
    fun diagnose(test: Test): TestDiagnosticResult
}
