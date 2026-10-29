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
import tech.testsys.domain.model.user.CommunityRole
import tech.testsys.domain.model.user.RegistrationRequestId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import java.time.Duration
import java.time.Instant

/**
 * Expected failure of an operation, returned in [OperationResult.Error].
 *
 * @since 1.0.0-beta.1
 */
sealed interface OperationError

// region CommonTypes
// Kinds of failure shared by many operations: they let callers handle errors by kind, whatever operation returned them.

/**
 * An entity the operation refers to does not exist.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EntityNotExistsError : OperationError

/**
 * The user has no access to an entity the operation refers to.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AccessDeniedError : OperationError

/**
 * The user does not have the role required by the operation.
 *
 * @since 1.0.0-beta.1
 */
sealed interface MissedRequiredRoleError : OperationError

/**
 * The user has no access to resource in current context
 *
 * @since 1.0.0-beta.1
 */
sealed interface ResourceAccessError : OperationError

// endregion

// region ManagerOperations

/**
 * Failure of creating a class owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateClassError : OperationError

/**
 * Failure of creating a competition owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateCompetitionError : OperationError

/**
 * Failure of listing classes owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewClassesError : OperationError

/**
 * Failure of viewing a class owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewClassError : OperationError

/**
 * Failure of listing competitions owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewCompetitionsError : OperationError

/**
 * Failure of viewing a competition owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewCompetitionError : OperationError

/**
 * Failure of viewing the results of a contest added to a class owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewClassContestError : OperationError

/**
 * Failure of viewing the results of a contest added to a competition owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewCompetitionContestError : OperationError

/**
 * Failure of adding a contest to a class owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AddClassContestError : OperationError

/**
 * Failure of adding a contest to a competition owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AddCompetitionContestError : OperationError

/**
 * Failure of creating participants in a competition owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateParticipantsError : OperationError

/**
 * Failure of creating or replacing the invite code of a class owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateClassInviteError : OperationError

/**
 * Failure of extending the invite code of a class owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ExtendClassInviteError : OperationError

/**
 * Failure of replacing the expired invite code of a class on behalf of its owning manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RefreshClassInviteError : OperationError

/**
 * Failure of deleting selected participants of a competition owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DeleteParticipantsError : OperationError

/**
 * Failure of listing the contests the manager can add to classes and competitions.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewAvailableContestsError : OperationError

/**
 * Failure of editing the name and description of a class owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EditClassError : OperationError

/**
 * Failure of editing the name and description of a competition owned by the manager.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EditCompetitionError : OperationError

/**
 * The user does not hold the manager role.
 *
 * @since 1.0.0-beta.1
 */
data object MissedManagerRoleError :
    DeleteParticipantsError,
    ViewAvailableContestsError,
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
    CreateParticipantsError,
    CreateClassInviteError,
    ExtendClassInviteError,
    RefreshClassInviteError,
    EditClassError,
    EditCompetitionError,
    MissedRequiredRoleError

/**
 * The class name is empty or contains only whitespace.
 *
 * @since 1.0.0-beta.1
 */
data object ClassNameBlankError : CreateClassError, EditClassError

/**
 * The class name exceeds 255 Unicode code points.
 *
 * @property className the supplied class name.
 * @since 1.0.0-beta.1
 */
data class ClassNameTooLongError(val className: String) : CreateClassError, EditClassError

/**
 * The competition name is empty or contains only whitespace.
 *
 * @since 1.0.0-beta.1
 */
data object CompetitionNameBlankError : CreateCompetitionError, EditCompetitionError

/**
 * The competition name exceeds 255 Unicode code points.
 *
 * @property competitionName the supplied competition name.
 * @since 1.0.0-beta.1
 */
data class CompetitionNameTooLongError(val competitionName: String) : CreateCompetitionError, EditCompetitionError

/**
 * The class does not exist.
 *
 * @property classId the id of the missing class.
 * @since 1.0.0-beta.1
 */
data class ClassNotExistsError(val classId: ClassId) :
    ViewClassError,
    EditClassError,
    ViewClassContestError,
    AddClassContestError,
    CreateClassInviteError,
    ExtendClassInviteError,
    RefreshClassInviteError,
    ViewStudentContestsError,
    EnterStudentContestError,
    ViewStudentContestError,
    ViewStudentTaskError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
    SendStudentSolutionError,
    EntityNotExistsError

/**
 * The user has no access to the class for the requested operation: it belongs to another user,
 * or the user is not enrolled in it.
 *
 * @property classId the id of the inaccessible class.
 * @since 1.0.0-beta.1
 */
data class ClassAccessDeniedError(val classId: ClassId) :
    ViewClassError,
    EditClassError,
    ViewClassContestError,
    AddClassContestError,
    CreateClassInviteError,
    ExtendClassInviteError,
    RefreshClassInviteError,
    ViewStudentContestsError,
    EnterStudentContestError,
    ViewStudentContestError,
    ViewStudentTaskError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
    SendStudentSolutionError,
    AccessDeniedError

/**
 * The competition does not exist.
 *
 * @property competitionId the id of the missing competition.
 * @since 1.0.0-beta.1
 */
data class CompetitionNotExistsError(val competitionId: CompetitionId) :
    DeleteParticipantsError,
    EditCompetitionError,
    ViewCompetitionError,
    ViewCompetitionContestError,
    AddCompetitionContestError,
    CreateParticipantsError,
    ViewParticipantContestsError,
    EnterParticipantContestError,
    ViewParticipantContestError,
    ViewParticipantTaskError,
    DownloadParticipantTaskResourceError,
    DownloadParticipantSolutionError,
    SendParticipantSolutionError,
    EntityNotExistsError

/**
 * The competition is not accessible to the user.
 *
 * @property competitionId the id of the inaccessible competition.
 * @since 1.0.0-beta.1
 */
data class CompetitionAccessDeniedError(val competitionId: CompetitionId) :
    DeleteParticipantsError,
    EditCompetitionError,
    ViewCompetitionError,
    ViewCompetitionContestError,
    AddCompetitionContestError,
    CreateParticipantsError,
    AccessDeniedError

