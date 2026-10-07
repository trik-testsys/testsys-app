package tech.testsys.operation.user

import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestNotEnteredError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.DownloadParticipantTaskResourceError
import tech.testsys.operation.error.DownloadStudentTaskResourceError
import tech.testsys.operation.error.MissedParticipantRoleError
import tech.testsys.operation.error.MissedStudentRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ResourceNotInCommittedTaskError
import tech.testsys.operation.error.TaskAccessDeniedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.ViewParticipantContestError
import tech.testsys.operation.error.ViewParticipantTaskError
import tech.testsys.operation.error.ViewStudentContestError
import tech.testsys.operation.error.ViewStudentTaskError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.hasRole
import java.time.Instant

/**
 * Operations shared by participants and students for studying available contests.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class StudyOperations(
    private val competitionRepository: CompetitionRepository,
    private val classRepository: ClassRepository,
    private val contestRepository: ContestRepository,
    private val participantContestEntryRepository: ParticipantContestEntryRepository,
    private val studentContestEntryRepository: StudentContestEntryRepository,
    private val taskRepository: TaskRepository,
    private val submissionRepository: SubmissionRepository,
    private val verdictRepository: VerdictRepository,
    private val judgmentOrderRepository: JudgmentOrderRepository,
    private val statementRepository: StatementRepository,
    private val exerciseRepository: ExerciseRepository,
) {

    /**
     * Returns the first entry time and original [contestId] in the competition of [user].
     * Viewing is allowed regardless of dates and does not save an entry; storage exceptions propagate.
     *
     * @return the exact first entry time or `null`, paired with the original contest without resolving its tasks.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewContest")
    fun viewContest(user: SingleRoleUser, contestId: ContestId): OperationResult<Pair<Instant?, Contest>, ViewParticipantContestError> =
        operation<Pair<Instant?, Contest>, ViewParticipantContestError> {
            ensure(user is Participant, MissedParticipantRoleError)
            val competitionId = user.data.competition.id
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            ensure(contestId in competition.data.contests.ids) { ContestAccessDeniedError(contestId) }
            val entry = participantContestEntryRepository.findByContext(
                participantId = user.id,
                competitionId = competitionId,
                contestId = contestId,
            )
            return (entry?.data?.enteredAt to contest).asSuccess()
        }

    /**
     * Returns the first entry time in [classId] and original [contestId] for [user].
     * Viewing is allowed regardless of dates and does not save an entry; storage exceptions propagate.
     *
     * @return the exact first entry time or `null`, paired with the original contest without resolving its tasks.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewContest")
    fun viewContest(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
    ): OperationResult<Pair<Instant?, Contest>, ViewStudentContestError> = operation<Pair<Instant?, Contest>, ViewStudentContestError> {
        ensure(user.hasRole<Student>(), MissedStudentRoleError)
        val studyClass = classRepository.findById(classId)
        ensure(studyClass != null) { ClassNotExistsError(classId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        ensure(user.id in studyClass.data.students.ids) { ClassAccessDeniedError(classId) }
        ensure(contestId in studyClass.data.contests.ids) { ContestAccessDeniedError(contestId) }
        val entry = studentContestEntryRepository.findByContext(
            userId = user.id,
            studyClassId = classId,
            contestId = contestId,
        )
        return (entry?.data?.enteredAt to contest).asSuccess()
    }

    /**
     * Returns [taskId] of [contestId] in the competition of [user] with the grading submissions of [user] for it.
     * Requires a saved first entry; works after the contest end, saves nothing, and storage exceptions propagate.
     *
     * @return the original task, submissions in repository order and the best of them or `null`, with lazy references.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewTask")
    fun viewTask(
        user: SingleRoleUser,
        contestId: ContestId,
        taskId: TaskId,
    ): OperationResult<Triple<Task, List<Submission>, Submission?>, ViewParticipantTaskError> =
        operation<Triple<Task, List<Submission>, Submission?>, ViewParticipantTaskError> {
            ensure(user is Participant, MissedParticipantRoleError)
            val competitionId = user.data.competition.id
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            ensure(contestId in competition.data.contests.ids) { ContestAccessDeniedError(contestId) }
            ensure(taskId in contest.data.tasks.ids) { TaskAccessDeniedError(taskId) }
            val entry = participantContestEntryRepository.findByContext(
                participantId = user.id,
                competitionId = competitionId,
                contestId = contestId,
            )
            ensure(entry != null) { ContestNotEnteredError(contestId) }
            val submissions = submissionRepository.findGradingByContext(authorId = user.id, taskId = taskId, contestId = contestId)
            return Triple(task, submissions, bestSubmission(submissions)).asSuccess()
        }

    /**
     * Returns [taskId] of [contestId] in [classId] with the grading submissions of [user] for it in that contest.
     * Requires a saved first entry in [classId]; works after the contest end, saves nothing, and storage exceptions propagate.
     *
     * @return the original task, submissions in repository order and the best of them or `null`, with lazy references.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewTask")
    fun viewTask(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
        taskId: TaskId,
    ): OperationResult<Triple<Task, List<Submission>, Submission?>, ViewStudentTaskError> =
        operation<Triple<Task, List<Submission>, Submission?>, ViewStudentTaskError> {
            ensure(user.hasRole<Student>(), MissedStudentRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            val task = taskRepository.findById(taskId)
            ensure(task != null) { TaskNotExistsError(taskId) }
            ensure(user.id in studyClass.data.students.ids) { ClassAccessDeniedError(classId) }
            ensure(contestId in studyClass.data.contests.ids) { ContestAccessDeniedError(contestId) }
            ensure(taskId in contest.data.tasks.ids) { TaskAccessDeniedError(taskId) }
            val entry = studentContestEntryRepository.findByContext(
                userId = user.id,
                studyClassId = classId,
                contestId = contestId,
            )
            ensure(entry != null) { ContestNotEnteredError(contestId) }
            val submissions = submissionRepository.findGradingByContext(authorId = user.id, taskId = taskId, contestId = contestId)
            return Triple(task, submissions, bestSubmission(submissions)).asSuccess()
        }

    /**
     * Returns the file of statement or exercise [resourceId] of the last committed revision of [taskId] of [contestId]
     * in the competition of [user]. Requires a saved first entry; works after the contest end and saves nothing.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewTask")
    fun downloadTaskResource(
        user: SingleRoleUser,
        contestId: ContestId,
        taskId: TaskId,
        resourceId: DomainId,
    ): OperationResult<FileData, DownloadParticipantTaskResourceError> = operation<FileData, DownloadParticipantTaskResourceError> {
        ensure(user is Participant, MissedParticipantRoleError)
        val competitionId = user.data.competition.id
        val competition = competitionRepository.findById(competitionId)
        ensure(competition != null) { CompetitionNotExistsError(competitionId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(contestId in competition.data.contests.ids) { ContestAccessDeniedError(contestId) }
        ensure(taskId in contest.data.tasks.ids) { TaskAccessDeniedError(taskId) }
        val entry = participantContestEntryRepository.findByContext(
            participantId = user.id,
            competitionId = competitionId,
            contestId = contestId,
        )
        ensure(entry != null) { ContestNotEnteredError(contestId) }
        val file = committedResourceFile(task, resourceId)
        ensure(file != null) { ResourceNotInCommittedTaskError(taskId, resourceId) }
        return file.asSuccess()
    }

    /**
     * Returns the file of statement or exercise [resourceId] of the last committed revision of [taskId] of [contestId]
     * in [classId] for [user]. Requires a saved first entry in [classId]; works after the contest end and saves nothing.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewTask")
    fun downloadTaskResource(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
        taskId: TaskId,
        resourceId: DomainId,
    ): OperationResult<FileData, DownloadStudentTaskResourceError> = operation<FileData, DownloadStudentTaskResourceError> {
        ensure(user.hasRole<Student>(), MissedStudentRoleError)
        val studyClass = classRepository.findById(classId)
        ensure(studyClass != null) { ClassNotExistsError(classId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        val task = taskRepository.findById(taskId)
        ensure(task != null) { TaskNotExistsError(taskId) }
        ensure(user.id in studyClass.data.students.ids) { ClassAccessDeniedError(classId) }
        ensure(contestId in studyClass.data.contests.ids) { ContestAccessDeniedError(contestId) }
        ensure(taskId in contest.data.tasks.ids) { TaskAccessDeniedError(taskId) }
        val entry = studentContestEntryRepository.findByContext(
            userId = user.id,
            studyClassId = classId,
            contestId = contestId,
        )
        ensure(entry != null) { ContestNotEnteredError(contestId) }
        val file = committedResourceFile(task, resourceId)
        ensure(file != null) { ResourceNotInCommittedTaskError(taskId, resourceId) }
        return file.asSuccess()
    }

    /**
     * Selects the submission with the highest final score; ties go to the earlier one by creation time and then id.
     */
    private fun bestSubmission(submissions: List<Submission>): Submission? = submissions
        .mapNotNull { submission -> finalScore(submission)?.let { score -> submission to score } }
        .minWithOrNull(
            compareByDescending<Pair<Submission, Int>> { (_, score) -> score }
                .thenBy { (submission, _) -> submission.createdAt }
                .thenBy { (submission, _) -> submission.id.value },
        )
        ?.first

    /**
     * Returns the score of the last judgment order, otherwise the verdict total, or `null` without a successful verdict.
     * Loads through the ports so that the lazy references of the returned submissions stay unresolved.
     */
    private fun finalScore(submission: Submission): Int? {
        val verdict = when (val status = submission.data.status) {
            SubmissionStatus.Queued, SubmissionStatus.InProgress -> return null
            is SubmissionStatus.Graded -> when (val grade = status.grade) {
                is GradingResult.Success -> grade.verdict
                is GradingResult.GradingError, GradingResult.Timeout -> return null
            }
        }
        val judgmentOrders = submission.data.judgmentOrders
        if (judgmentOrders.ids.isNotEmpty()) {
            val lastOrder = judgmentOrderRepository.load(judgmentOrders)
                .maxWith(compareBy<JudgmentOrder> { order -> order.createdAt }.thenBy { order -> order.id.value })
            return lastOrder.data.score.value
        }
        return verdictRepository.load(verdict).data.testVerdicts.sumOf { testVerdict -> testVerdict.score.value }
    }

    /**
     * Returns the file of [resourceId] if it is the statement or an exercise of the last committed revision of [task].
     */
    private fun committedResourceFile(task: Task, resourceId: DomainId): FileData? {
        val committed = when (val content = task.data.content) {
            is TaskContent.New -> return null
            is TaskContent.Uncommitted -> content.lastCommitted
            is TaskContent.Committed -> content.lastCommitted
        }
        return when (resourceId) {
            is StatementId -> if (committed.statement.id == resourceId) {
                checkNotNull(statementRepository.findById(resourceId)) {
                    "Statement id=${resourceId.value} of committed task id=${task.id.value} does not exist"
                }.data.file
            } else {
                null
            }
            is ExerciseId -> if (resourceId in committed.exercises.ids) {
                checkNotNull(exerciseRepository.findById(resourceId)) {
                    "Exercise id=${resourceId.value} of committed task id=${task.id.value} does not exist"
                }.data.file
            } else {
                null
            }
            else -> null
        }
    }
}
