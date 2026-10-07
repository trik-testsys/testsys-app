package tech.testsys.operation.error

import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.DeveloperSolutionId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.StatementId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.domain.model.task.TrikSupportedLanguage
import tech.testsys.domain.model.task.VersionBucket
import java.time.Duration
import java.time.Instant

/**
 * Expected failure of an operation, returned in [OperationResult.Error].
 *
 * @since %CURRENT_VERSION%
 */
sealed interface OperationError

// region CommonTypes
// Kinds of failure shared by many operations: they let callers handle errors by kind, whatever operation returned them.

/**
 * An entity the operation refers to does not exist.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface EntityNotExistsError : OperationError

/**
 * The user has no access to an entity the operation refers to.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AccessDeniedError : OperationError

/**
 * The user does not have the role required by the operation.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface MissedRequiredRoleError : OperationError

/**
 * The user has no access to resource in current context
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ResourceAccessError : OperationError

// endregion

// region ManagerOperations

/**
 * Failure of creating a class owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface CreateClassError : OperationError

/**
 * Failure of creating a competition owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface CreateCompetitionError : OperationError

/**
 * Failure of listing classes owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewClassesError : OperationError

/**
 * Failure of viewing a class owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewClassError : OperationError

/**
 * Failure of listing competitions owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewCompetitionsError : OperationError

/**
 * Failure of viewing a competition owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewCompetitionError : OperationError

/**
 * Failure of viewing the results of a contest added to a class owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewClassContestError : OperationError

/**
 * Failure of viewing the results of a contest added to a competition owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewCompetitionContestError : OperationError

/**
 * Failure of adding a contest to a class owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddClassContestError : OperationError

/**
 * Failure of adding a contest to a competition owned by the manager.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddCompetitionContestError : OperationError

/**
 * The user does not hold the manager role.
 *
 * @since %CURRENT_VERSION%
 */
data object MissedManagerRoleError :
    CreateClassError,
    CreateCompetitionError,
    ViewClassesError,
    ViewClassError,
    ViewCompetitionsError,
    ViewCompetitionError,
    ViewClassContestError,
    ViewCompetitionContestError,
    AddClassContestError,
    AddCompetitionContestError,
    MissedRequiredRoleError

/**
 * The class name is empty or contains only whitespace.
 *
 * @since %CURRENT_VERSION%
 */
data object ClassNameBlankError : CreateClassError

/**
 * The class name exceeds 255 Unicode code points.
 *
 * @property className the supplied class name.
 * @since %CURRENT_VERSION%
 */
data class ClassNameTooLongError(val className: String) : CreateClassError

/**
 * The competition name is empty or contains only whitespace.
 *
 * @since %CURRENT_VERSION%
 */
data object CompetitionNameBlankError : CreateCompetitionError

/**
 * The competition name exceeds 255 Unicode code points.
 *
 * @property competitionName the supplied competition name.
 * @since %CURRENT_VERSION%
 */
data class CompetitionNameTooLongError(val competitionName: String) : CreateCompetitionError

/**
 * The class does not exist.
 *
 * @property classId the id of the missing class.
 * @since %CURRENT_VERSION%
 */
data class ClassNotExistsError(val classId: ClassId) :
    ViewClassError,
    ViewClassContestError,
    AddClassContestError,
    EntityNotExistsError

/**
 * The class belongs to another user.
 *
 * @property classId the id of the inaccessible class.
 * @since %CURRENT_VERSION%
 */
data class ClassAccessDeniedError(val classId: ClassId) :
    ViewClassError,
    ViewClassContestError,
    AddClassContestError,
    AccessDeniedError

/**
 * The competition does not exist.
 *
 * @property competitionId the id of the missing competition.
 * @since %CURRENT_VERSION%
 */
data class CompetitionNotExistsError(val competitionId: CompetitionId) :
    ViewCompetitionError,
    ViewCompetitionContestError,
    AddCompetitionContestError,
    EntityNotExistsError