/**
 * The contest is not added to the class.
 *
 * @property classId the id of the class.
 * @property contestId the id of the contest missing from the class.
 * @since 1.0.0-beta.1
 */
data class ContestNotAddedToClassError(val classId: ClassId, val contestId: ContestId) : ViewClassContestError

/**
 * The contest is not added to the competition.
 *
 * @property competitionId the id of the competition.
 * @property contestId the id of the contest missing from the competition.
 * @since 1.0.0-beta.1
 */
data class ContestNotAddedToCompetitionError(val competitionId: CompetitionId, val contestId: ContestId) :
    ViewCompetitionContestError

/**
 * The contest is already added to the class.
 *
 * @property classId the id of the class.
 * @property contestId the id of the contest already added to the class.
 * @since 1.0.0-beta.1
 */
data class ContestAlreadyAddedToClassError(val classId: ClassId, val contestId: ContestId) : AddClassContestError

/**
 * The contest is already added to the competition.
 *
 * @property competitionId the id of the competition.
 * @property contestId the id of the contest already added to the competition.
 * @since 1.0.0-beta.1
 */
data class ContestAlreadyAddedToCompetitionError(val competitionId: CompetitionId, val contestId: ContestId) :
    AddCompetitionContestError

/**
 * The requested number of new participants is zero or negative.
 *
 * @property participantCount the requested number of new participants.
 * @since 1.0.0-beta.1
 */
data class NonPositiveParticipantCountError(val participantCount: Int) : CreateParticipantsError

/**
 * The total number of participants in the competition would exceed the configured maximum.
 *
 * @property competitionId the id of the competition.
 * @property currentParticipantCount the number of participants already in the competition.
 * @property participantCount the requested number of new participants.
 * @property maxParticipants the maximum total number of participants read for this call.
 * @since 1.0.0-beta.1
 */
data class CompetitionParticipantLimitExceededError(
    val competitionId: CompetitionId,
    val currentParticipantCount: Int,
    val participantCount: Int,
    val maxParticipants: Int,
) : CreateParticipantsError

/**
 * The participant does not exist or does not belong to the competition.
 *
 * @property competitionId the id of the competition.
 * @property participantId the id of the missing participant.
 * @since 1.0.0-beta.1
 */
data class CompetitionParticipantNotExistsError(val competitionId: CompetitionId, val participantId: SingleRoleUserId) :
    DeleteParticipantsError,
    EntityNotExistsError

/**
 * The participant has submissions, so it cannot be deleted.
 *
 * @property participantId the id of the participant.
 * @since 1.0.0-beta.1
 */
data class ParticipantHasSubmissionsError(val participantId: SingleRoleUserId) : DeleteParticipantsError

// endregion

// region AdministratorOperations

/**
 * Failure of creating an observer in a community owned by the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateObserverError : OperationError

/**
 * The observer name is empty or contains only whitespace.
 *
 * @since 1.0.0-beta.1
 */
data object ObserverNameBlankError : CreateObserverError, CreateSupervisorObserverError

/**
 * The observer name exceeds 255 Unicode code points.
 *
 * @property observerName the rejected name.
 * @since 1.0.0-beta.1
 */
data class ObserverNameTooLongError(val observerName: String) : CreateObserverError, CreateSupervisorObserverError

/**
 * No contests were selected for the observer.
 *
 * @since 1.0.0-beta.1
 */
data object ObserverContestsEmptyError : CreateObserverError, CreateSupervisorObserverError

/**
 * Failure of listing users available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUsersError : OperationError

/**
 * Failure of viewing one user available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUserError : OperationError

/**
 * Failure of listing the tasks created by a user available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUserTasksError : OperationError

/**
 * Failure of listing the contests created by a user available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUserContestsError : OperationError

/**
 * Failure of listing the classes created by a user available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUserClassesError : OperationError

/**
 * Failure of listing the competitions created by a user available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUserCompetitionsError : OperationError

/**
 * Failure of listing the judgment orders issued by a user available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUserJudgmentsError : OperationError

/**
 * Failure of listing the contests assigned to an observer available to the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewUserAssignedContestsError : OperationError

/**
 * Failure of creating or replacing the invite code of a community owned by the administrator for a role.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateCommunityInviteError : OperationError

/**
 * Failure of extending the invite code of a community owned by the administrator for a role.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ExtendCommunityInviteError : OperationError

/**
 * Failure of viewing the invite codes of a community owned by the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewCommunityInvitesError : OperationError

/**
 * Failure of replacing the expired invite code of a community for a role on behalf of its owning administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RefreshCommunityInviteError : OperationError

/**
 * Failure of listing the communities created by the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewCommunitiesError : OperationError

/**
 * Failure of creating a community owned by the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateCommunityError : OperationError

/**
 * Failure of changing the name and description of a community owned by the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EditCommunityError : OperationError

/**
 * Failure of making a user available to the administrator a member of their community in a role.
 *
 * @since 1.0.0-beta.1
 */
sealed interface GrantRoleError : OperationError

/**
 * Failure of removing a user from a community of the administrator in a role.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RemoveFromCommunityError : OperationError

/**
 * Failure of deleting an observer of a community of the administrator.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DeleteObserverError : OperationError

/**
 * Failure of listing the contests shared to a community owned by the administrator, from which an observer is assigned.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewCommunityContestsError : OperationError

/**
 * The user does not hold the administrator role.
 *
 * @since 1.0.0-beta.1
 */
data object MissedAdministratorRoleError :
    CreateObserverError,
    ViewUsersError,
    ViewUserError,
    ViewUserTasksError,
    ViewUserContestsError,
    ViewUserClassesError,
    ViewUserCompetitionsError,
    ViewUserJudgmentsError,
    ViewUserAssignedContestsError,
    CreateCommunityInviteError,
    ExtendCommunityInviteError,
    RefreshCommunityInviteError,
    ViewCommunityInvitesError,
    ViewCommunitiesError,
    CreateCommunityError,
    EditCommunityError,
    GrantRoleError,
    RemoveFromCommunityError,
    DeleteObserverError,
    ViewCommunityContestsError,
    MissedRequiredRoleError

