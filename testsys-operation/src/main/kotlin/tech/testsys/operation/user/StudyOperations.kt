package tech.testsys.operation.user

import tech.testsys.domain.builder.api.solutionData
import tech.testsys.domain.builder.api.submissionData
import tech.testsys.domain.contract.FileContentReader
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.DeveloperSolutionRepository
import tech.testsys.domain.contract.persistence.repository.ExerciseRepository
import tech.testsys.domain.contract.persistence.repository.JudgmentOrderRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SolutionRepository
import tech.testsys.domain.contract.persistence.repository.StatementRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.VerdictRepository
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.TextLimits
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.CommittedTaskContent
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Exercise
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.GradingResult
import tech.testsys.domain.model.task.JudgmentOrder
import tech.testsys.domain.model.task.Statement
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.SubmissionStatus
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskContent
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.Verdict
import tech.testsys.domain.model.task.VerdictId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUser
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestAttemptExpiredError
import tech.testsys.operation.error.ContestEndedError
import tech.testsys.operation.error.ContestNotEnteredError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.DownloadParticipantTaskResourceError
import tech.testsys.operation.error.DownloadStudentTaskResourceError
import tech.testsys.operation.error.MissedParticipantRoleError
import tech.testsys.operation.error.MissedStudentRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ResourceNotInCommittedTaskError
import tech.testsys.operation.error.SendParticipantSolutionError
import tech.testsys.operation.error.SendStudentSolutionError
import tech.testsys.operation.error.SolutionLanguageNotAllowedError
import tech.testsys.operation.error.TaskAccessDeniedError
import tech.testsys.operation.error.TaskNotExistsError
import tech.testsys.operation.error.UploadedFileNameTooLongError
import tech.testsys.operation.error.ViewParticipantContestError
import tech.testsys.operation.error.ViewParticipantTaskError
import tech.testsys.operation.error.ViewStudentContestError
import tech.testsys.operation.error.ViewStudentTaskError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.hasRole
import tech.testsys.operation.util.loadByIdsAsMap
import java.time.Clock
import java.time.Instant