/**
 * The competition belongs to another user.
 *
 * @property competitionId the id of the inaccessible competition.
 * @since %CURRENT_VERSION%
 */
data class CompetitionAccessDeniedError(val competitionId: CompetitionId) :
    ViewCompetitionError,
    ViewCompetitionContestError,
    AddCompetitionContestError,
    AccessDeniedError

/**
 * The contest is not added to the class.
 *
 * @property classId the id of the class.
 * @property contestId the id of the contest missing from the class.
 * @since %CURRENT_VERSION%
 */
data class ContestNotAddedToClassError(val classId: ClassId, val contestId: ContestId) : ViewClassContestError

/**
 * The contest is not added to the competition.
 *
 * @property competitionId the id of the competition.
 * @property contestId the id of the contest missing from the competition.
 * @since %CURRENT_VERSION%
 */
data class ContestNotAddedToCompetitionError(val competitionId: CompetitionId, val contestId: ContestId) :
    ViewCompetitionContestError

/**
 * The contest is already added to the class.
 *
 * @property classId the id of the class.
 * @property contestId the id of the contest already added to the class.
 * @since %CURRENT_VERSION%
 */
data class ContestAlreadyAddedToClassError(val classId: ClassId, val contestId: ContestId) : AddClassContestError

/**
 * The contest is already added to the competition.
 *
 * @property competitionId the id of the competition.
 * @property contestId the id of the contest already added to the competition.
 * @since %CURRENT_VERSION%
 */
data class ContestAlreadyAddedToCompetitionError(val competitionId: CompetitionId, val contestId: ContestId) :
    AddCompetitionContestError

// endregion

// region JudgeOperations

/**
 * Failure of listing verdicts available to the judge.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewResultsError : OperationError

/**
 * Failure of issuing a judgment order for a submission.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ChangeVerdictError : OperationError

/**
 * The user does not hold the judge role.
 *
 * @since %CURRENT_VERSION%
 */
data object MissedJudgeRoleError : ViewResultsError, ChangeVerdictError, MissedRequiredRoleError

/**
 * The submission does not exist.
 *
 * @property submissionId the id of the missing submission.
 * @since %CURRENT_VERSION%
 */
data class SubmissionNotExistsError(val submissionId: SubmissionId) : ChangeVerdictError, EntityNotExistsError

/**
 * The submission is outside the judge's access or is a developer solution test.
 *
 * @property submissionId the id of the inaccessible submission.
 * @since %CURRENT_VERSION%
 */
data class SubmissionAccessDeniedError(val submissionId: SubmissionId) : ChangeVerdictError, AccessDeniedError

/**
 * The submission has no successful automatic grading result.
 *
 * @property submissionId the id of the submission.
 * @since %CURRENT_VERSION%
 */
data class SubmissionNotSuccessfullyGradedError(val submissionId: SubmissionId) : ChangeVerdictError

/**
 * The requested judgment score is negative.
 *
 * @property score the invalid score.
 * @since %CURRENT_VERSION%
 */
data class NegativeJudgmentScoreError(val score: Score) : ChangeVerdictError

/**
 * The judgment reason is empty or contains only whitespace.
 *
 * @since %CURRENT_VERSION%
 */
data object BlankJudgmentReasonError : ChangeVerdictError

// endregion

// region DeveloperOperations

/**
 * Expected failure of requesting testing of an owned task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface TestTaskError : OperationError

/**
 * Expected failure of viewing the testing history of an owned task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewTaskValidationRequestsError : OperationError

/**
 * Expected failure of committing the working revision of an owned task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface CommitTaskError : OperationError

/**
 * The working revision has no successfully completed validation request with a matching snapshot.
 *
 * @property taskId the identifier of the task being committed.
 * @since %CURRENT_VERSION%
 */
data class TaskNotTestedError(val taskId: TaskId) : CommitTaskError