/**
 * The community name is empty or contains only whitespace.
 *
 * @since 1.0.0-beta.1
 */
data object CommunityNameBlankError : CreateCommunityError, EditCommunityError, EditSupervisorCommunityError

/**
 * The community name exceeds 255 Unicode code points.
 *
 * @property communityName the rejected name.
 * @since 1.0.0-beta.1
 */
data class CommunityNameTooLongError(val communityName: String) : CreateCommunityError, EditCommunityError, EditSupervisorCommunityError

/**
 * The user holds a fixed role, so no role can be granted to them in a community.
 *
 * @property userId the id of the user with the fixed role.
 * @since 1.0.0-beta.1
 */
data class UserHasFixedRoleError(val userId: UserId) : GrantRoleError, AddToCommunityError, GrantAdministratorError

/**
 * The user does not exist.
 *
 * @property userId the id of the missing user.
 * @since 1.0.0-beta.1
 */
data class UserNotExistsError(val userId: UserId) :
    ViewUserError,
    ViewUserTasksError,
    ViewUserContestsError,
    ViewUserClassesError,
    ViewUserCompetitionsError,
    ViewUserJudgmentsError,
    ViewUserAssignedContestsError,
    GrantRoleError,
    RemoveFromCommunityError,
    DeleteObserverError,
    AddToCommunityError,
    GrantAdministratorError,
    ViewSupervisorUserError,
    RemoveSupervisorMemberError,
    ImpersonateError,
    EntityNotExistsError

/**
 * The user exists but is not available to the administrator.
 *
 * @property userId the id of the inaccessible user.
 * @since 1.0.0-beta.1
 */
data class UserAccessDeniedError(val userId: UserId) :
    ViewUserError,
    ViewUserTasksError,
    ViewUserContestsError,
    ViewUserClassesError,
    ViewUserCompetitionsError,
    ViewUserJudgmentsError,
    ViewUserAssignedContestsError,
    GrantRoleError,
    DeleteObserverError,
    AccessDeniedError

/**
 * The role cannot be granted by an administrator.
 *
 * @property role the rejected role.
 * @since 1.0.0-beta.1
 */
data class RoleNotGrantableError(val role: CommunityRole) : GrantRoleError

/**
 * The role cannot be removed by an administrator.
 *
 * @property role the rejected role.
 * @since 1.0.0-beta.1
 */
data class RoleNotRemovableError(val role: CommunityRole) : RemoveFromCommunityError, RemoveSupervisorMemberError

/**
 * The community is the public community, from which no user is removed.
 *
 * @property communityId the id of the public community.
 * @since 1.0.0-beta.1
 */
data class CommunityIsPublicError(val communityId: CommunityId) : RemoveFromCommunityError, RemoveSupervisorMemberError

/**
 * The user is not a member of the community in the role.
 *
 * @property userId the id of the user.
 * @property communityId the id of the community.
 * @property role the role the user is not a member in.
 * @since 1.0.0-beta.1
 */
data class UserNotCommunityMemberError(val userId: UserId, val communityId: CommunityId, val role: CommunityRole) :
    RemoveFromCommunityError,
    RemoveSupervisorMemberError

// endregion

// region SupervisorOperations

/**
 * Failure of listing users available to a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUsersError : OperationError

/**
 * Failure of reading developer tasks as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUserTasksError : OperationError

/**
 * Failure of reading developer contests as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUserContestsError : OperationError

/**
 * Failure of reading manager classes as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUserClassesError : OperationError

/**
 * Failure of reading manager competitions as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUserCompetitionsError : OperationError

/**
 * Failure of reading judge orders as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUserJudgmentsError : OperationError

/**
 * Failure of reading observer assignments as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUserAssignedContestsError : OperationError

/**
 * Failure of viewing one non-supervisor user.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorUserError :
    ViewSupervisorUserTasksError,
    ViewSupervisorUserContestsError,
    ViewSupervisorUserClassesError,
    ViewSupervisorUserCompetitionsError,
    ViewSupervisorUserJudgmentsError,
    ViewSupervisorUserAssignedContestsError

/**
 * Failure of listing all communities for a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorCommunitiesError : OperationError

/**
 * Failure of viewing one community for a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorCommunityError : OperationError

/**
 * Failure of adding a multiple-role user to any community in a non-fixed role.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AddToCommunityError : OperationError

/**
 * Failure of granting the administrator role to a multiple-role user without choosing a community.
 *
 * @since 1.0.0-beta.1
 */
sealed interface GrantAdministratorError : OperationError

/**
 * Failure of editing a community as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EditSupervisorCommunityError : OperationError

/**
 * Failure of removing a membership as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RemoveSupervisorMemberError : OperationError

/**
 * Failure of creating an observer as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateSupervisorObserverError : OperationError

/**
 * Failure of finding contests for a new observer as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorCommunityContestsError : OperationError

/**
 * Failure of reading community invites as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSupervisorCommunityInvitesError : OperationError

/**
 * Failure of replacing a community invite as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateSupervisorCommunityInviteError : OperationError

/**
 * Failure of extending a community invite as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ExtendSupervisorCommunityInviteError : OperationError

/**
 * Failure of refreshing a community invite as a supervisor.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RefreshSupervisorCommunityInviteError : OperationError

/**
 * Failure of signing a supervisor in as another user.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ImpersonateError : OperationError

/**
 * The actor is not a supervisor.
 *
 * @since 1.0.0-beta.1
 */
data object MissedSupervisorRoleError :
    ViewSupervisorUsersError,
    ViewSupervisorUserError,
    ViewSupervisorCommunitiesError,
    ViewSupervisorCommunityError,
    AddToCommunityError,
    GrantAdministratorError,
    EditSupervisorCommunityError,
    RemoveSupervisorMemberError,
    CreateSupervisorObserverError,
    ViewSupervisorCommunityContestsError,
    ViewSupervisorCommunityInvitesError,
    CreateSupervisorCommunityInviteError,
    ExtendSupervisorCommunityInviteError,
    RefreshSupervisorCommunityInviteError,
    ImpersonateError,
    MissedRequiredRoleError

/**
 * Administrator role cannot be assigned in the public community.
 *
 * @property communityId the public community where the role was requested.
 * @since 1.0.0-beta.1
 */
