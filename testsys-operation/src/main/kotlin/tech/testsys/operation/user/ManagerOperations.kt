package tech.testsys.operation.user

import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.ContestFilter
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.contract.persistence.repository.TaskRepository
import tech.testsys.domain.contract.persistence.repository.UserRepository
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.DomainId
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.group.RawInviteCodeDependency
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Task
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.RawAccessTokenDependency
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.User
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.config.ClassInviteConfig
import tech.testsys.operation.config.CompetitionConfig
import tech.testsys.operation.error.AddClassContestError
import tech.testsys.operation.error.AddCompetitionContestError
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNameBlankError
import tech.testsys.operation.error.ClassNameTooLongError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionAccessDeniedError
import tech.testsys.operation.error.CompetitionNameBlankError
import tech.testsys.operation.error.CompetitionNameTooLongError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.CompetitionParticipantLimitExceededError
import tech.testsys.operation.error.CompetitionParticipantNotExistsError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestAlreadyAddedToClassError
import tech.testsys.operation.error.ContestAlreadyAddedToCompetitionError
import tech.testsys.operation.error.ContestNotAddedToClassError
import tech.testsys.operation.error.ContestNotAddedToCompetitionError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.CreateClassError
import tech.testsys.operation.error.CreateClassInviteError
import tech.testsys.operation.error.CreateCompetitionError
import tech.testsys.operation.error.CreateParticipantsError
import tech.testsys.operation.error.DeleteParticipantError
import tech.testsys.operation.error.DownloadParticipantsError
import tech.testsys.operation.error.EditClassError
import tech.testsys.operation.error.EditCompetitionError
import tech.testsys.operation.error.ExtendClassInviteError
import tech.testsys.operation.error.MissedManagerRoleError
import tech.testsys.operation.error.NonPositiveParticipantCountError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ParticipantHasSubmissionsError
import tech.testsys.operation.error.RefreshClassInviteError
import tech.testsys.operation.error.ViewAvailableContestsError
import tech.testsys.operation.error.ViewClassContestError
import tech.testsys.operation.error.ViewClassError
import tech.testsys.operation.error.ViewClassesError
import tech.testsys.operation.error.ViewCompetitionContestError
import tech.testsys.operation.error.ViewCompetitionError
import tech.testsys.operation.error.ViewCompetitionsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.generateInviteCode
import tech.testsys.operation.util.hasRole
import tech.testsys.operation.util.inviteExpiresAt
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.UUID

private const val MAX_CLASS_NAME_CODE_POINTS = 255
private const val MAX_COMPETITION_NAME_CODE_POINTS = 255

/**
 * Operations of a user with the [Manager] role.
 *
 * @since %CURRENT_VERSION%
 */
