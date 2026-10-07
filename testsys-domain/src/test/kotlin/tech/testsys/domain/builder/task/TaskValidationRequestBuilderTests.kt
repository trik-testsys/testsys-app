package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.*
import tech.testsys.domain.model.task.*
import java.time.Instant

class TaskValidationRequestBuilderTests :
    DomainEntityBuilderTests<TaskValidationRequest, TaskValidationRequestData, TaskValidationRequestDataBuilder>(
        TaskValidationRequestBuilder(),
        TaskValidationRequestDataBuilder(),
    ) {
    override fun buildDataWithAllFields(): List<TaskValidationRequestData> = listOf(
        taskValidationRequestData {
            task(1)
            requestedBy(2)
            snapshot = taskValidationSnapshot {}
            execution.pendingDiagnostics()
        },
        taskValidationRequestData {
            task(1)
            requestedBy(2)
            snapshot = taskValidationSnapshot { tests(listOf(3)) }
            execution.createdSubmissionsFailure {
                diagnostics = mutableListOf(testDiagnosticResult { testId(3) })
                submissions(listOf(4))
                failure = taskValidationTechnicalFailure {
                    description = "Storage unavailable"
                    occurredAt = Instant.EPOCH
                }
            }
        },
    )
}