/**
 * The working task has no attached statement.
 *
 * @property taskId the identifier of the tested or committed task.
 * @since %CURRENT_VERSION%
 */
data class TaskTestingNoStatementError(val taskId: TaskId) : TestTaskError, CommitTaskError

/**
 * The working task has no exercise for an author solution's language.
 *
 * @property taskId the identifier of the tested or committed task.
 * @property language the author solution language without an exercise.
 * @since %CURRENT_VERSION%
 */
data class TaskTestingNoExerciseForLanguageError(val taskId: TaskId, val language: TrikSupportedLanguage) : TestTaskError, CommitTaskError

/**
 * The working task has no attached polygons.
 *
 * @property taskId the tested task identifier.
 * @since %CURRENT_VERSION%
 */
data class TaskTestingNoPolygonsError(val taskId: TaskId) : TestTaskError

/**
 * The working task has no attached author solutions.
 *
 * @property taskId the tested task identifier.
 * @since %CURRENT_VERSION%
 */
data class TaskTestingNoDeveloperSolutionsError(val taskId: TaskId) : TestTaskError

/**
 * The working task supports no TRIK Studio version.
 *
 * @property taskId the tested task identifier.
 * @since %CURRENT_VERSION%
 */
data class TaskTestingNoTrikStudioVersionsError(val taskId: TaskId) : TestTaskError

/**
 * Failure of listing contests available to the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewContestsError : OperationError

/**
 * Failure of viewing a contest available to the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewContestError : OperationError

/**
 * Failure of reverting a task owned by the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface RevertTaskError : OperationError

/**
 * Failure of listing tasks available to the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewTasksError : OperationError

/**
 * Failure of viewing a task owned by the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewTaskError : OperationError

/**
 * Failure of editing information of a task owned by the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface EditTaskInfoError : OperationError

/**
 * Failure of listing the developer's resources.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewResourcesError : OperationError

/**
 * Failure of viewing a resource and its existing versions.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewResourceError : OperationError

/**
 * Failure of obtaining the file reference of a resource version.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DownloadResourceVersionError : OperationError

/**
 * Failure of updating a statement uploaded to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface UpdateStatementError : OperationError

/**
 * Failure of updating an exercise uploaded to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface UpdateExerciseError : OperationError

/**
 * Failure of updating a polygon uploaded to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface UpdateTestError : OperationError

/**
 * Failure of updating a developer solution uploaded to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface UpdateDeveloperSolutionError : OperationError

/**
 * Failure of uploading a new statement to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddStatementError : OperationError

/**
 * Failure of uploading a new exercise to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddExerciseError : OperationError

/**
 * Failure of uploading a new polygon to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddTestError : OperationError

/**
 * Failure of uploading a new developer solution to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AddDeveloperSolutionError : OperationError

/**
 * Failure of creating a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface CreateTaskError : OperationError

/**
 * Failure of creating a contest.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface CreateContestError : OperationError

/**
 * Failure of editing a contest owned by the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface EditContestError : OperationError

/**
 * Failure of sharing a contest to communities.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ShareContestError : OperationError

/**
 * Failure of attaching a task to a contest.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachTaskError : OperationError

/**
 * Failure of detaching a task from a contest.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DetachTaskError : OperationError

/**
 * Failure of deleting a contest owned by the developer.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DeleteContestError : OperationError

/**
 * Failure of attaching a statement to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachStatementError : OperationError

/**
 * Failure of attaching a exercise to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachExerciseError : OperationError

/**
 * Failure of attaching a test to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachTestError : OperationError

/**
 * Failure of attaching a developersolution to a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface AttachDeveloperSolutionError : OperationError

/**
 * Failure of sharing a task to communities.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ShareTaskError : OperationError

/**
 * Failure of detaching a statement version from a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DetachStatementError : OperationError

/**
 * Failure of detaching an exercise version from a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DetachExerciseError : OperationError

/**
 * Failure of detaching a polygon version from a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DetachTestError : OperationError

/**
 * Failure of detaching a developer solution version from a task.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DetachDeveloperSolutionError : OperationError
// endregion

// region Errors

/**
 * The user does not have the developer role.
 *
 * @since %CURRENT_VERSION%
 */
