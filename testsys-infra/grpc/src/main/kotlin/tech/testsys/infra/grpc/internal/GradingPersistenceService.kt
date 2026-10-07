@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TestId
import trik.testsys.grading.GradingNodeOuterClass as Proto

@InternalGrpcApi
internal data class PreparedSubmission(val submission: Submission, val message: Proto.Submission, val testIds: List<TestId>)

@InternalGrpcApi
@Component
internal class GradingPersistenceService(
    private val submissions: SubmissionRepository,
    private val solutions: SolutionRepository,
    private val tasks: TaskRepository,
    private val contests: ContestRepository,
    private val tests: TestRepository,
    private val logs: LogsRepository,
    private val recordings: RecordingRepository,
    private val verdicts: VerdictRepository,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager)

    fun prepare(submission: Submission, shouldRecordVideo: Boolean): PreparedSubmission {
        val solution = solutions.load(submission.data.solution)
        val task = tasks.load(submission.data.task)
        val content = task.data.content
        val kind = submission.data.kind
        val trikStudioVersion = when (kind) {
            is SubmissionKind.DeveloperSolutionTest -> kind.trikStudioVersion
            is SubmissionKind.Grading -> contests.load(kind.contest).data.trikStudioVersion
        }
        val testReferences = when (kind) {
            is SubmissionKind.DeveloperSolutionTest -> when (content) {
                is TaskContent.New -> content.wip.tests
                is TaskContent.Uncommitted -> content.wip.tests
                is TaskContent.Committed -> content.lastCommitted.tests
            }
            is SubmissionKind.Grading -> when (content) {
                is TaskContent.New -> error("Task ${task.id.value} has no committed revision")
                is TaskContent.Uncommitted -> content.lastCommitted.tests
                is TaskContent.Committed -> content.lastCommitted.tests
            }
        }
        require(testReferences.ids.isNotEmpty() && testReferences.ids.distinct().size == testReferences.ids.size) {
            "Submission ${submission.id.value} must have nonempty distinct polygons"
        }
        val loadedTests = tests.load(testReferences)
        require(loadedTests.map { test -> test.id }.toSet() == testReferences.ids.toSet()) {
            "Missing polygons for submission ${submission.id.value}"
        }
        markQueued(submission.id)
        return PreparedSubmission(
            submission = submission,
            message = encodeSubmission(submission.id, solution, loadedTests, trikStudioVersion, shouldRecordVideo),
            testIds = testReferences.ids.toList(),
        )
    }

    fun markInProgress(id: SubmissionId) {
        transaction.executeWithoutResult {
            val current = requireNotNull(submissions.findById(id)) { "Missing submission ${id.value}" }
            submissions.update(current.withData { status.inProgress() })
        }
    }

    fun saveResult(submission: Submission, result: CheckedResult) {
        transaction.executeWithoutResult {
            val current = requireNotNull(submissions.findById(submission.id)) {
                "Missing submission ${submission.id.value}"
            }
            val updated = when (result) {
                is CheckedResult.Success -> {
                    val verdict = verdicts.save(
                        verdictData {
                            task = submission.data.task.id
                            this.submission = submission.id
                            result.tests.forEach { checked ->
                                val savedLogs = logs.save(
                                    logsData {
                                        file(uploadedFilename = checked.logs.name, content = checked.logs.content.toByteArray())
                                    },
                                )
                                val savedRecording = checked.recording?.let { recording ->
                                    recordings.save(
                                        recordingData {
                                            file(uploadedFilename = recording.name, content = recording.content.toByteArray())
                                        },
                                    )
                                }
                                testVerdict {
                                    score = checked.score.value
                                    test = checked.testId
                                    this.logs = savedLogs.id
                                    recording = savedRecording?.id
                                }
                            }
                        },
                    )
                    current.withData { status.graded { status.success { this.verdict = verdict.id } } }
                }
                is CheckedResult.Failure -> current.withData { status.graded { status.error { description = result.description } } }
                CheckedResult.Timeout -> current.withData { status.graded { status.timeout() } }
            }
            submissions.update(updated)
        }
    }

    private fun markQueued(id: SubmissionId) {
        transaction.executeWithoutResult {
            val current = requireNotNull(submissions.findById(id)) { "Missing submission ${id.value}" }
            submissions.update(current.withData { status.queued() })
        }
    }
}