/**
 * Operations shared by participants and students for studying available contests.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class StudyOperations(
    private val fileContentReader: FileContentReader,
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
    private val solutionRepository: SolutionRepository,
    private val developerSolutionRepository: DeveloperSolutionRepository,
    private val clock: Clock,
) {

    /**
     * Returns the first entry time, original [contestId] in the competition of [user] and its tasks.
     * Viewing is allowed regardless of dates and does not save an entry; storage exceptions propagate.
     *
     * @return the exact first entry time or `null`, the original contest and its tasks in contest order.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewContest")
    fun viewContest(
        user: SingleRoleUser,
        contestId: ContestId,
    ): OperationResult<Triple<Instant?, Contest, List<Task>>, ViewParticipantContestError> =
        operation<Triple<Instant?, Contest, List<Task>>, ViewParticipantContestError> {
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
            return Triple(entry?.data?.enteredAt, contest, tasksOf(contest)).asSuccess()
        }

    /**
     * Returns the first entry time in [classId] for [user], original [contestId] and its tasks.
     * Viewing is allowed regardless of dates and does not save an entry; storage exceptions propagate.
     *
     * @return the exact first entry time or `null`, the original contest and its tasks in contest order.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewContest")
    fun viewContest(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
    ): OperationResult<Triple<Instant?, Contest, List<Task>>, ViewStudentContestError> =
        operation<Triple<Instant?, Contest, List<Task>>, ViewStudentContestError> {
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
            return Triple(entry?.data?.enteredAt, contest, tasksOf(contest)).asSuccess()
        }

    /**
     * Returns [taskId] of [contestId] in the competition of [user] with the grading submissions of [user] for it.
     * Requires a saved first entry; works after the contest end, saves nothing, and storage exceptions propagate.
     *
     * @return the task with its committed resources, allowed languages, submissions in repository order and the best one.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewTask")
    fun viewTask(user: SingleRoleUser, contestId: ContestId, taskId: TaskId): OperationResult<StudyTask, ViewParticipantTaskError> =
        operation<StudyTask, ViewParticipantTaskError> {
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
            return studyTask(task, submissions).asSuccess()
        }

    /**
     * Returns [taskId] of [contestId] in [classId] with the grading submissions of [user] for it in that contest.
     * Requires a saved first entry in [classId]; works after the contest end, saves nothing, and storage exceptions propagate.
     *
     * @return the task with its committed resources, allowed languages, submissions in repository order and the best one.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.viewTask")
    fun viewTask(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
        taskId: TaskId,
    ): OperationResult<StudyTask, ViewStudentTaskError> = operation<StudyTask, ViewStudentTaskError> {
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
        return studyTask(task, submissions).asSuccess()
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
        return FileData(uploadedFilename = file.uploadedFilename, content = fileContentReader.read(file)).asSuccess()
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
        return FileData(uploadedFilename = file.uploadedFilename, content = fileContentReader.read(file)).asSuccess()
    }

    /**
     * Saves [file] in [language] as a queued grading submission of [user] for [taskId] of [contestId] in their competition.
     * Requires a first entry before the contest and attempt ends; the caller passes the result to the grader after commit.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.sendSolution")
    fun sendSolution(
        user: SingleRoleUser,
        contestId: ContestId,
        taskId: TaskId,
        file: FileData,
        language: TrikSupportedLanguage,
    ): OperationResult<Submission, SendParticipantSolutionError> = operation<Submission, SendParticipantSolutionError> {
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
        val now = clock.instant()
        val endsAt = contest.data.endsAt
        ensure(endsAt == null || now.isBefore(endsAt)) { ContestEndedError(contestId, requireNotNull(endsAt)) }
        val expiresAt = contest.data.attemptDuration?.let { duration -> entry.data.enteredAt + duration }
        ensure(expiresAt == null || now.isBefore(expiresAt)) { ContestAttemptExpiredError(contestId, requireNotNull(expiresAt)) }
        ensure(isLanguageAllowed(task, language)) { SolutionLanguageNotAllowedError(taskId, language) }
        ensure(TextLimits.isValidUploadedFilename(file.uploadedFilename)) { UploadedFileNameTooLongError(file.uploadedFilename) }
        return saveSubmission(author = user.id, contestId = contestId, taskId = taskId, file = file, language = language).asSuccess()
    }

    /**
     * Saves [file] in [language] as a queued grading submission of [user] for [taskId] of [contestId] in [classId].
     * Requires a first entry in [classId] before the contest and attempt ends; the caller passes the result to the grader after commit.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.study.sendSolution")
    fun sendSolution(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
        taskId: TaskId,
        file: FileData,
        language: TrikSupportedLanguage,
    ): OperationResult<Submission, SendStudentSolutionError> = operation<Submission, SendStudentSolutionError> {
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
        val now = clock.instant()
        val endsAt = contest.data.endsAt
        ensure(endsAt == null || now.isBefore(endsAt)) { ContestEndedError(contestId, requireNotNull(endsAt)) }
        val expiresAt = contest.data.attemptDuration?.let { duration -> entry.data.enteredAt + duration }
        ensure(expiresAt == null || now.isBefore(expiresAt)) { ContestAttemptExpiredError(contestId, requireNotNull(expiresAt)) }
        ensure(isLanguageAllowed(task, language)) { SolutionLanguageNotAllowedError(taskId, language) }
        ensure(TextLimits.isValidUploadedFilename(file.uploadedFilename)) { UploadedFileNameTooLongError(file.uploadedFilename) }
        return saveSubmission(author = user.id, contestId = contestId, taskId = taskId, file = file, language = language).asSuccess()
    }

    /**
     * Returns the tasks of [contest] in contest order: the port returns them in any order.
     */
    private fun tasksOf(contest: Contest): List<Task> {
        val tasks = taskRepository.load(contest.data.tasks).associateBy { task -> task.id }
        return contest.data.tasks.ids.map { taskId -> tasks.getValue(taskId) }
    }

    /**
     * Returns [task] with the resources and languages of its last committed revision and [submissions] with the best one.
     */
    private fun studyTask(task: Task, submissions: List<Submission>): StudyTask {
        val committed = lastCommitted(task)
        val scoresById = finalScores(submissions).associate { (submission, score) -> submission.id to score }
        val solutionsById = solutionRepository.loadByIdsAsMap(submissions.map { submission -> submission.data.solution.id })
        val studySubmissions = submissions.map { submission ->
            val solution = solutionsById.getValue(submission.data.solution.id)
            StudySubmission(submission = submission, filename = solution.data.file.uploadedFilename, score = scoresById[submission.id])
        }
        return StudyTask(
            task = task,
            statement = committed?.let { revision -> statementRepository.load(revision.statement) },
            exercises = committed?.let { revision -> exerciseRepository.load(revision.exercises) }.orEmpty(),
            languages = allowedLanguages(committed),
            submissions = studySubmissions,
            best = bestSubmission(studySubmissions),
        )
    }

    /**
     * Selects the submission with the highest final score; ties go to the earlier one by creation time and then id.
     */
    private fun bestSubmission(submissions: List<StudySubmission>): StudySubmission? = submissions
        .filter { submission -> submission.score != null }
        .minWithOrNull(
            compareByDescending<StudySubmission> { submission -> submission.score }
                .thenBy { submission -> submission.submission.createdAt }
                .thenBy { submission -> submission.submission.id.value },
        )

    /**
     * Pairs each of [submissions] with a successful verdict with its score of the last judgment order, otherwise its
     * verdict total. Loads all judgment orders and the verdicts of submissions without orders through the ports, one call
     * each, so that the lazy references of the returned submissions stay unresolved.
     */
    private fun finalScores(submissions: List<Submission>): List<Pair<Submission, Int>> {
        val graded = submissions.mapNotNull { submission -> successfulVerdictOf(submission)?.let { verdict -> submission to verdict } }
        val ordersById = judgmentOrderRepository.loadByIdsAsMap(graded.flatMap { (submission, _) -> submission.data.judgmentOrders.ids })
        val verdictsById = verdictRepository.loadByIdsAsMap(
            graded.filter { (submission, _) -> submission.data.judgmentOrders.ids.isEmpty() }.map { (_, verdict) -> verdict.id },
        )

        return graded.map { (submission, verdict) ->
            val orderIds = submission.data.judgmentOrders.ids
            val score = if (orderIds.isNotEmpty()) {
                orderIds.map(ordersById::getValue)
                    .maxWith(compareBy<JudgmentOrder> { order -> order.createdAt }.thenBy { order -> order.id.value })
                    .data.score.value
            } else {
                verdictsById.getValue(verdict.id).data.testVerdicts.sumOf { testVerdict -> testVerdict.score.value }
            }
            submission to score
        }
    }

    /**
     * Returns the verdict of [submission] if it was graded successfully, otherwise `null`.
     */
    private fun successfulVerdictOf(submission: Submission): LazyEntity<VerdictId, Verdict>? = when (val status = submission.data.status) {
        SubmissionStatus.Queued, SubmissionStatus.InProgress -> null
        is SubmissionStatus.Graded -> when (val grade = status.grade) {
            is GradingResult.Success -> grade.verdict
            is GradingResult.GradingError, GradingResult.Timeout -> null
        }
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

    /**
     * Checks whether the last committed revision of [task] has a developer solution in [language]; a new task has none.
     */
    private fun isLanguageAllowed(task: Task, language: TrikSupportedLanguage): Boolean = language in allowedLanguages(lastCommitted(task))

    /**
     * Returns the distinct languages of the developer solutions of [committed] in their order, none without a revision.
     */
    private fun allowedLanguages(committed: CommittedTaskContent?): List<TrikSupportedLanguage> {
        if (committed == null) return emptyList()

        val developerSolutions = developerSolutionRepository.load(committed.developerSolutions)
        val solutionIds = developerSolutions.map { developerSolution -> developerSolution.data.solution.id }
        val solutions = solutionRepository.loadByIdsAsMap(solutionIds)
        return solutionIds.map { id -> solutions.getValue(id).data.language }.distinct()
    }

    /**
     * Returns the last committed revision of [task], or `null` for a new task.
     */
    private fun lastCommitted(task: Task): CommittedTaskContent? = when (val content = task.data.content) {
        is TaskContent.New -> null
        is TaskContent.Uncommitted -> content.lastCommitted
        is TaskContent.Committed -> content.lastCommitted
    }

    /**
     * Saves a new solution from [file] and [language] and a queued grading submission of it by [author].
     */
    private fun saveSubmission(
        author: UserId,
        contestId: ContestId,
        taskId: TaskId,
        file: FileData,
        language: TrikSupportedLanguage,
    ): Submission {
        val solution = solutionRepository.save(
            solutionData {
                file(file)
                when (language) {
                    TrikSupportedLanguage.Python -> this.language.python()
                    TrikSupportedLanguage.JavaScript -> this.language.javaScript()
                    TrikSupportedLanguage.VisualLanguage -> this.language.visualLanguage()
                }
            },
        )
        return submissionRepository.save(
            submissionData {
                this.author = author
                this.solution = solution.id
                task = taskId
                status.queued()
                kind.grading { contest = contestId }
            },
        )
    }

    /**
     * Task of a contest as a participant or a student sees it, with its last committed revision and their submissions.
     *
     * @property task the original task.
     * @property statement the statement of the last committed revision, or `null` if the task has no committed revision.
     * @property exercises the exercises of the last committed revision, empty if the task has no committed revision.
     * @property languages the distinct languages of the developer solutions of the last committed revision, in their order.
     * @property submissions the grading submissions of the user in repository order.
     * @property best the submission with the highest final score, or `null` if none has a successful verdict.
     * @since %CURRENT_VERSION%
     */
    data class StudyTask(
        val task: Task,
        val statement: Statement?,
        val exercises: List<Exercise>,
        val languages: List<TrikSupportedLanguage>,
        val submissions: List<StudySubmission>,
        val best: StudySubmission?,
    )

    /**
     * Submission of a [StudyTask] with the file name of its solution and its final score.
     *
     * @property submission the original submission with unresolved references.
     * @property filename the uploaded file name of the solution.
     * @property score the score of the last judgment order, otherwise the verdict total, or `null` without a successful verdict.
     * @since %CURRENT_VERSION%
     */
    data class StudySubmission(
        val submission: Submission,
        val filename: String,
        val score: Int?,
    )
}