data class AdministratorInPublicCommunityError(val communityId: CommunityId) : AddToCommunityError

/**
 * The user is the technical owner of the public community, whom nobody signs in as.
 *
 * @property userId the id of the technical owner.
 * @since 1.0.0-beta.1
 */
data class ImpersonationDeniedError(val userId: UserId) :
    ImpersonateError,
    AccessDeniedError

// endregion

// region MultipleRoleUserOperations

/**
 * Failure of joining a community by an invite code.
 *
 * @since 1.0.0-beta.1
 */
sealed interface JoinCommunityError : OperationError

/**
 * The entered code matches no community invite code.
 *
 * @property inviteCode the code as entered by the user.
 * @since 1.0.0-beta.1
 */
data class CommunityInviteCodeNotValidError(val inviteCode: String) : JoinCommunityError

/**
 * The community invite code matching the entered code has expired.
 *
 * @property inviteCode the code as entered by the user.
 * @since 1.0.0-beta.1
 */
data class CommunityInviteCodeExpiredError(val inviteCode: String) : JoinCommunityError

/**
 * Failure of viewing the profile. It has no cases: a user with a fixed role is excluded by the parameter type.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewProfileError : OperationError

// endregion

// region JudgeOperations

/**
 * Failure of listing verdicts available to the judge.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewResultsError : OperationError

/**
 * Failure of issuing a judgment order for a submission.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ChangeVerdictError : OperationError

/**
 * Failure of viewing a submission available to the judge.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewSolutionError : OperationError

/**
 * Failure of downloading the solution file of a submission available to the judge.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadSolutionError : OperationError

/**
 * Failure of downloading the grading logs of a test of a submission available to the judge.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadLogsError : OperationError

/**
 * Failure of downloading the recording of a test of a submission available to the judge.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadRecordingError : OperationError

/**
 * The user does not hold the judge role.
 *
 * @since 1.0.0-beta.1
 */
data object MissedJudgeRoleError :
    ViewResultsError,
    ChangeVerdictError,
    ViewSolutionError,
    DownloadSolutionError,
    DownloadLogsError,
    DownloadRecordingError,
    MissedRequiredRoleError

/**
 * The submission does not exist.
 *
 * @property submissionId the id of the missing submission.
 * @since 1.0.0-beta.1
 */
data class SubmissionNotExistsError(val submissionId: SubmissionId) :
    DownloadParticipantSolutionError,
    DownloadStudentSolutionError,
    ChangeVerdictError,
    ViewSolutionError,
    DownloadSolutionError,
    DownloadLogsError,
    DownloadRecordingError,
    EntityNotExistsError

/**
 * The submission is outside the requested study context or the current judge cannot access its author.
 *
 * @property submissionId the id of the inaccessible submission.
 * @since 1.0.0-beta.1
 */
data class SubmissionAccessDeniedError(val submissionId: SubmissionId) :
    DownloadParticipantSolutionError,
    DownloadStudentSolutionError,
    ChangeVerdictError,
    ViewSolutionError,
    DownloadSolutionError,
    DownloadLogsError,
    DownloadRecordingError,
    AccessDeniedError

/**
 * The submission was created to test a developer solution, so it cannot receive a judgment order.
 *
 * @property submissionId the id of the developer solution test submission.
 * @since 1.0.0-beta.1
 */
data class SubmissionIsDeveloperSolutionTestError(val submissionId: SubmissionId) : ChangeVerdictError

/**
 * The submission has no successful automatic grading result.
 *
 * @property submissionId the id of the submission.
 * @since 1.0.0-beta.1
 */
data class SubmissionNotSuccessfullyGradedError(val submissionId: SubmissionId) :
    ChangeVerdictError,
    DownloadLogsError,
    DownloadRecordingError

/**
 * The requested judgment score is negative.
 *
 * @property score the invalid score.
 * @since 1.0.0-beta.1
 */
data class NegativeJudgmentScoreError(val score: Score) : ChangeVerdictError

/**
 * The judgment reason is empty or contains only whitespace.
 *
 * @since 1.0.0-beta.1
 */
data object BlankJudgmentReasonError : ChangeVerdictError

/**
 * The test is not part of the successful verdict of the submission.
 *
 * @property submissionId the id of the submission.
 * @property testId the id of the test.
 * @since 1.0.0-beta.1
 */
data class TestNotInVerdictError(val submissionId: SubmissionId, val testId: TestId) : DownloadLogsError, DownloadRecordingError

/**
 * The run of the submission on the test produced no recording.
 *
 * @property submissionId the id of the submission.
 * @property testId the id of the test.
 * @since 1.0.0-beta.1
 */
data class RecordingNotExistsError(val submissionId: SubmissionId, val testId: TestId) :
    DownloadRecordingError,
    EntityNotExistsError

// endregion

// region DeveloperOperations

/**
 * The task name exceeds the Unicode code point limit.
 *
 * @property taskName the rejected task name.
 * @since 1.0.0-beta.1
 */
data class TaskNameTooLongError(val taskName: String) : CreateTaskError, EditTaskInfoError

/**
 * The contest name exceeds the Unicode code point limit.
 *
 * @property contestName the rejected contest name.
 * @since 1.0.0-beta.1
 */
data class ContestNameTooLongError(val contestName: String) : CreateContestError, EditContestError

/**
 * The TRIK Studio version is not registered with this tag and version number: it was deleted or its tag was changed.
 *
 * @property trikStudioVersion the unregistered version.
 * @since 1.0.0-beta.1
 */
data class TrikStudioVersionNotRegisteredError(val trikStudioVersion: TrikStudioVersion) :
    EntityNotExistsError,
    CreateContestError,
    EditTaskInfoError

/**
 * The resource name exceeds the Unicode code point limit.
 *
 * @property resourceName the rejected resource name.
 * @since 1.0.0-beta.1
 */
data class ResourceNameTooLongError(val resourceName: String) :
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    UpdateStatementError,
    UpdateExerciseError,
    UpdateTestError,
    UpdateDeveloperSolutionError

/**
 * The uploaded file name exceeds the Unicode code point limit.
 *
 * @property uploadedFilename the rejected uploaded file name.
 * @since 1.0.0-beta.1
 */