data object MissedDeveloperRoleError :
    TestTaskError,
    ViewTaskValidationRequestsError,
    CommitTaskError,
    AttachTaskError,
    DetachTaskError,
    DeleteContestError,
    ViewContestsError,
    ViewContestError,
    RevertTaskError,
    ShareContestError,
    EditContestError,
    EditTaskInfoError,
    ViewTaskError,
    ViewTasksError,
    DetachStatementError,
    DetachExerciseError,
    DetachTestError,
    DetachDeveloperSolutionError,
    ViewResourcesError,
    ViewResourceError,
    DownloadResourceVersionError,
    UpdateStatementError,
    UpdateExerciseError,
    UpdateTestError,
    UpdateDeveloperSolutionError,
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    MissedRequiredRoleError,
    CreateTaskError,
    CreateContestError,
    AttachStatementError,
    ShareTaskError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError

/**
 * The contest end is specified without its start.
 *
 * @property endsAt the end moment supplied without a start.
 * @since %CURRENT_VERSION%
 */
data class ContestEndWithoutStartError(val endsAt: Instant) : CreateContestError, EditContestError

/**
 * The contest end is not later than its start.
 *
 * @property startsAt the requested start moment.
 * @property endsAt the requested end moment.
 * @since %CURRENT_VERSION%
 */
data class ContestEndNotAfterStartError(val startsAt: Instant, val endsAt: Instant) : CreateContestError, EditContestError

/**
 * The individual time limit is zero or negative.
 *
 * @property attemptDuration the nonpositive individual limit.
 * @since %CURRENT_VERSION%
 */
data class NonPositiveAttemptDurationError(val attemptDuration: Duration) : CreateContestError

/**
 * The individual time limit exceeds the contest interval.
 *
 * @property attemptDuration the requested individual limit.
 * @property contestDuration the available contest interval.
 * @since %CURRENT_VERSION%
 */
data class AttemptDurationExceedsContestDurationError(
    val attemptDuration: Duration,
    val contestDuration: Duration,
) : CreateContestError, EditContestError

/**
 * The contest does not exist.
 *
 * @property contestId the id of the missing contest.
 * @since %CURRENT_VERSION%
 */
data class ContestNotExistsError(
    val contestId: ContestId,
) : EntityNotExistsError,
    EditContestError,
    ShareContestError,
    AttachTaskError,
    DetachTaskError,
    ViewContestError,
    DeleteContestError,
    EnterParticipantContestError,
    EnterStudentContestError,
    ViewParticipantContestError,
    ViewStudentContestError,
    ViewParticipantTaskError,
    ViewStudentTaskError,
    DownloadParticipantTaskResourceError,
    DownloadStudentTaskResourceError,
    SendParticipantSolutionError,
    SendStudentSolutionError,
    ViewClassContestError,
    ViewCompetitionContestError,
    AddClassContestError,
    AddCompetitionContestError

/**
 * The user has no access to the contest for the requested operation.
 *
 * @property contestId the id of the contest.
 * @since %CURRENT_VERSION%
 */
data class ContestAccessDeniedError(
    val contestId: ContestId,
) : AccessDeniedError,
    EditContestError,
    ShareContestError,
    AttachTaskError,
    DetachTaskError,
    ViewContestError,
    DeleteContestError,
    EnterParticipantContestError,
    EnterStudentContestError,
    ViewParticipantContestError,
    ViewStudentContestError,
    ViewParticipantTaskError,
    ViewStudentTaskError,
    DownloadParticipantTaskResourceError,
    DownloadStudentTaskResourceError,
    SendParticipantSolutionError,
    SendStudentSolutionError,
    AddClassContestError,
    AddCompetitionContestError

