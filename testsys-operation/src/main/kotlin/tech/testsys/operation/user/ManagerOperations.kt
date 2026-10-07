package tech.testsys.operation.user

import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.operation.annotation.Feature
import tech.testsys.operation.annotation.InternalOperationsApi
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNameBlankError
import tech.testsys.operation.error.ClassNameTooLongError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionNameBlankError
import tech.testsys.operation.error.CompetitionNameTooLongError
import tech.testsys.operation.error.CreateClassError
import tech.testsys.operation.error.CreateCompetitionError
import tech.testsys.operation.error.MissedManagerRoleError
import tech.testsys.operation.error.OperationResult
import tech.testsys.operation.error.ViewClassError
import tech.testsys.operation.error.ViewClassesError
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
    private val multipleRoleUserRepository: MultipleRoleUserRepository,
    private val contestRepository: ContestRepository,
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
     * Returns [classId] owned by [user] with its enrolled students and assigned contests loaded.
     * Missing role, class and access are expected failures; storage exceptions propagate to the caller.
     *
     * @throws IllegalArgumentException if an enrolled student or assigned contest does not exist.
     * @since %CURRENT_VERSION%
     */
    @Feature("testsys.user.multi.manager.class.viewClass")
    fun viewClass(user: MultipleRoleUser, classId: ClassId): OperationResult<ClassView, ViewClassError> =
        operation<ClassView, ViewClassError> {
            ensure(user.hasRole<Manager>(), MissedManagerRoleError)
            val studyClass = classRepository.findById(classId)
            ensure(studyClass != null) { ClassNotExistsError(classId) }
            ensure(studyClass.data.owner.id == user.id) { ClassAccessDeniedError(classId) }

            val students = studyClass.data.students.load(multipleRoleUserRepository)
            val contests = studyClass.data.contests.load(contestRepository)
            return ClassView(studyClass = studyClass, students = students, contests = contests).asSuccess()
        }

    /**
     * A class and its loaded membership and assigned contests returned by the viewing operation.
     *
     * @property studyClass the class with its stored data and version preserved.
     * @property students the enrolled users, regardless of their current roles.
     * @property contests all assigned contests, regardless of current community access.
     * @since %CURRENT_VERSION%
     */
    data class ClassView(
        val studyClass: Class,
        val students: List<MultipleRoleUser>,
        val contests: List<Contest>,
    )
}