data class UploadedFileNameTooLongError(val uploadedFilename: String) :
    AddStatementError,
    AddExerciseError,
    AddTestError,
    AddDeveloperSolutionError,
    UpdateStatementError,
    UpdateExerciseError,
    UpdateTestError,
    UpdateDeveloperSolutionError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * Expected failure of requesting testing of an owned task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface TestTaskError : OperationError

/**
 * Expected failure of viewing the testing history of an owned task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewTaskValidationRequestsError : OperationError

/**
 * Expected failure of committing the working revision of an owned task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CommitTaskError : OperationError

/**
 * Expected failure of counting the submissions a commit of an owned task would send to grading again.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CountRegradeSubmissionsError : OperationError

/**
 * Expected failure of viewing the regrades of an owned task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewRegradesError : OperationError

/**
 * The working revision has no successfully completed validation request with a matching snapshot.
 *
 * @property taskId the identifier of the task being committed.
 * @since 1.0.0-beta.1
 */
data class TaskNotTestedError(val taskId: TaskId) : CommitTaskError

/**
 * The working task has no attached statement.
 *
 * @property taskId the identifier of the tested or committed task.
 * @since 1.0.0-beta.1
 */
data class TaskTestingNoStatementError(val taskId: TaskId) : TestTaskError, CommitTaskError

/**
 * The working task has no exercise for an author solution's language.
 *
 * @property taskId the identifier of the tested or committed task.
 * @property language the author solution language without an exercise.
 * @since 1.0.0-beta.1
 */
data class TaskTestingNoExerciseForLanguageError(val taskId: TaskId, val language: TrikSupportedLanguage) : TestTaskError, CommitTaskError

/**
 * The working task has no attached polygons.
 *
 * @property taskId the tested task identifier.
 * @since 1.0.0-beta.1
 */
data class TaskTestingNoPolygonsError(val taskId: TaskId) : TestTaskError

/**
 * The working task has no attached author solutions.
 *
 * @property taskId the tested task identifier.
 * @since 1.0.0-beta.1
 */
data class TaskTestingNoDeveloperSolutionsError(val taskId: TaskId) : TestTaskError

/**
 * The working task supports no TRIK Studio version.
 *
 * @property taskId the tested task identifier.
 * @since 1.0.0-beta.1
 */
data class TaskTestingNoTrikStudioVersionsError(val taskId: TaskId) : TestTaskError

/**
 * Failure of listing contests available to the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewContestsError : OperationError

/**
 * Failure of viewing a contest available to the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewContestError : OperationError

/**
 * Failure of reverting a task owned by the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RevertTaskError : OperationError

/**
 * Failure of listing tasks available to the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewTasksError : OperationError

/**
 * Failure of viewing a task owned by the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewTaskError : OperationError

/**
 * Failure of editing information of a task owned by the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EditTaskInfoError : OperationError

/**
 * Failure of listing the developer's resources.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewResourcesError : OperationError

/**
 * Failure of viewing a resource and its existing versions.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewResourceError : OperationError

/**
 * Failure of obtaining the file reference of a resource version.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadResourceVersionError : OperationError

/**
 * Failure of listing the TRIK Studio versions registered in the system.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewTrikStudioVersionsError : OperationError

/**
 * Failure of updating a statement uploaded to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface UpdateStatementError : OperationError

/**
 * Failure of updating an exercise uploaded to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface UpdateExerciseError : OperationError

/**
 * Failure of updating a polygon uploaded to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface UpdateTestError : OperationError

/**
 * Failure of updating a developer solution uploaded to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface UpdateDeveloperSolutionError : OperationError

/**
 * Failure of uploading a new statement to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AddStatementError : OperationError

/**
 * Failure of uploading a new exercise to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AddExerciseError : OperationError

/**
 * Failure of uploading a new polygon to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AddTestError : OperationError

/**
 * Failure of uploading a new developer solution to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AddDeveloperSolutionError : OperationError

/**
 * Failure of creating a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateTaskError : OperationError

/**
 * Failure of creating a contest.
 *
 * @since 1.0.0-beta.1
 */
sealed interface CreateContestError : OperationError

/**
 * Failure of editing a contest owned by the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EditContestError : OperationError

/**
 * Failure of sharing a contest to communities.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ShareContestError : OperationError

/**
 * Failure of attaching a task to a contest.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AttachTaskError : OperationError

/**
 * Failure of detaching a task from a contest.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DetachTaskError : OperationError

/**
 * Failure of deleting a contest owned by the developer.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DeleteContestError : OperationError

/**
 * Failure of attaching a statement to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AttachStatementError : OperationError

/**
 * Failure of attaching a exercise to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AttachExerciseError : OperationError

/**
 * Failure of attaching a test to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AttachTestError : OperationError

/**
 * Failure of attaching a developersolution to a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AttachDeveloperSolutionError : OperationError

/**
 * Failure of sharing a task to communities.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ShareTaskError : OperationError

/**
 * Failure of detaching a statement version from a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DetachStatementError : OperationError

/**
 * Failure of detaching an exercise version from a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DetachExerciseError : OperationError

/**
 * Failure of detaching a polygon version from a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DetachTestError : OperationError

/**
 * Failure of detaching a developer solution version from a task.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DetachDeveloperSolutionError : OperationError
// endregion

// region Errors

/**
 * The user does not have the developer role.
 *
 * @since 1.0.0-beta.1
 */
data object MissedDeveloperRoleError :
    TestTaskError,
    ViewTaskValidationRequestsError,
    CountRegradeSubmissionsError,
    ViewRegradesError,
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
    AttachDeveloperSolutionError,
    ViewTrikStudioVersionsError

/**
 * The contest end is specified without its start.
 *
 * @property endsAt the end moment supplied without a start.
 * @since 1.0.0-beta.1
 */
data class ContestEndWithoutStartError(val endsAt: Instant) : CreateContestError, EditContestError

/**
 * The contest end is not later than its start.
 *
 * @property startsAt the requested start moment.
 * @property endsAt the requested end moment.
 * @since 1.0.0-beta.1
 */
