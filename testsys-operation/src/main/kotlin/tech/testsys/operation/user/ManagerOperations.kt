package tech.testsys.operation.user

import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNameBlankError
import tech.testsys.operation.error.ClassNameTooLongError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionAccessDeniedError
import tech.testsys.operation.error.CompetitionNameBlankError
import tech.testsys.operation.error.CompetitionNameTooLongError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.CreateClassError
import tech.testsys.operation.error.CreateCompetitionError
import tech.testsys.operation.error.MissedManagerRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ViewClassError
import tech.testsys.operation.error.ViewClassesError
import tech.testsys.operation.error.ViewCompetitionError
import tech.testsys.operation.error.ViewCompetitionsError
import tech.testsys.operation.error.asSuccess
import tech.testsys.operation.error.ensure
import tech.testsys.operation.error.operation
import tech.testsys.operation.util.hasRole

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
) {

    /**
     * Creates a class owned by [user] with unchanged [className], an empty description and no students or contests.
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

            val classData = classData {
                owner = user.id
                name = className
                description = ""
            }

            val studyClass = classRepository.save(classData)
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
     * Returns [classId] owned by [user], preserving its data, including the stored student and contest ids.
     * Missing role, class and access are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.viewClass")
    fun viewClass(user: MultipleRoleUser, classId: ClassId): OperationResult<Class, ViewClassError> = operation<Class, ViewClassError> {
        ensure(user.hasRole<Manager>(), MissedManagerRoleError)
        val studyClass = classRepository.findById(classId)
        ensure(studyClass != null) { ClassNotExistsError(classId) }
        ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }
        return studyClass.asSuccess()
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
     * Returns [competitionId] owned by [user], preserving its data, including the stored participant and contest ids.
     * Missing role, competition and access are expected failures; storage exceptions propagate to the caller.
     *
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.competition.viewCompetition")
    fun viewCompetition(user: MultipleRoleUser, competitionId: CompetitionId): OperationResult<Competition, ViewCompetitionError> =
        operation<Competition, ViewCompetitionError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val competition = competitionRepository.findById(competitionId)
            ensure(competition != null) { CompetitionNotExistsError(competitionId) }
            ensure(competition.data.owner.id == user.id) { CompetitionAccessDeniedError(competitionId) }
            return competition.asSuccess()
        }
}