/**
 * The contest is shared to at least one community.
 *
 * @property contestId the id of the shared contest.
 * @since %CURRENT_VERSION%
 */
data class ContestAlreadySharedError(val contestId: ContestId) : EditContestError, AttachTaskError, DetachTaskError, DeleteContestError

/**
 * The task does not exist.
 *
 * @property taskId the id of the missing task.
 * @since %CURRENT_VERSION%
 */
data class TaskNotExistsError(val taskId: TaskId) :
    TestTaskError,
    ViewTaskValidationRequestsError,
    CommitTaskError,
    AttachTaskError,
    DetachTaskError,
    RevertTaskError,
    EditTaskInfoError,
    ViewTaskError,
    DetachStatementError,
    DetachExerciseError,
    DetachTestError,
    DetachDeveloperSolutionError,
    ViewResourceError,
    DownloadResourceVersionError,
    UpdateStatementError,
    UpdateExerciseError,
    UpdateTestError,
    UpdateDeveloperSolutionError,
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    EntityNotExistsError,
    AttachStatementError,
    ShareTaskError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError,
    ViewParticipantTaskError,
    ViewStudentTaskError,
    DownloadParticipantTaskResourceError,
    DownloadStudentTaskResourceError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The statement does not exist.
 *
 * @property statementId the id of the missing statement.
 * @since %CURRENT_VERSION%
 */
data class StatementNotExistsError(val statementId: StatementId) :
    EntityNotExistsError,
    AttachStatementError,
    UpdateStatementError,
    DetachStatementError

/**
 * The statement's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property statementId the id of the resource outside the task's uploaded chains.
 * @since %CURRENT_VERSION%
 */
data class StatementNotUploadedToTaskError(val taskId: TaskId, val statementId: StatementId) :
    DetachStatementError,
    UpdateStatementError,
    AttachStatementError,
    ResourceAccessError

/**
 * The task already has a statement.
 *
 * @since %CURRENT_VERSION%
 */
data object TaskAlreadyHasStatementError : AttachStatementError

/**
 * The community does not exist.
 *
 * @property communityId the id of the missing community.
 * @since %CURRENT_VERSION%
 */
data class CommunityNotExistsError(val communityId: CommunityId) : EntityNotExistsError, ShareTaskError, ShareContestError

/**
 * The user lacks the access to the task required by the operation.
 *
 * @property taskId the id of the task.
 * @since %CURRENT_VERSION%
 */
data class TaskAccessDeniedError(val taskId: TaskId) :
    TestTaskError,
    ViewTaskValidationRequestsError,
    CommitTaskError,
    AttachTaskError,
    RevertTaskError,
    EditTaskInfoError,
    ViewTaskError,
    DetachStatementError,
    DetachExerciseError,
    DetachTestError,
    DetachDeveloperSolutionError,
    ViewResourceError,
    DownloadResourceVersionError,
    UpdateStatementError,
    UpdateExerciseError,
    UpdateTestError,
    UpdateDeveloperSolutionError,
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    AccessDeniedError,
    AttachStatementError,
    ShareTaskError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError,
    ViewParticipantTaskError,
    ViewStudentTaskError,
    DownloadParticipantTaskResourceError,
    DownloadStudentTaskResourceError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The user is not a member of the community.
 *
 * @property communityId the id of the community.
 * @since %CURRENT_VERSION%
 */
data class CommunityAccessDeniedError(val communityId: CommunityId) : AccessDeniedError, ShareTaskError, ShareContestError

/**
 * The task has no committed version.
 *
 * @property taskId the id of the task.
 * @since %CURRENT_VERSION%
 */
data class TaskNotCommittedError(val taskId: TaskId) : ShareTaskError, AttachTaskError, RevertTaskError

/**
 * The task's last committed revision does not support the contest's TRIK Studio version.
 *
 * @property taskId the id of the incompatible task.
 * @property trikStudioVersion the version required by the contest.
 * @since %CURRENT_VERSION%
 */
