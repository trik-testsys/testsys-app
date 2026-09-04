package tech.testsys.domain.builder.task

import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData

class SubmissionBuilderTests : DomainEntityBuilderTests<Submission, SubmissionData, SubmissionDataBuilder>(
    SubmissionBuilder(),
    SubmissionDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(
        submissionData {
            author(42)
            solution(1)
            task(1)
            status.queued()
            kind.developerSolutionTest()
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            status.inProgress()
            kind.grading { contest(10) }
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            status.graded { status.success { verdict(100) }  }
            kind.grading { contest(10) }
            judgmentOrders(listOf(1L, 2L))
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            status.graded { status.error { description = "description" } }
            kind.developerSolutionTest()
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            status.graded { status.timeout() }
            kind.grading { contest(10) }
        },
    )
}

class VerdictBuilderTests : DomainEntityBuilderTests<Verdict, VerdictData, VerdictDataBuilder>(
    VerdictBuilder(),
    VerdictDataBuilder()
) {
    override fun buildDataWithAllFields() = listOf(verdictData {
        score = 100
        task(1)
        submission(1)
        logs(1)
        recording(1)
    })
}