@OptIn(InternalOperationsApi::class)
class ManagerOperations(
    private val classRepository: ClassRepository,
    private val competitionRepository: CompetitionRepository,
    private val contestRepository: ContestRepository,
    private val submissionRepository: SubmissionRepository,
    private val participantRepository: ParticipantRepository,
    private val competitionConfig: CompetitionConfig,
    private val classInviteRepository: ClassInviteRepository,
    private val classInviteConfig: ClassInviteConfig,
    private val clock: Clock,
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val userRepository: UserRepository,
    private val taskRepository: TaskRepository,
) {

    private val random = SecureRandom()

    /**
     * Creates a class owned by [user] with unchanged [className], an empty description, no students or contests and a new invite code.
     * The name must be nonblank and contain at most 255 Unicode code points; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.createClass")
    fun createClass(user: MultipleRoleUser, className: String): OperationResult<Class, CreateClassError> =
        operation<Class, CreateClassError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            ensure(className.isNotBlank(), ClassNameBlankError)
            ensure(className.codePointCount(0, className.length) <= MAX_CLASS_NAME_CODE_POINTS) { ClassNameTooLongError(className) }

            val expiresAt = inviteExpiresAt(now = clock.instant(), ttl = classInviteConfig.ttl)
            val inviteData = classInviteData {
                code(generateInviteCode(random = random, previous = null), HashAlgorithm.Identity)
                this.expiresAt = expiresAt
            }
            val studyClass = classRepository.saveWithInvite(inviteData) { inviteId ->
                classData {
                    owner = user.id
                    name = className
                    description = ""
                    invite = inviteId
                }
            }
            return studyClass.asSuccess()
        }

    /**
     * Creates a competition owned by [user] with unchanged [competitionName], an empty description and no participants or contests.
     * The name must be nonblank and contain at most 255 Unicode code points; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.createCompetition")
    fun createCompetition(user: MultipleRoleUser, competitionName: String): OperationResult<Competition, CreateCompetitionError> =
        operation<Competition, CreateCompetitionError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            ensure(competitionName.isNotBlank(), CompetitionNameBlankError)
            ensure(competitionName.codePointCount(0, competitionName.length) <= MAX_COMPETITION_NAME_CODE_POINTS) {
                CompetitionNameTooLongError(competitionName)
            }

            val competitionData = competitionData {
                owner = user.id
                name = competitionName
                description = ""
            }

            val competition = competitionRepository.save(competitionData)
            return competition.asSuccess()
        }

    /**
     * Returns a [pagination] page matching [filter] of classes owned by [user], preserving stored state.
     * Missing manager role is an expected failure; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.viewClasses")
    fun viewClasses(
        user: MultipleRoleUser,
        pagination: Pagination,
        filter: ClassFilter = ClassFilter(),
    ): OperationResult<Page<Class>, ViewClassesError> = operation<Page<Class>, ViewClassesError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val classes = classRepository.findAvailableToManager(ownerId = user.id, pagination = pagination, filter = filter)
        return classes.asSuccess()
    }

    /**
     * Returns [classId] owned by [user] with its invite, its students with their last logins and its contests, both in
     * stored order. Missing role, class and access are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.viewClass")
    @RawInviteCodeDependency(reason = "Returns the class invite whose stored value is shown as the issued code.")
    fun viewClass(user: MultipleRoleUser, classId: ClassId): OperationResult<ClassDetails, ViewClassError> =
        operation<ClassDetails, ViewClassError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }

            val studentIds = studyClass.data.students.ids
            val students = multipleRoleUserRepository.findByIds(studentIds).inOrderOf(studentIds)
            val lastLogins = userRepository.findLastLogins(studentIds)
            return ClassDetails(
                studyClass = studyClass,
                invite = studyClass.data.invite.load(classInviteRepository),
                students = students.map { student -> student to lastLogins[student.id] },
                contests = contestRepository.findByIds(studyClass.data.contests.ids).inOrderOf(studyClass.data.contests.ids),
            ).asSuccess()
        }

    /**
     * Replaces the name and description of [classId] owned by [user] with unchanged [className] and [description]; other class
     * data is kept. Missing role, class, access and a blank or longer than 255 code points name are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.editClass")
    fun editClass(
        user: MultipleRoleUser,
        classId: ClassId,
        className: String,
        description: String,
    ): OperationResult<Class, EditClassError> = operation<Class, EditClassError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val studyClass = classRepository.findById(classId)
        ensure(studyClass != null) { ClassNotExistsError(classId) }
        ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }
        ensure(className.isNotBlank(), ClassNameBlankError)
        ensure(className.codePointCount(0, className.length) <= MAX_CLASS_NAME_CODE_POINTS) { ClassNameTooLongError(className) }

        val edited = studyClass.withData {
            name = className
            this.description = description
        }
        return classRepository.update(edited).asSuccess()
    }

    /**
     * Replaces the invite code of [classId] owned by [user] with a new code and a fresh expiration moment.
     * Missing role, class and access are expected failures; storage exceptions, including a code collision, propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.createInvite")
    @RawInviteCodeDependency(reason = "Returns the issued invite code from the stored value.")
    fun createClassInvite(user: MultipleRoleUser, classId: ClassId): OperationResult<ClassInvite, CreateClassInviteError> =
        operation<ClassInvite, CreateClassInviteError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }
            val expiresAt = inviteExpiresAt(now = clock.instant(), ttl = classInviteConfig.ttl)
            return replaceCode(invite = studyClass.data.invite.load(classInviteRepository), expiresAt = expiresAt).asSuccess()
        }

    /**
     * Restarts the validity period of the invite code of [classId] owned by [user] without changing the code,
     * including an expired code. Missing role, class and access are expected failures; storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.extendInvite")
    @RawInviteCodeDependency(reason = "Returns the issued invite code from the stored value.")
    fun extendClassInvite(user: MultipleRoleUser, classId: ClassId): OperationResult<ClassInvite, ExtendClassInviteError> =
        operation<ClassInvite, ExtendClassInviteError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }
            val existing = studyClass.data.invite.load(classInviteRepository)
            val expiresAt = inviteExpiresAt(now = clock.instant(), ttl = classInviteConfig.ttl)
            val extended = existing.withData { this.expiresAt = expiresAt }
            return classInviteRepository.update(extended).asSuccess()
        }

    /**
     * Replaces the expired invite code of [classId] owned by [user] with a new code and a fresh expiration moment;
     * a still valid code is returned unchanged. Missing role, class and access are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.createInvite")
    fun refreshClassInvite(user: MultipleRoleUser, classId: ClassId): OperationResult<ClassInvite, RefreshClassInviteError> =
        operation<ClassInvite, RefreshClassInviteError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }
            val existing = studyClass.data.invite.load(classInviteRepository)
            val now = clock.instant()
            if (now < existing.data.expiresAt) return existing.asSuccess()
            val expiresAt = inviteExpiresAt(now = now, ttl = classInviteConfig.ttl)
            return replaceCode(invite = existing, expiresAt = expiresAt).asSuccess()
        }

    /**
     * Returns a [pagination] page matching [filter] of competitions owned by [user], preserving stored state.
     * Missing manager role is an expected failure; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.viewCompetitions")
    fun viewCompetitions(
        user: MultipleRoleUser,
        pagination: Pagination,
        filter: CompetitionFilter = CompetitionFilter(),
    ): OperationResult<Page<Competition>, ViewCompetitionsError> = operation<Page<Competition>, ViewCompetitionsError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val competitions = competitionRepository.findAvailableToManager(ownerId = user.id, pagination = pagination, filter = filter)
        return competitions.asSuccess()
    }

    /**
     * Returns [competitionId] owned by [user] with its participants and their last logins and its contests, both in stored
     * order. Missing role, competition and access are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.viewCompetition")
    @RawAccessTokenDependency(reason = "Returns participants whose stored access codes are shown as the issued ones.")
    fun viewCompetition(user: MultipleRoleUser, competitionId: CompetitionId): OperationResult<CompetitionDetails, ViewCompetitionError> =
        operation<CompetitionDetails, ViewCompetitionError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }

            val participantIds = competition.data.participants.ids
            val participants = participantRepository.findByIds(participantIds).inOrderOf(participantIds)
            val lastLogins = userRepository.findLastLogins(participantIds)
            return CompetitionDetails(
                competition = competition,
                participants = participants.map { participant -> participant to lastLogins[participant.id] },
                contests = contestRepository.findByIds(competition.data.contests.ids).inOrderOf(competition.data.contests.ids),
            ).asSuccess()
        }

    /**
     * Replaces the name and description of [competitionId] owned by [user] with unchanged [competitionName] and [description];
     * other competition data is kept. Missing role, competition, access and a blank or too long name are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.editCompetition")
    fun editCompetition(
        user: MultipleRoleUser,
        competitionId: CompetitionId,
        competitionName: String,
        description: String,
    ): OperationResult<Competition, EditCompetitionError> = operation<Competition, EditCompetitionError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val competition = competitionRepository.findById(competitionId)
        ensure(competition != null) { CompetitionNotExistsError(competitionId) }
        ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }
        ensure(competitionName.isNotBlank(), CompetitionNameBlankError)
        ensure(competitionName.codePointCount(0, competitionName.length) <= MAX_COMPETITION_NAME_CODE_POINTS) {
            CompetitionNameTooLongError(competitionName)
        }

        val edited = competition.withData {
            name = competitionName
            this.description = description
        }
        return competitionRepository.update(edited).asSuccess()
    }

    /**
     * Returns the participants of [competitionId] owned by [user] in stored order, for the file that hands out their access
     * codes. Missing role, competition and access are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.downloadParticipants")
    @RawAccessTokenDependency(reason = "Returns participants whose stored access codes are written as the issued ones.")
    fun downloadParticipants(
        user: MultipleRoleUser,
        competitionId: CompetitionId,
    ): OperationResult<List<Participant>, DownloadParticipantsError> = operation<List<Participant>, DownloadParticipantsError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val competition = competitionRepository.findById(competitionId)
        ensure(competition != null) { CompetitionNotExistsError(competitionId) }
        ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }

        val participantIds = competition.data.participants.ids
        return participantRepository.findByIds(participantIds).inOrderOf(participantIds).asSuccess()
    }

    /**
     * Deletes [participantId] of [competitionId] owned by [user] together with its contest entries and returns it as it was.
     * Missing role, competition, access, participant and existing submissions are expected failures; storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.deleteParticipant")
    fun deleteParticipant(
        user: MultipleRoleUser,
        competitionId: CompetitionId,
        participantId: SingleRoleUserId,
    ): OperationResult<Participant, DeleteParticipantError> = operation<Participant, DeleteParticipantError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val competition = competitionRepository.findById(competitionId)
        ensure(competition != null) { CompetitionNotExistsError(competitionId) }
        ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }
        val participant = participantRepository.findById(participantId)?.takeIf { found -> found.data.competition.id == competitionId }
        ensure(participant != null) {
            CompetitionParticipantNotExistsError(competitionId = competitionId, participantId = participantId)
        }
        val submissions = submissionRepository.countGrading(
            authorIds = setOf(participantId),
            contestIds = competition.data.contests.ids.toSet(),
        )
        ensure(submissions.submissions == 0L) { ParticipantHasSubmissionsError(participantId) }

        participantRepository.removeById(participantId)
        return participant.asSuccess()
    }

    /**
     * Returns a [pagination] page of contests shared to the communities of the [Manager] role of [user] whose name contains
     * [name] ignoring case, or all of them without one. Missing manager role is an expected failure.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.addContest")
    fun viewAvailableContests(
        user: MultipleRoleUser,
        pagination: Pagination,
        name: String? = null,
    ): OperationResult<Page<Contest>, ViewAvailableContestsError> = operation<Page<Contest>, ViewAvailableContestsError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val communityIds = user.data.roles.filterIsInstance<Manager>().single().memberOf.ids.toSet()
        val filter = ContestFilter(name = name)
        return contestRepository.findSharedTo(communityIds = communityIds, pagination = pagination, filter = filter).asSuccess()
    }

    /**
     * Returns [contestId] added to [classId] owned by [user] with its tasks, the class students and their results by contest task.
     * Missing role, class, contest, access and contest assignment are expected failures; storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.viewContest")
    fun viewClassContest(
        user: MultipleRoleUser,
        classId: ClassId,
        contestId: ContestId,
    ): OperationResult<ContestResults, ViewClassContestError> = operation<ContestResults, ViewClassContestError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val studyClass = classRepository.findById(classId)
        ensure(studyClass != null) { ClassNotExistsError(classId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }
        ensure(contestId in studyClass.data.contests.ids) { ContestNotAddedToClassError(classId = classId, contestId = contestId) }
        val studentIds = studyClass.data.students.ids
        val students = multipleRoleUserRepository.findByIds(studentIds).inOrderOf(studentIds)
        return findContestResults(contest = contest, members = students).asSuccess()
    }

    /**
     * Returns [contestId] added to [competitionId] owned by [user] with its tasks, the participants and their results by task.
     * Missing role, competition, contest, access and contest assignment are expected failures; storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.viewContest")
    fun viewCompetitionContest(
        user: MultipleRoleUser,
        competitionId: CompetitionId,
        contestId: ContestId,
    ): OperationResult<ContestResults, ViewCompetitionContestError> = operation<ContestResults, ViewCompetitionContestError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val competition = competitionRepository.findById(competitionId)
        ensure(competition != null) { CompetitionNotExistsError(competitionId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }
        ensure(contestId in competition.data.contests.ids) {
            ContestNotAddedToCompetitionError(competitionId = competitionId, contestId = contestId)
        }
        val participantIds = competition.data.participants.ids
        val participants = participantRepository.findByIds(participantIds).inOrderOf(participantIds)
        return findContestResults(contest = contest, members = participants).asSuccess()
    }

    /**
     * Adds [contestId] shared to a community of the [Manager] role of [user] to [classId] they own and returns the updated class.
     * Missing role, class, contest, access and repeated addition are expected failures; storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.addContest")
    fun addClassContest(user: MultipleRoleUser, classId: ClassId, contestId: ContestId): OperationResult<Class, AddClassContestError> =
        operation<Class, AddClassContestError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            val contest = contestRepository.findById(contestId)
            ensure(contest != null) { ContestNotExistsError(contestId) }
            ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }
            ensure(isAvailableToManager(user = user, contest = contest)) { ContestAccessDeniedError(contestId) }
            ensure(contestId !in studyClass.data.contests.ids) {
                ContestAlreadyAddedToClassError(classId = classId, contestId = contestId)
            }

            val updatedClass = studyClass.withData {
                contests.add(contestId)
            }
            return classRepository.update(updatedClass).asSuccess()
        }

    /**
     * Adds [contestId] shared to a community of the [Manager] role of [user] to [competitionId] they own and returns the updated
     * competition. Missing role, competition, contest, access and repeated addition are expected failures; storage exceptions propagate.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.addContest")
    fun addCompetitionContest(
        user: MultipleRoleUser,
        competitionId: CompetitionId,
        contestId: ContestId,
    ): OperationResult<Competition, AddCompetitionContestError> = operation<Competition, AddCompetitionContestError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val competition = competitionRepository.findById(competitionId)
        ensure(competition != null) { CompetitionNotExistsError(competitionId) }
        val contest = contestRepository.findById(contestId)
        ensure(contest != null) { ContestNotExistsError(contestId) }
        ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }
        ensure(isAvailableToManager(user = user, contest = contest)) { ContestAccessDeniedError(contestId) }
        ensure(contestId !in competition.data.contests.ids) {
            ContestAlreadyAddedToCompetitionError(competitionId = competitionId, contestId = contestId)
        }

        val updatedCompetition = competition.withData {
            contests.add(contestId)
        }
        return competitionRepository.update(updatedCompetition).asSuccess()
    }

    /**
     * Creates [participantCount] participants with random UUID access codes and `st<id>` names in [competitionId] owned by [user].
     * Missing role, competition, access, a nonpositive count and exceeding the participant limit are expected failures.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.createParticipants")
    fun createParticipants(
        user: MultipleRoleUser,
        competitionId: CompetitionId,
        participantCount: Int,
    ): OperationResult<List<Participant>, CreateParticipantsError> = operation<List<Participant>, CreateParticipantsError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val competition = competitionRepository.findById(competitionId)
        ensure(competition != null) { CompetitionNotExistsError(competitionId) }
        ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }
        ensure(participantCount > 0) { NonPositiveParticipantCountError(participantCount) }

        val maxParticipants = competitionConfig.maxParticipants
        val currentParticipantCount = competition.data.participants.ids.size
        ensure(currentParticipantCount.toLong() + participantCount <= maxParticipants) {
            CompetitionParticipantLimitExceededError(
                competitionId = competitionId,
                currentParticipantCount = currentParticipantCount,
                participantCount = participantCount,
                maxParticipants = maxParticipants,
            )
        }

        val accessTokenHashes = generateSequence { UUID.randomUUID().toString() }
            .distinct()
            .take(participantCount)
            .map { accessToken -> AccessTokenHash.hashAccessToken(rawAccessToken = accessToken, algorithm = HashAlgorithm.Identity) }
            .toList()
        val participants = participantRepository.saveToCompetition(
            competitionId = competitionId,
            accessTokenHashes = accessTokenHashes,
        ) { participantId ->
            "st${participantId.value}"
        }
        return participants.asSuccess()
    }

    private fun replaceCode(invite: ClassInvite, expiresAt: Instant): ClassInvite {
        val rawCode = generateInviteCode(random = random, previous = invite.data.codeHash)
        val replaced = invite.withData {
            code(rawCode, HashAlgorithm.Identity)
            this.expiresAt = expiresAt
        }
        return classInviteRepository.update(replaced)
    }

    private fun isAvailableToManager(user: MultipleRoleUser, contest: Contest): Boolean {
        val managerCommunityIds = user.data.roles.filterIsInstance<Manager>().single().memberOf.ids
        return contest.data.sharedTo.ids.any { communityId -> communityId in managerCommunityIds }
    }

    private fun findContestResults(contest: Contest, members: List<User<*>>): ContestResults {
        val taskIds = contest.data.tasks.ids
        val results = submissionRepository.findContestResults(
            contestId = contest.id,
            authorIds = members.map { member -> member.id }.toSet(),
            taskIds = taskIds.toSet(),
        )

        val tasks = taskRepository.findByIds(taskIds).inOrderOf(taskIds)
        return ContestResults(contest = contest, tasks = tasks, members = members, results = results)
    }

    private fun <Id : DomainId, Entity : DomainEntity<Id>> List<Entity>.inOrderOf(ids: List<Id>): List<Entity> {
        val byId = associateBy { entity -> entity.id }
        return ids.mapNotNull(byId::get)
    }

    /**
     * Class owned by the manager with the data shown on its page.
     *
     * @property studyClass the viewed class.
     * @property invite the invite code of the class.
     * @property students the students in stored order, each with its last login, or `null` if never logged in.
     * @property contests the added contests in stored order.
     * @since %CURRENT_VERSION%
     */
    data class ClassDetails(
        val studyClass: Class,
        val invite: ClassInvite,
        val students: List<Pair<MultipleRoleUser, Instant?>>,
        val contests: List<Contest>,
    )

    /**
     * Competition owned by the manager with the data shown on its page.
     *
     * @property competition the viewed competition.
     * @property participants the participants in stored order, each with its last login, or `null` if never logged in.
     * @property contests the added contests in stored order.
     * @since %CURRENT_VERSION%
     */
    data class CompetitionDetails(
        val competition: Competition,
        val participants: List<Pair<Participant, Instant?>>,
        val contests: List<Contest>,
    )

    /**
     * Results of a contest within a class or competition owned by the manager.
     *
     * @property contest the viewed contest.
     * @property tasks the tasks of the contest in stored order.
     * @property members the class students or competition participants in stored order.
     * @property results the results of member and task pairs having submissions, ordered by member id and then task id.
     * @since %CURRENT_VERSION%
     */
    data class ContestResults(
        val contest: Contest,
        val tasks: List<Task>,
        val members: List<User<*>>,
        val results: List<ContestTaskResult>,
    )
}