data class TaskTrikStudioVersionNotSupportedError(
    val taskId: TaskId,
    val trikStudioVersion: TrikStudioVersion,
) : AttachTaskError, TestTaskError, CommitTaskError

/**
 * The task is already attached to the contest.
 *
 * @property contestId the id of the contest.
 * @property taskId the id of the already attached task.
 * @since %CURRENT_VERSION%
 */
data class TaskAlreadyAttachedToContestError(val contestId: ContestId, val taskId: TaskId) : AttachTaskError

/**
 * The task is not attached to the contest.
 *
 * @property contestId the id of the contest.
 * @property taskId the id of the task absent from the contest.
 * @since %CURRENT_VERSION%
 */
data class TaskNotAttachedToContestError(val contestId: ContestId, val taskId: TaskId) : DetachTaskError

/**
 * The task has no uncommitted changes to revert.
 *
 * @property taskId the id of the committed task.
 * @since %CURRENT_VERSION%
 */
data class TaskAlreadyCommittedError(val taskId: TaskId) : RevertTaskError, TestTaskError, CommitTaskError
// endregion

/**
 * The requested statement version is no longer the latest version in its chain.
 *
 * @property statementId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class StatementVersionNotLatestError(val statementId: StatementId) : AttachStatementError, UpdateStatementError

/**
 * The exercise does not exist.
 *
 * @property exerciseId the id of the missing resource.
 * @since %CURRENT_VERSION%
 */
data class ExerciseNotExistsError(val exerciseId: ExerciseId) :
    EntityNotExistsError,
    AttachExerciseError,
    UpdateExerciseError,
    DetachExerciseError

/**
 * The exercise's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property exerciseId the id of the resource outside the task's uploaded chains.
 * @since %CURRENT_VERSION%
 */
data class ExerciseNotUploadedToTaskError(val taskId: TaskId, val exerciseId: ExerciseId) :
    DetachExerciseError,
    AttachExerciseError,
    UpdateExerciseError,
    ResourceAccessError

/**
 * The requested exercise version is no longer the latest version in its chain.
 *
 * @property exerciseId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class ExerciseVersionNotLatestError(val exerciseId: ExerciseId) : AttachExerciseError, UpdateExerciseError

/**
 * The test does not exist.
 *
 * @property testId the id of the missing resource.
 * @since %CURRENT_VERSION%
 */
data class TestNotExistsError(val testId: TestId) :
    EntityNotExistsError,
    AttachTestError,
    UpdateTestError,
    DetachTestError

/**
 * The test's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property testId the id of the resource outside the task's uploaded chains.
 * @since %CURRENT_VERSION%
 */
data class TestNotUploadedToTaskError(val taskId: TaskId, val testId: TestId) :
    AttachTestError,
    UpdateTestError,
    DetachTestError,
    ResourceAccessError

/**
 * The requested test version is no longer the latest version in its chain.
 *
 * @property testId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class TestVersionNotLatestError(val testId: TestId) : AttachTestError, UpdateTestError

/**
 * The developer solution does not exist.
 *
 * @property developerSolutionId the id of the missing resource.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionNotExistsError(
    val developerSolutionId: DeveloperSolutionId,
) : EntityNotExistsError, AttachDeveloperSolutionError, UpdateDeveloperSolutionError, DetachDeveloperSolutionError

/**
 * The developer solution's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property developerSolutionId the id of the resource outside the task's uploaded chains.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionNotUploadedToTaskError(
    val taskId: TaskId,
    val developerSolutionId: DeveloperSolutionId,
) : AttachDeveloperSolutionError, UpdateDeveloperSolutionError, DetachDeveloperSolutionError, ResourceAccessError

/**
 * The requested developer solution version is no longer the latest version in its chain.
 *
 * @property developerSolutionId the id of the requested version.
 * @since %CURRENT_VERSION%
 */
data class DeveloperSolutionVersionNotLatestError(val developerSolutionId: DeveloperSolutionId) :
    AttachDeveloperSolutionError,
    UpdateDeveloperSolutionError

