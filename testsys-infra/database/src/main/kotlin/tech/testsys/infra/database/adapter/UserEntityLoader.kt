package tech.testsys.infra.database.adapter

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Component
import tech.testsys.domain.model.LazyEntityList
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.ExerciseId
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SolutionId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.task.TestId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.DeveloperData
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.JudgeData
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.ManagerData
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.Observer
import tech.testsys.domain.model.user.ObserverData
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.ParticipantData
import tech.testsys.domain.model.user.Student
import tech.testsys.domain.model.user.StudentData
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.domain.model.user.User
import tech.testsys.domain.model.user.UserId
import tech.testsys.domain.model.LazyEntity
import tech.testsys.infra.database.jpa.entity.UserJpaEntity
import tech.testsys.infra.database.jpa.entity.UserRoleJpaEntity
import tech.testsys.infra.database.jpa.spring.UserRoleSpringRepository
import tech.testsys.infra.database.jpa.spring.UserSpringRepository

@Component
class UserEntityLoader(
    private val userRepo: UserSpringRepository,
    private val roleRepo: UserRoleSpringRepository,
) {
    fun loadById(id: Long): User {
        val jpa: UserJpaEntity = userRepo.findById(id).orElseThrow {
            EntityNotFoundException("User $id not found")
        }
        return toDomain(jpa)
    }

    fun loadByIdOrNull(id: Long): User? =
        userRepo.findById(id).orElse(null)?.let { toDomain(it) }

    fun loadByIds(ids: List<Long>, pageSize: Int, page: Int): List<User> {
        val paginatedIds = ids.drop(page * pageSize).take(pageSize)
        return userRepo.findAllById(paginatedIds).map { toDomain(it) }
    }

    fun toDomain(jpa: UserJpaEntity): User = when (jpa.userType) {
        "PARTICIPANT" -> Participant(
            id = UserId(jpa.id),
            data = ParticipantData(
                competition = LazyEntity(CompetitionId(jpa.competitionId!!))
            )
        )
        "OBSERVER" -> Observer(
            id = UserId(jpa.id),
            data = ObserverData(
                competitions = LazyEntityList(jpa.observerCompetitionIds.map { CompetitionId(it) })
            )
        )
        "SUPERVISOR" -> Supervisor(id = UserId(jpa.id))
        "MULTIPLE_ROLE" -> {
            val roles = roleRepo.findByUserId(jpa.id).map { toDomainRole(it) }
            TODO(
                "MultipleRoleUser is sealed with no concrete subclass. " +
                "Add a concrete subclass to tech.testsys.domain.model.user and construct it here. " +
                "Roles loaded: $roles, data: ${MultipleRoleUserData(roles = roles)}"
            )
        }
        else -> error("Unknown user type: ${jpa.userType}")
    }

    private fun toDomainRole(jpa: UserRoleJpaEntity): CompatibleUserRole = when (jpa.roleType) {
        "DEVELOPER" -> Developer(
            memberOf = LazyEntityList(jpa.communityIds.map { CommunityId(it) }),
            data = DeveloperData(
                tasks = LazyEntityList(jpa.taskIds.map { TaskId(it) }),
                contest = LazyEntityList(jpa.contestIds.map { ContestId(it) }),
                polygons = LazyEntityList(jpa.testIds.map { TestId(it) }),
                solutions = LazyEntityList(jpa.solutionIds.map { SolutionId(it) }),
                exercises = LazyEntityList(jpa.exerciseIds.map { ExerciseId(it) }),
            )
        )
        "STUDENT" -> Student(
            memberOf = LazyEntityList(jpa.communityIds.map { CommunityId(it) }),
            data = StudentData(
                classes = LazyEntityList(jpa.classIds.map { ClassId(it) }),
                submissions = LazyEntityList(jpa.submissionIds.map { SubmissionId(it) }),
            )
        )
        "ADMINISTRATOR" -> Administrator(
            memberOf = LazyEntityList(jpa.communityIds.map { CommunityId(it) })
        )
        "JUDGE" -> Judge(
            memberOf = LazyEntityList(jpa.communityIds.map { CommunityId(it) }),
            data = JudgeData(
                judgmentOrders = LazyEntityList(jpa.judgmentOrderIds.map { JudgmentOrderId(it) })
            )
        )
        "MANAGER" -> Manager(
            memberOf = LazyEntityList(jpa.communityIds.map { CommunityId(it) }),
            data = ManagerData(
                classes = LazyEntityList(jpa.classIds.map { ClassId(it) }),
                competitions = LazyEntityList(jpa.competitionIds.map { CompetitionId(it) }),
            )
        )
        else -> error("Unknown role type: ${jpa.roleType}")
    }

    fun toJpaRole(userId: Long, role: CompatibleUserRole): UserRoleJpaEntity {
        val jpa = UserRoleJpaEntity(userId = userId)
        when (role) {
            is Developer -> {
                jpa.roleType = "DEVELOPER"
                jpa.communityIds = role.memberOf.ids.map { it.value }.toMutableList()
                jpa.taskIds = role.data.tasks.ids.map { it.value }.toMutableList()
                jpa.contestIds = role.data.contest.ids.map { it.value }.toMutableList()
                jpa.testIds = role.data.polygons.ids.map { it.value }.toMutableList()
                jpa.solutionIds = role.data.solutions.ids.map { it.value }.toMutableList()
                jpa.exerciseIds = role.data.exercises.ids.map { it.value }.toMutableList()
            }
            is Student -> {
                jpa.roleType = "STUDENT"
                jpa.communityIds = role.memberOf.ids.map { it.value }.toMutableList()
                jpa.classIds = role.data.classes.ids.map { it.value }.toMutableList()
                jpa.submissionIds = role.data.submissions.ids.map { it.value }.toMutableList()
            }
            is Administrator -> {
                jpa.roleType = "ADMINISTRATOR"
                jpa.communityIds = role.memberOf.ids.map { it.value }.toMutableList()
            }
            is Judge -> {
                jpa.roleType = "JUDGE"
                jpa.communityIds = role.memberOf.ids.map { it.value }.toMutableList()
                jpa.judgmentOrderIds = role.data.judgmentOrders.ids.map { it.value }.toMutableList()
            }
            is Manager -> {
                jpa.roleType = "MANAGER"
                jpa.communityIds = role.memberOf.ids.map { it.value }.toMutableList()
                jpa.classIds = role.data.classes.ids.map { it.value }.toMutableList()
                jpa.competitionIds = role.data.competitions.ids.map { it.value }.toMutableList()
            }
        }
        return jpa
    }
}