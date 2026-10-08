package tech.testsys.web.app.service.study

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.FileData
import tech.testsys.domain.model.task.Submission
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.user.StudyOperations
import tech.testsys.web.app.config.AfterCommitGrader
import tech.testsys.web.app.service.ContestVo
import tech.testsys.web.app.service.CurrentUser
import tech.testsys.web.app.service.TaskVo
import tech.testsys.web.app.service.toVo
import java.time.Instant

/**
 * Runs [StudyOperations] for the current user in one transaction per call.
 * A failed operation throws `OperationException`, which rolls the transaction back.
 * A sent solution is passed to grading after the transaction commits.
 *
 * @since %CURRENT_VERSION%
 */
@Service
@Transactional
class StudyService(
    private val operations: StudyOperations,
    private val currentUser: CurrentUser,
    private val grader: AfterCommitGrader,
) {
    /**
     * Runs [StudyOperations.viewContest] for a participant.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewContest(contestId: ContestId): Pair<Instant?, ContestVo> = operations.viewContest(currentUser.singleRoleUser(), contestId)
        .getOrThrow().let { (enteredAt, contest) -> enteredAt to contest.toVo() }

    /**
     * Runs [StudyOperations.viewContest] for a student of [classId].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewContest(classId: ClassId, contestId: ContestId): Pair<Instant?, ContestVo> =
        operations.viewContest(currentUser.multipleRoleUser(), classId, contestId)
            .getOrThrow().let { (enteredAt, contest) -> enteredAt to contest.toVo() }

    /**
     * Runs [StudyOperations.viewTask] for a participant.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewTask(contestId: ContestId, taskId: TaskId): Triple<TaskVo, List<SubmissionVo>, SubmissionVo?> =
        operations.viewTask(currentUser.singleRoleUser(), contestId, taskId).getOrThrow().toVo()

    /**
     * Runs [StudyOperations.viewTask] for a student of [classId].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun viewTask(classId: ClassId, contestId: ContestId, taskId: TaskId): Triple<TaskVo, List<SubmissionVo>, SubmissionVo?> =
        operations.viewTask(currentUser.multipleRoleUser(), classId, contestId, taskId).getOrThrow().toVo()

    /**
     * Runs [StudyOperations.downloadTaskResource] for a participant.
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun downloadTaskResource(contestId: ContestId, taskId: TaskId, resourceId: DomainId): FileData =
        operations.downloadTaskResource(currentUser.singleRoleUser(), contestId, taskId, resourceId).getOrThrow()

    /**
     * Runs [StudyOperations.downloadTaskResource] for a student of [classId].
     *
     * @since %CURRENT_VERSION%
     */
    @Transactional(readOnly = true)
    fun downloadTaskResource(classId: ClassId, contestId: ContestId, taskId: TaskId, resourceId: DomainId): FileData =
        operations.downloadTaskResource(currentUser.multipleRoleUser(), classId, contestId, taskId, resourceId).getOrThrow()

    /**
     * Runs [StudyOperations.sendSolution] for a participant and grades the submission after commit.
     *
     * @since %CURRENT_VERSION%
     */
    fun sendSolution(contestId: ContestId, taskId: TaskId, file: FileData, language: TrikSupportedLanguage): SubmissionVo =
        grade(operations.sendSolution(currentUser.singleRoleUser(), contestId, taskId, file, language).getOrThrow())

    /**
     * Runs [StudyOperations.sendSolution] for a student of [classId] and grades the submission after commit.
     *
     * @since %CURRENT_VERSION%
     */
    fun sendSolution(
        classId: ClassId,
        contestId: ContestId,
        taskId: TaskId,
        file: FileData,
        language: TrikSupportedLanguage,
    ): SubmissionVo = grade(
        operations.sendSolution(
            user = currentUser.multipleRoleUser(),
            classId = classId,
            contestId = contestId,
            taskId = taskId,
            file = file,
            language = language,
        ).getOrThrow(),
    )

    private fun grade(submission: Submission): SubmissionVo {
        grader.sendToGrade(submission)

        return submission.toVo()
    }

    private fun Triple<Task, List<Submission>, Submission?>.toVo() =
        Triple(first.toVo(), second.map { submission -> submission.toVo() }, third?.toVo())
}