data class ContestEndNotAfterStartError(val startsAt: Instant, val endsAt: Instant) : CreateContestError, EditContestError

/**
 * The individual time limit is zero or negative.
 *
 * @property attemptDuration the nonpositive individual limit.
 * @since 1.0.0-beta.1
 */
data class NonPositiveAttemptDurationError(val attemptDuration: Duration) : CreateContestError

/**
 * The individual time limit exceeds the contest interval.
 *
 * @property attemptDuration the requested individual limit.
 * @property contestDuration the available contest interval.
 * @since 1.0.0-beta.1
 */
data class AttemptDurationExceedsContestDurationError(
    val attemptDuration: Duration,
    val contestDuration: Duration,
) : CreateContestError, EditContestError

/**
 * The contest does not exist.
 *
 * @property contestId the id of the missing contest.
 * @since 1.0.0-beta.1
 */
data class ContestNotExistsError(
    val contestId: ContestId,
) : CreateObserverError,
    CreateSupervisorObserverError,
    EntityNotExistsError,
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
    DownloadParticipantSolutionError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
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
 * @since 1.0.0-beta.1
 */
data class ContestAccessDeniedError(
    val contestId: ContestId,
) : CreateObserverError,
    CreateSupervisorObserverError,
    AccessDeniedError,
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
    DownloadParticipantSolutionError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
    SendParticipantSolutionError,
    SendStudentSolutionError,
    AddClassContestError,
    AddCompetitionContestError

/**
 * The contest is shared to at least one community.
 *
 * @property contestId the id of the shared contest.
 * @since 1.0.0-beta.1
 */
data class ContestAlreadySharedError(val contestId: ContestId) : EditContestError, AttachTaskError, DetachTaskError, DeleteContestError

/**
 * The task does not exist.
 *
 * @property taskId the id of the missing task.
 * @since 1.0.0-beta.1
 */
data class TaskNotExistsError(val taskId: TaskId) :
    TestTaskError,
    ViewTaskValidationRequestsError,
    CountRegradeSubmissionsError,
    ViewRegradesError,
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
    DownloadParticipantSolutionError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The statement does not exist.
 *
 * @property statementId the id of the missing statement.
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class StatementNotUploadedToTaskError(val taskId: TaskId, val statementId: StatementId) :
    DetachStatementError,
    UpdateStatementError,
    AttachStatementError,
    ResourceAccessError

/**
 * The task already has a statement.
 *
 * @since 1.0.0-beta.1
 */
data object TaskAlreadyHasStatementError : AttachStatementError

/**
 * The community does not exist.
 *
 * @property communityId the id of the missing community.
 * @since 1.0.0-beta.1
 */
data class CommunityNotExistsError(val communityId: CommunityId) :
    CreateObserverError,
    EntityNotExistsError,
    ShareTaskError,
    ShareContestError,
    CreateCommunityInviteError,
    ExtendCommunityInviteError,
    ViewCommunityInvitesError,
    RefreshCommunityInviteError,
    GrantRoleError,
    RemoveFromCommunityError,
    ViewCommunityContestsError,
    EditCommunityError,
    AddToCommunityError,
    ViewSupervisorCommunityError,
    EditSupervisorCommunityError,
    RemoveSupervisorMemberError,
    CreateSupervisorObserverError,
    ViewSupervisorCommunityContestsError,
    ViewSupervisorCommunityInvitesError,
    CreateSupervisorCommunityInviteError,
    ExtendSupervisorCommunityInviteError,
    RefreshSupervisorCommunityInviteError

/**
 * The user lacks the access to the task required by the operation.
 *
 * @property taskId the id of the task.
 * @since 1.0.0-beta.1
 */
data class TaskAccessDeniedError(val taskId: TaskId) :
    TestTaskError,
    ViewTaskValidationRequestsError,
    CountRegradeSubmissionsError,
    ViewRegradesError,
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
    DownloadParticipantSolutionError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The user is not a member of the community, or it belongs to another user.
 *
 * @property communityId the id of the community.
 * @since 1.0.0-beta.1
 */
data class CommunityAccessDeniedError(val communityId: CommunityId) :
    CreateObserverError,
    AccessDeniedError,
    ShareTaskError,
    ShareContestError,
    CreateCommunityInviteError,
    ExtendCommunityInviteError,
    ViewCommunityInvitesError,
    RefreshCommunityInviteError,
    GrantRoleError,
    RemoveFromCommunityError,
    ViewCommunityContestsError,
    EditCommunityError

/**
 * The task has no committed version.
 *
 * @property taskId the id of the task.
 * @since 1.0.0-beta.1
 */
data class TaskNotCommittedError(val taskId: TaskId) : ShareTaskError, AttachTaskError, RevertTaskError

/**
 * The checked revision of the task does not support the contest's TRIK Studio version: the last committed revision
 * when the task is attached to the contest, the working revision when the task is tested or committed.
 *
 * @property taskId the id of the incompatible task.
 * @property trikStudioVersion the version required by the contest.
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class TaskAlreadyAttachedToContestError(val contestId: ContestId, val taskId: TaskId) : AttachTaskError

/**
 * The task is not attached to the contest.
 *
 * @property contestId the id of the contest.
 * @property taskId the id of the task absent from the contest.
 * @since 1.0.0-beta.1
 */
data class TaskNotAttachedToContestError(val contestId: ContestId, val taskId: TaskId) : DetachTaskError

/**
 * The task has no uncommitted changes to test, commit or revert.
 *
 * @property taskId the id of the committed task.
 * @since 1.0.0-beta.1
 */
data class TaskAlreadyCommittedError(val taskId: TaskId) : RevertTaskError, TestTaskError, CommitTaskError
// endregion

/**
 * The requested statement version is no longer the latest version in its chain.
 *
 * @property statementId the id of the requested version.
 * @since 1.0.0-beta.1
 */
data class StatementVersionNotLatestError(val statementId: StatementId) : AttachStatementError, UpdateStatementError

