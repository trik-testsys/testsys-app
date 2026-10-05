package tech.testsys.domain.builder.task

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.builder.api.testVerdict
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.model.task.LogsId
import tech.testsys.domain.model.task.RecordingId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionData
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictData

class SubmissionBuilderTests : DomainEntityBuilderTests<Submission, SubmissionData, SubmissionDataBuilder>(
    SubmissionBuilder(),
    SubmissionDataBuilder(),
) {
    @Test
    fun `should throw IllegalArgumentException if TRIK Studio version is missing`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            submissionData {
                author(42)
                solution(1)
                task(1)
                status.queued()
                kind.developerSolutionTest()
            }
        }
    }

    override fun buildDataWithAllFields() = listOf(
        submissionData {
            author(42)
            solution(1)
            task(1)
            trikStudioVersion("3.0.0")
            status.queued()
            kind.developerSolutionTest()
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            trikStudioVersion("3.0.0")
            status.inProgress()
            kind.grading { contest(10) }
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            trikStudioVersion("3.0.0")
            status.graded { status.success { verdict(100) } }
            kind.grading { contest(10) }
            judgmentOrders(listOf(1L, 2L))
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            trikStudioVersion("3.0.0")
            status.graded { status.error { description = "description" } }
            kind.developerSolutionTest()
        },
        submissionData {
            author(42)
            solution(1)
            task(1)
            trikStudioVersion("3.0.0")
            status.graded { status.timeout() }
            kind.grading { contest(10) }
        },
    )
}

class VerdictBuilderTests : DomainEntityBuilderTests<Verdict, VerdictData, VerdictDataBuilder>(
    VerdictBuilder(),
    VerdictDataBuilder(),
) {
    @Test
    fun `should throw IllegalArgumentException if test verdicts are empty`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            verdictData {
                task(1)
                submission(1)
            }
        }
    }

    @Test
    fun `should throw IllegalArgumentException if copy removes all test verdicts`() {
        val data = verdictData {
            task(1)
            submission(1)
            testVerdict {
                score = 0
                test(1)
                logs(1)
            }
        }

        Assertions.assertThrows(IllegalArgumentException::class.java) {
            @Suppress("UnusedDataClassCopyResult")
            data.copy(testVerdicts = emptyList())
        }
    }

    override fun buildDataWithAllFields() = listOf(
        verdictData {
            task(1)
            submission(1)
            testVerdict {
                score = 0
                test(1)
                logs(1)
            }
        },
        verdictData {
            task(1)
            submission(1)
            testVerdict {
                score = 100
                test(1)
                logs(1)
                recording(1)
            }
            testVerdict {
                score = 0
                test(2)
                logs(2)
            }
        },
    )
}

class TestVerdictBuilderTests {

    @Test
    fun `should build test verdict with all fields`() {
        val built = testVerdict {
            score = 100
            test(1)
            logs(2)
            recording(3)
        }

        Assertions.assertEquals(Score(100), built.score)
        Assertions.assertEquals(TestId(1), built.test.id)
        Assertions.assertEquals(LogsId(2), built.logs.id)
        Assertions.assertEquals(RecordingId(3), built.recording?.id)
    }

    @Test
    fun `should build test verdict without recording`() {
        val built = testVerdict {
            score = 50
            test(1)
            logs(2)
        }

        Assertions.assertEquals(LogsId(2), built.logs.id)
        Assertions.assertNull(built.recording)
    }

    @Test
    fun `should throw IllegalArgumentException if score is missing`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            testVerdict {
                test(1)
                logs(2)
            }
        }
    }

    @Test
    fun `should throw IllegalArgumentException if test is missing`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            testVerdict {
                score = 100
                logs(2)
            }
        }
    }

    @Test
    fun `should throw IllegalArgumentException if logs is missing`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            testVerdict {
                score = 100
                test(1)
            }
        }
    }
}