/**
 * A version from the resource chain is already attached to the editable task revision.
 *
 * @property taskId the id of the task.
 * @property versionBucket the already attached resource chain.
 * @since %CURRENT_VERSION%
 */
data class ResourceAlreadyAttachedError(
    val taskId: TaskId,
    val versionBucket: VersionBucket,
) :
    AttachStatementError,
    AttachExerciseError,
    AttachTestError,
    AttachDeveloperSolutionError

/**
 * Another exercise in the editable task revision already uses the requested programming language.
 *
 * @property taskId the id of the task.
 * @property language the programming language already occupied in the revision.
 * @since %CURRENT_VERSION%
 */
data class ExerciseLanguageAlreadyAttachedError(
    val taskId: TaskId,
    val language: TrikSupportedLanguage,
) : AttachExerciseError

/**
 * The requested resource version is absent from the task's editable revision.
 *
 * @property taskId the task identifier.
 * @property versionId the unattached version identifier.
 * @since %CURRENT_VERSION%
 */
data class ResourceVersionNotAttachedError(val taskId: TaskId, val versionId: DomainId) :
    DetachStatementError,
    DetachExerciseError,
    DetachTestError,
    DetachDeveloperSolutionError

/**
 * The requested resource chain does not exist.
 *
 * @property versionBucket the missing resource chain.
 * @since %CURRENT_VERSION%
 */
data class ResourceNotExistsError(val versionBucket: VersionBucket) : EntityNotExistsError, ViewResourceError, DownloadResourceVersionError

/**
 * The requested resource chain is not uploaded to the task.
 *
 * @property taskId the task identifier.
 * @property versionBucket the resource chain outside the task.
 * @since %CURRENT_VERSION%
 */
data class ResourceNotUploadedToTaskError(val taskId: TaskId, val versionBucket: VersionBucket) :
    ResourceAccessError,
    ViewResourceError,
    DownloadResourceVersionError

/**
 * The requested resource version does not exist in the selected chain.
 *
 * @property versionBucket the selected resource chain.
 * @property versionId the missing version identifier.
 * @since %CURRENT_VERSION%
 */
data class ResourceVersionNotExistsError(val versionBucket: VersionBucket, val versionId: DomainId) :
    EntityNotExistsError,
    DownloadResourceVersionError

// region StudyOperations

/**
 * Failure of viewing a contest in the participant's competition.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewParticipantContestError : OperationError

/**
 * Failure of viewing a contest in the selected class.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewStudentContestError : OperationError

/**
 * Failure of listing contests of the participant.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewParticipantContestsError : OperationError

/**
 * Failure of listing contests of the selected class.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewStudentContestsError : OperationError

/**
 * Failure of listing classes of the student.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewStudentClassesError : OperationError

/**
 * Failure of entering a participant contest.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface EnterParticipantContestError : OperationError

/**
 * Failure of entering a student contest.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface EnterStudentContestError : OperationError

/**
 * Failure of viewing a task of a contest in the participant's competition.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewParticipantTaskError : OperationError

/**
 * Failure of viewing a task of a contest in the selected class.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface ViewStudentTaskError : OperationError

/**
 * Failure of downloading a statement or an exercise of a task in the participant's competition.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DownloadParticipantTaskResourceError : OperationError

/**
 * Failure of downloading a statement or an exercise of a task in the selected class.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface DownloadStudentTaskResourceError : OperationError

/**
 * Failure of sending a solution for a task of a contest in the participant's competition.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface SendParticipantSolutionError : OperationError

/**
 * Failure of sending a solution for a task of a contest in the selected class.
 *
 * @since %CURRENT_VERSION%
 */
sealed interface SendStudentSolutionError : OperationError

/**
 * The user is not a participant.
 *
 * @since %CURRENT_VERSION%
 */