/**
 * The exercise does not exist.
 *
 * @property exerciseId the id of the missing resource.
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class ExerciseVersionNotLatestError(val exerciseId: ExerciseId) : AttachExerciseError, UpdateExerciseError

/**
 * The test does not exist.
 *
 * @property testId the id of the missing resource.
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class TestVersionNotLatestError(val testId: TestId) : AttachTestError, UpdateTestError

/**
 * The developer solution does not exist.
 *
 * @property developerSolutionId the id of the missing resource.
 * @since 1.0.0-beta.1
 */
data class DeveloperSolutionNotExistsError(
    val developerSolutionId: DeveloperSolutionId,
) : EntityNotExistsError, AttachDeveloperSolutionError, UpdateDeveloperSolutionError, DetachDeveloperSolutionError

/**
 * The developer solution's version chain is not uploaded to the task.
 *
 * @property taskId the id of the task.
 * @property developerSolutionId the id of the resource outside the task's uploaded chains.
 * @since 1.0.0-beta.1
 */
data class DeveloperSolutionNotUploadedToTaskError(
    val taskId: TaskId,
    val developerSolutionId: DeveloperSolutionId,
) : AttachDeveloperSolutionError, UpdateDeveloperSolutionError, DetachDeveloperSolutionError, ResourceAccessError

/**
 * The requested developer solution version is no longer the latest version in its chain.
 *
 * @property developerSolutionId the id of the requested version.
 * @since 1.0.0-beta.1
 */
data class DeveloperSolutionVersionNotLatestError(val developerSolutionId: DeveloperSolutionId) :
    AttachDeveloperSolutionError,
    UpdateDeveloperSolutionError

/**
 * A version from the resource chain is already attached to the editable task revision.
 *
 * @property taskId the id of the task.
 * @property versionBucket the already attached resource chain.
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class ResourceNotExistsError(val versionBucket: VersionBucket) : EntityNotExistsError, ViewResourceError, DownloadResourceVersionError

/**
 * The requested resource chain is not uploaded to the task.
 *
 * @property taskId the task identifier.
 * @property versionBucket the resource chain outside the task.
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class ResourceVersionNotExistsError(val versionBucket: VersionBucket, val versionId: DomainId) :
    EntityNotExistsError,
    DownloadResourceVersionError

// region ObserverOperations

/**
 * Failure of listing the observer's assigned contests.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewObserverContestsError : OperationError

/**
 * Failure of downloading results of all competitions containing an observer's assigned contests.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadObserverResultError : OperationError

/**
 * The user is not an observer.
 *
 * @since 1.0.0-beta.1
 */
data object MissedObserverRoleError : MissedRequiredRoleError, ViewObserverContestsError, DownloadObserverResultError

// endregion

// region StudyOperations

/**
 * Failure of viewing a contest in the participant's competition.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewParticipantContestError : OperationError

/**
 * Failure of viewing a contest in the selected class.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewStudentContestError : OperationError

/**
 * Failure of listing contests of the participant.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewParticipantContestsError : OperationError

/**
 * Failure of listing contests of the selected class.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewStudentContestsError : OperationError

/**
 * Failure of listing classes of the student.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewStudentClassesError : OperationError

/**
 * Failure of entering a participant contest.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EnterParticipantContestError : OperationError

/**
 * Failure of entering a student contest.
 *
 * @since 1.0.0-beta.1
 */
sealed interface EnterStudentContestError : OperationError

/**
 * Failure of viewing a task of a contest in the participant's competition.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewParticipantTaskError : OperationError

/**
 * Failure of viewing a task of a contest in the selected class.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ViewStudentTaskError : OperationError

/**
 * Failure of downloading a statement or an exercise of a task in the participant's competition.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadParticipantTaskResourceError : OperationError

/**
 * Failure of downloading an owned solution in the participant's study context.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadParticipantSolutionError : OperationError

/**
 * Failure of downloading a statement or an exercise of a task in the selected class.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadStudentTaskResourceError : OperationError

/**
 * Failure of downloading an owned solution in the student's study context.
 *
 * @since 1.0.0-beta.1
 */
sealed interface DownloadStudentSolutionError : OperationError

/**
 * Failure of sending a solution for a task of a contest in the participant's competition.
 *
 * @since 1.0.0-beta.1
 */
sealed interface SendParticipantSolutionError : OperationError

/**
 * Failure of sending a solution for a task of a contest in the selected class.
 *
 * @since 1.0.0-beta.1
 */
sealed interface SendStudentSolutionError : OperationError

/**
 * Failure of joining a class by an invite code.
 *
 * @since 1.0.0-beta.1
 */
sealed interface JoinClassError : OperationError

/**
 * The user is not a participant.
 *
 * @since 1.0.0-beta.1
 */
data object MissedParticipantRoleError :
    MissedRequiredRoleError,
    ViewParticipantContestsError,
    EnterParticipantContestError,
    ViewParticipantContestError,
    ViewParticipantTaskError,
    DownloadParticipantTaskResourceError,
    DownloadParticipantSolutionError,
    SendParticipantSolutionError

/**
 * The user does not hold the student role.
 *
 * @since 1.0.0-beta.1
 */
data object MissedStudentRoleError :
    MissedRequiredRoleError,
    ViewStudentContestsError,
    ViewStudentClassesError,
    EnterStudentContestError,
    ViewStudentContestError,
    ViewStudentTaskError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
    SendStudentSolutionError,
    JoinClassError

/**
 * The entered code matches no class invite code.
 *
 * @property inviteCode the code as entered by the user.
 * @since 1.0.0-beta.1
 */
data class ClassInviteCodeNotValidError(val inviteCode: String) : JoinClassError

/**
 * The class invite code matching the entered code has expired.
 *
 * @property inviteCode the code as entered by the user.
 * @since 1.0.0-beta.1
 */
data class ClassInviteCodeExpiredError(val inviteCode: String) : JoinClassError

/**
 * The first entry precedes the contest start.
 *
 * @property contestId the requested contest.
 * @property startsAt the interval boundary.
 * @since 1.0.0-beta.1
 */
data class ContestNotStartedError(val contestId: ContestId, val startsAt: Instant) :
    EnterParticipantContestError, EnterStudentContestError

/**
 * The first entry or a solution submission is at or after the contest end.
 *
 * @property contestId the requested contest.
 * @property endsAt the interval boundary.
 * @since 1.0.0-beta.1
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
 * @since 1.0.0-beta.1
 */
