@file:OptIn(InternalGrpcApi::class)

package tech.testsys.infra.grpc.internal

import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.builder.api.logsData
import tech.testsys.domain.builder.api.recordingData
import tech.testsys.domain.builder.api.verdictData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.LogsRepository
import tech.testsys.domain.contract.persistence.repository.RecordingRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.TaskValidationRequestRepository
import tech.testsys.domain.contract.persistence.repository.TestRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.SubmissionKind
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskValidationExecution
import tech.testsys.domain.model.task.TaskValidationRequest
import tech.testsys.domain.model.task.TaskValidationSnapshot
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
    private val validationRequests: TaskValidationRequestRepository,
    private val fileContentReader: FileContentReader,
    private val transactions: TransactionOperations,
) {
    fun prepare(submission: Submission, shouldRecordVideo: Boolean): PreparedSubmission = transactions.execute {
        val kind = submission.data.kind
        val trikStudioVersion = when (kind) {
            is SubmissionKind.DeveloperSolutionTest -> kind.trikStudioVersion
            is SubmissionKind.Grading -> contests.load(kind.contest).data.trikStudioVersion
        }
        val testReferences = when (kind) {
            is SubmissionKind.DeveloperSolutionTest -> {
                val request = validationRequests.findBySubmissionId(submission.id)
                    ?: error("Author submission ${submission.id.value} has no linked validation request")
                validateSnapshotSubmission(request, submission, kind)
                request.data.snapshot.tests
            }
            is SubmissionKind.Grading -> {
                val task = tasks.load(submission.data.task)
                when (val content = task.data.content) {
                    is TaskContent.New -> error("Task ${task.id.value} has no committed revision")
                    is TaskContent.Uncommitted -> content.lastCommitted.tests
                    is TaskContent.Committed -> content.lastCommitted.tests
                }
            }
        }
        val solution = solutions.load(submission.data.solution)
        require(testReferences.ids.isNotEmpty() && testReferences.ids.distinct().size == testReferences.ids.size) {
            "Submission ${submission.id.value} must have nonempty distinct polygons"
        }
        val loadedTests = tests.load(testReferences)
        require(loadedTests.map { test -> test.id }.toSet() == testReferences.ids.toSet()) {
            "Missing polygons for submission ${submission.id.value}"
        }
        markQueued(submission.id)
        PreparedSubmission(
            submission = submission,
            message = encodeSubmission(submission.id, solution, loadedTests, trikStudioVersion, shouldRecordVideo, fileContentReader),
            testIds = testReferences.ids.toList(),
        )
    }

    fun markInProgress(id: SubmissionId) {
        transactions.executeWithoutResult {
            val current = requireNotNull(submissions.findById(id)) { "Missing submission ${id.value}" }
            submissions.update(current.withData { status.inProgress() })
        }
    }

    fun saveResult(submission: Submission, result: CheckedResult) {
        transactions.executeWithoutResult {
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
        val current = requireNotNull(submissions.findById(id)) { "Missing submission ${id.value}" }
        submissions.update(current.withData { status.queued() })
    }

    private fun validateSnapshotSubmission(
        request: TaskValidationRequest,
        submission: Submission,
        kind: SubmissionKind.DeveloperSolutionTest,
    ) {
        val state = checkNotNull(request.data.execution as? TaskValidationExecution.WithSubmissions) {
            "Validation request ${request.id.value} has no submission links"
        }
        val runs = TaskValidationSnapshot.authorRuns(request.data.snapshot)
        check(state.submissions.ids.size == runs.size) {
            "Validation request ${request.id.value} has ${state.submissions.ids.size} submissions for ${runs.size} author runs"
        }
        val position = state.submissions.ids.indexOf(submission.id)
        check(position >= 0) { "Submission ${submission.id.value} is not linked to validation request ${request.id.value}" }
        val run = runs[position]
        check(
            submission.data.task.id == request.data.task.id && submission.data.author.id == request.data.requestedBy.id &&
                submission.data.solution.id == run.input.solution.id && kind.trikStudioVersion == run.trikStudioVersion,
        ) { "Submission ${submission.id.value} does not match its author run in validation request ${request.id.value}" }
    }
}