data object MissedParticipantRoleError :
    MissedRequiredRoleError,
    ViewParticipantContestsError,
    EnterParticipantContestError,
    ViewParticipantContestError,
    ViewParticipantTaskError,
    DownloadParticipantTaskResourceError,
    SendParticipantSolutionError

/**
 * The user does not hold the student role.
 *
 * @since %CURRENT_VERSION%
 */
data object MissedStudentRoleError :
    MissedRequiredRoleError,
    ViewStudentContestsError,
    ViewStudentClassesError,
    EnterStudentContestError,
    ViewStudentContestError,
    ViewStudentTaskError,
    DownloadStudentTaskResourceError,
    SendStudentSolutionError

/**
 * The competition does not exist.
 *
 * @property competitionId the missing competition.
 * @since %CURRENT_VERSION%
 */
data class CompetitionNotExistsError(val competitionId: CompetitionId) :
    EntityNotExistsError,
    ViewParticipantContestsError,
    EnterParticipantContestError,
    ViewParticipantContestError,
    ViewParticipantTaskError,
    DownloadParticipantTaskResourceError,
    SendParticipantSolutionError

/**
 * The class does not exist.
 *
 * @property classId the missing class.
 * @since %CURRENT_VERSION%
 */
data class ClassNotExistsError(val classId: ClassId) :
    EntityNotExistsError,
    ViewStudentContestsError,
    EnterStudentContestError,
    ViewStudentContestError,
    ViewStudentTaskError,
    DownloadStudentTaskResourceError,
    SendStudentSolutionError

/**
 * The user is not enrolled in the selected class.
 *
 * @property classId the inaccessible class.
 * @since %CURRENT_VERSION%
 */
data class ClassAccessDeniedError(val classId: ClassId) :
    AccessDeniedError,
    ViewStudentContestsError,
    EnterStudentContestError,
    ViewStudentContestError,
    ViewStudentTaskError,
    DownloadStudentTaskResourceError,
    SendStudentSolutionError

/**
 * The first entry precedes the contest start.
 *
 * @property contestId the requested contest.
 * @property startsAt the interval boundary.
 * @since %CURRENT_VERSION%
 */
data class ContestNotStartedError(val contestId: ContestId, val startsAt: Instant) :
    EnterParticipantContestError, EnterStudentContestError

/**
 * The first entry or a solution submission is at or after the contest end.
 *
 * @property contestId the requested contest.
 * @property endsAt the interval boundary.
 * @since %CURRENT_VERSION%
 */
data class ContestEndedError(val contestId: ContestId, val endsAt: Instant) :
    EnterParticipantContestError,
    EnterStudentContestError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The user has no first entry to the contest in the selected context.
 *
 * @property contestId the requested contest.
 * @since %CURRENT_VERSION%
 */
data class ContestNotEnteredError(val contestId: ContestId) :
    ViewParticipantTaskError,
    ViewStudentTaskError,
    DownloadParticipantTaskResourceError,
    DownloadStudentTaskResourceError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * A solution submission is at or after the first entry in the selected context plus the contest attempt duration.
 *
 * @property contestId the requested contest.
 * @property expiresAt the end of the individual attempt.
 * @since %CURRENT_VERSION%
 */
data class ContestAttemptExpiredError(val contestId: ContestId, val expiresAt: Instant) :
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The last committed revision of the task has no developer solution in the language of the sent solution.
 *
 * @property taskId the requested task.
 * @property language the language of the sent solution.
 * @since %CURRENT_VERSION%
 */
data class SolutionLanguageNotAllowedError(val taskId: TaskId, val language: TrikSupportedLanguage) :
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The requested version is not a statement or an exercise of the last committed revision of the task.
 *
 * @property taskId the requested task.
 * @property resourceId the requested version identifier.
 * @since %CURRENT_VERSION%
 */
data class ResourceNotInCommittedTaskError(val taskId: TaskId, val resourceId: DomainId) :
    ResourceAccessError,
    DownloadParticipantTaskResourceError,
    DownloadStudentTaskResourceError

// endregion