data class ContestNotEnteredError(val contestId: ContestId) :
    ViewParticipantTaskError,
    ViewStudentTaskError,
    DownloadParticipantTaskResourceError,
    DownloadParticipantSolutionError,
    DownloadStudentTaskResourceError,
    DownloadStudentSolutionError,
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * A solution submission is at or after the first entry in the selected context plus the contest attempt duration.
 *
 * @property contestId the requested contest.
 * @property expiresAt the end of the individual attempt.
 * @since 1.0.0-beta.1
 */
data class ContestAttemptExpiredError(val contestId: ContestId, val expiresAt: Instant) :
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The last committed revision of the task has no developer solution in the language of the sent solution.
 *
 * @property taskId the requested task.
 * @property language the language of the sent solution.
 * @since 1.0.0-beta.1
 */
data class SolutionLanguageNotAllowedError(val taskId: TaskId, val language: TrikSupportedLanguage) :
    SendParticipantSolutionError,
    SendStudentSolutionError

/**
 * The requested version is not a statement or an exercise of the last committed revision of the task.
 *
 * @property taskId the requested task.
 * @property resourceId the requested version identifier.
 * @since 1.0.0-beta.1
 */
data class ResourceNotInCommittedTaskError(val taskId: TaskId, val resourceId: DomainId) :
    ResourceAccessError,
    DownloadParticipantTaskResourceError,
    DownloadStudentTaskResourceError

// endregion

// region UserOperations

/**
 * Failure of signing in to the system by the access code of a fixed-role user.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AuthenticateError : OperationError

/**
 * The entered access code is not assigned to any fixed-role user.
 *
 * @since 1.0.0-beta.1
 */
data object InvalidAccessTokenError : AuthenticateError

/**
 * Failure of signing in to the system by the e-mail address and the password of a user without a fixed role.
 *
 * @since 1.0.0-beta.1
 */
sealed interface AuthenticateByPasswordError : OperationError

/**
 * The e-mail address is bound to no user who can sign in, or the password differs from the password of that user.
 *
 * @since 1.0.0-beta.1
 */
data object InvalidCredentialsError : AuthenticateByPasswordError

/**
 * The new password is shorter than 8 or longer than 64 characters, has a character other than a Latin letter, a digit
 * and `!@#$%^&*()-_=+.,?`, or lacks a lowercase letter, an uppercase letter, a digit or one of those symbols.
 *
 * @since 1.0.0-beta.1
 */
data object InvalidPasswordError : ConfirmRegistrationError, ConfirmAccessRestorationError, ChangePasswordError

/**
 * Failure of requesting self-registration with an e-mail address.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RequestRegistrationError : OperationError

/**
 * Failure of confirming self-registration by the code sent to the e-mail address with a nickname and a role.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ConfirmRegistrationError : OperationError

/**
 * The nickname is empty after trimming or longer than 512 Unicode code points.
 *
 * @since 1.0.0-beta.1
 */
data object InvalidUserNameError : ConfirmRegistrationError

/**
 * The e-mail address is longer than 255 Unicode code points or lacks exactly one `@` with non-empty parts around it.
 *
 * @since 1.0.0-beta.1
 */
data object InvalidEmailError : RequestRegistrationError, RequestEmailChangeError

/**
 * The e-mail address is already bound to a user.
 *
 * @since 1.0.0-beta.1
 */
data object EmailAlreadyBoundError :
    RequestRegistrationError,
    ConfirmRegistrationError,
    RequestEmailChangeError,
    ConfirmEmailChangeError

/**
 * The registration request does not exist.
 *
 * @property registrationRequestId the id of the missing request.
 * @since 1.0.0-beta.1
 */
data class RegistrationRequestNotExistsError(val registrationRequestId: RegistrationRequestId) :
    ConfirmRegistrationError,
    EntityNotExistsError

/**
 * The confirmation code has expired.
 *
 * @since 1.0.0-beta.1
 */
data object ConfirmationCodeExpiredError : ConfirmRegistrationError, ConfirmEmailChangeError, ConfirmAccessRestorationError

/**
 * No attempts to enter the confirmation code are left.
 *
 * @since 1.0.0-beta.1
 */
data object ConfirmationAttemptsExhaustedError : ConfirmRegistrationError, ConfirmEmailChangeError, ConfirmAccessRestorationError

/**
 * The entered confirmation code differs from the sent one.
 *
 * @since 1.0.0-beta.1
 */
data object InvalidConfirmationCodeError : ConfirmRegistrationError, ConfirmEmailChangeError, ConfirmAccessRestorationError

// endregion

// region MultipleRoleUserOperations

/**
 * Failure of requesting a change of the user's e-mail address.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RequestEmailChangeError : OperationError

/**
 * Failure of confirming the new e-mail address of the user by the code sent to it.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ConfirmEmailChangeError : OperationError

/**
 * The new e-mail address equals the current e-mail address of the user.
 *
 * @since 1.0.0-beta.1
 */
data object EmailUnchangedError : RequestEmailChangeError

/**
 * The user has no e-mail change request.
 *
 * @since 1.0.0-beta.1
 */
data object EmailChangeRequestNotExistsError : ConfirmEmailChangeError, EntityNotExistsError

/**
 * Failure of requesting restoration of access by an e-mail address. It has no cases: the response does not disclose
 * whether the address is bound to a user.
 *
 * @since 1.0.0-beta.1
 */
sealed interface RequestAccessRestorationError : OperationError

/**
 * Failure of confirming restoration of access by the code sent to the e-mail address.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ConfirmAccessRestorationError : OperationError

/**
 * The e-mail address is bound to no user with an access restoration request.
 *
 * @since 1.0.0-beta.1
 */
data object AccessRestorationRequestNotExistsError : ConfirmAccessRestorationError, EntityNotExistsError

/**
 * Failure of changing the password of the user.
 *
 * @since 1.0.0-beta.1
 */
sealed interface ChangePasswordError : OperationError

/**
 * The entered current password differs from the password of the user.
 *
 * @since 1.0.0-beta.1
 */
data object InvalidCurrentPasswordError : ChangePasswordError

// endregion
