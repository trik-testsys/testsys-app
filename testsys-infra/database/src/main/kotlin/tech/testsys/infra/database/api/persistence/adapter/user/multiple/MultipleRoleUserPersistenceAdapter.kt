package tech.testsys.infra.database.api.persistence.adapter.user.multiple

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.JudgmentOrderId
import tech.testsys.domain.model.task.SubmissionId
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.Administrator
import tech.testsys.domain.model.user.CompatibleUserRole
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.Judge
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserData
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Student
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.UserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.UserTypeJpaEnum
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.AdministratorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.DeveloperDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.JudgeDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.ManagerDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserId
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.MultipleRoleToUserJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.StudentDataJpaEntity
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.UserMultipleRoleJpaEnum
import tech.testsys.infra.database.internal.jpa.repository.group.ClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.CompetitionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.StudentToClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.ContestJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.JudgmentOrderJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.SubmissionJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.task.TaskJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.AdministratorDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.DeveloperDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.JudgeDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.ManagerDataJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.MultipleRoleToUserJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.multiple.StudentDataJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.user.multiple.MultipleRoleUserMapping
import tech.testsys.infra.database.internal.mapping.user.multiple.MultipleRoleUserRoles
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable

/**
 * Persistence adapter for [MultipleRoleUser].
 *
 * Writable state is limited to the user scalars (name, access token, email),
 * the set of held roles (per-role data rows) and the per-role community
 * memberships ([MultipleRoleToUserJpaEntity]). The role payload lists —
 * developer tasks/contests, student classes/submissions, judge judgment orders,
 * manager classes/competitions — are read-only projections derived from the
 * owning side (`Task.ownerId`, `Submission.authorId`, `JudgmentOrder.judgeId`,
 * `StudentToClass`, ...) on read and are deliberately ignored on save/update.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class MultipleRoleUserPersistenceAdapter(
    jpaEntityRepository: UserJpaEntityRepository,
    private val multipleRoleToUserJpaEntityRepository: MultipleRoleToUserJpaEntityRepository,
    private val administratorDataJpaEntityRepository: AdministratorDataJpaEntityRepository,
    private val developerDataJpaEntityRepository: DeveloperDataJpaEntityRepository,
    private val studentDataJpaEntityRepository: StudentDataJpaEntityRepository,
    private val judgeDataJpaEntityRepository: JudgeDataJpaEntityRepository,
    private val managerDataJpaEntityRepository: ManagerDataJpaEntityRepository,
    private val taskJpaEntityRepository: TaskJpaEntityRepository,
    private val contestJpaEntityRepository: ContestJpaEntityRepository,
    private val submissionJpaEntityRepository: SubmissionJpaEntityRepository,
    private val studentToClassJpaEntityRepository: StudentToClassJpaEntityRepository,
    private val judgmentOrderJpaEntityRepository: JudgmentOrderJpaEntityRepository,
    private val classJpaEntityRepository: ClassJpaEntityRepository,
    private val competitionJpaEntityRepository: CompetitionJpaEntityRepository,
) : AbstractPersistenceAdapter<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser, UserJpaEntity>(jpaEntityRepository),
    MultipleRoleUserRepository {

    @Transactional
    override fun save(data: MultipleRoleUserData): MultipleRoleUser {
        val savedUserJpaEntity = jpaEntityRepository.save(MultipleRoleUserMapping.toUserJpaEntity(data))
        val userId = savedUserJpaEntity.requireId()
        syncRoles(userId, data.roles)
        val domainEntity = MultipleRoleUserMapping.toDomain(savedUserJpaEntity, assembleRoles(userId))
        return domainEntity
    }

    @Transactional
    override fun update(entity: MultipleRoleUser): MultipleRoleUser {
        val currentUserJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedUserJpaEntity = jpaEntityRepository.save(
            MultipleRoleUserMapping.toUserJpaEntity(entity, currentUserJpaEntity),
        )
        val userId = updatedUserJpaEntity.requireId()

        syncRoles(userId, entity.data.roles)

        val domainEntity = MultipleRoleUserMapping.toDomain(updatedUserJpaEntity, assembleRoles(userId))
        return domainEntity
    }

    @Transactional
    override fun removeById(id: MultipleRoleUserId) {
        val userJpaEntity = jpaEntityRepository.findByIdOrNull(id.value)?.takeIf { supports(it) } ?: return
        val userId = userJpaEntity.requireId()

        multipleRoleToUserJpaEntityRepository.deleteAll(multipleRoleToUserJpaEntityRepository.findAllByUserId(userId))
        studentToClassJpaEntityRepository.deleteAll(studentToClassJpaEntityRepository.findAllByStudentId(userId))
        administratorDataJpaEntityRepository.findByUserId(userId)?.let(administratorDataJpaEntityRepository::delete)
        developerDataJpaEntityRepository.findByUserId(userId)?.let(developerDataJpaEntityRepository::delete)
        studentDataJpaEntityRepository.findByUserId(userId)?.let(studentDataJpaEntityRepository::delete)
        judgeDataJpaEntityRepository.findByUserId(userId)?.let(judgeDataJpaEntityRepository::delete)
        managerDataJpaEntityRepository.findByUserId(userId)?.let(managerDataJpaEntityRepository::delete)
        jpaEntityRepository.deleteById(userId)
    }

    @Transactional
    override fun removeByIds(ids: List<MultipleRoleUserId>) = ids.forEach(::removeById)

    override fun supports(jpaEntity: UserJpaEntity) = jpaEntity.type == UserTypeJpaEnum.MULTIPLE_ROLE

    override fun assemble(jpaEntity: UserJpaEntity): MultipleRoleUser {
        val userId = jpaEntity.requireId()
        val roles = assembleRoles(userId)
        return MultipleRoleUserMapping.toDomain(jpaEntity, roles)
    }

    private fun assembleRoles(userId: Long): MultipleRoleUserRoles {
        val memberships = multipleRoleToUserJpaEntityRepository.findAllByUserId(userId)
        val communitiesByRole: Map<UserMultipleRoleJpaEnum, List<CommunityId>> =
            memberships.groupBy({ it.id.multipleRole }, { CommunityId(it.id.communityId) })

        fun memberOf(role: UserMultipleRoleJpaEnum) = communitiesByRole[role].orEmpty()

        return MultipleRoleUserRoles(
            administrator = administratorDataJpaEntityRepository.findByUserId(userId)
                ?.let { MultipleRoleUserRoles.AdministratorRoleInfo(memberOf = memberOf(UserMultipleRoleJpaEnum.ADMINISTRATOR)) },
            developer = developerDataJpaEntityRepository.findByUserId(userId)
                ?.let { buildDeveloperInfo(userId, memberOf(UserMultipleRoleJpaEnum.DEVELOPER)) },
            student = studentDataJpaEntityRepository.findByUserId(userId)
                ?.let { buildStudentInfo(userId, memberOf(UserMultipleRoleJpaEnum.STUDENT)) },
            judge = judgeDataJpaEntityRepository.findByUserId(userId)
                ?.let { buildJudgeInfo(userId, memberOf(UserMultipleRoleJpaEnum.JUDGE)) },
            manager = managerDataJpaEntityRepository.findByUserId(userId)
                ?.let { buildManagerInfo(userId, memberOf(UserMultipleRoleJpaEnum.MANAGER)) },
        )
    }

    private fun buildDeveloperInfo(userId: Long, memberOf: List<CommunityId>): MultipleRoleUserRoles.DeveloperRoleInfo {
        return MultipleRoleUserRoles.DeveloperRoleInfo(
            memberOf = memberOf,
            tasks = taskJpaEntityRepository.findAllByOwnerId(userId).map { TaskId(it.requireId()) },
            contests = contestJpaEntityRepository.findAllByOwnerId(userId).map { ContestId(it.requireId()) },
        )
    }

    private fun buildStudentInfo(userId: Long, memberOf: List<CommunityId>): MultipleRoleUserRoles.StudentRoleInfo {
        return MultipleRoleUserRoles.StudentRoleInfo(
            memberOf = memberOf,
            classes = studentToClassJpaEntityRepository.findAllByStudentId(userId).map { ClassId(it.id.classId) },
            submissions = submissionJpaEntityRepository.findAllByAuthorId(userId).map { SubmissionId(it.requireId()) },
        )
    }

    private fun buildJudgeInfo(userId: Long, memberOf: List<CommunityId>): MultipleRoleUserRoles.JudgeRoleInfo {
        return MultipleRoleUserRoles.JudgeRoleInfo(
            memberOf = memberOf,
            judgmentOrders = judgmentOrderJpaEntityRepository.findAllByJudgeId(userId)
                .map { JudgmentOrderId(it.requireId()) },
        )
    }

    private fun buildManagerInfo(userId: Long, memberOf: List<CommunityId>): MultipleRoleUserRoles.ManagerRoleInfo {
        return MultipleRoleUserRoles.ManagerRoleInfo(
            memberOf = memberOf,
            classes = classJpaEntityRepository.findAllByOwnerId(userId).map { ClassId(it.requireId()) },
            competitions = competitionJpaEntityRepository.findAllByOwnerId(userId)
                .map { CompetitionId(it.requireId()) },
        )
    }

    /**
     * Reconciles the set of role-data rows and `(role, community)` memberships
     * for [userId] with [roles].
     *
     * Existing role-data rows whose role is still held are left untouched
     * (`createdAt` and `version` are preserved); only newly-acquired roles are
     * inserted and dropped roles are removed. Memberships are diffed via
     * [syncJoinTable] so unchanged `(role, community)` pairs survive the update.
     */
    private fun syncRoles(userId: Long, roles: List<CompatibleUserRole>) {
        val targetRoleEnums = roles.map(::roleEnumOf).toSet()

        syncAdministratorPresence(userId, isTarget = UserMultipleRoleJpaEnum.ADMINISTRATOR in targetRoleEnums)
        syncDeveloperPresence(userId, isTarget = UserMultipleRoleJpaEnum.DEVELOPER in targetRoleEnums)
        syncStudentPresence(userId, isTarget = UserMultipleRoleJpaEnum.STUDENT in targetRoleEnums)
        syncJudgePresence(userId, isTarget = UserMultipleRoleJpaEnum.JUDGE in targetRoleEnums)
        syncManagerPresence(userId, isTarget = UserMultipleRoleJpaEnum.MANAGER in targetRoleEnums)

        syncMemberships(userId, roles)
    }

    private fun syncAdministratorPresence(userId: Long, isTarget: Boolean) {
        val current = administratorDataJpaEntityRepository.findByUserId(userId)
        when {
            isTarget && current == null ->
                administratorDataJpaEntityRepository.save(AdministratorDataJpaEntity(userId = userId))
            !isTarget && current != null -> administratorDataJpaEntityRepository.delete(current)
        }
    }

    private fun syncDeveloperPresence(userId: Long, isTarget: Boolean) {
        val current = developerDataJpaEntityRepository.findByUserId(userId)
        when {
            isTarget && current == null ->
                developerDataJpaEntityRepository.save(DeveloperDataJpaEntity(userId = userId))
            !isTarget && current != null -> developerDataJpaEntityRepository.delete(current)
        }
    }

    private fun syncStudentPresence(userId: Long, isTarget: Boolean) {
        val current = studentDataJpaEntityRepository.findByUserId(userId)
        when {
            isTarget && current == null ->
                studentDataJpaEntityRepository.save(StudentDataJpaEntity(userId = userId))
            !isTarget && current != null -> studentDataJpaEntityRepository.delete(current)
        }
    }

    private fun syncJudgePresence(userId: Long, isTarget: Boolean) {
        val current = judgeDataJpaEntityRepository.findByUserId(userId)
        when {
            isTarget && current == null ->
                judgeDataJpaEntityRepository.save(JudgeDataJpaEntity(userId = userId))
            !isTarget && current != null -> judgeDataJpaEntityRepository.delete(current)
        }
    }

    private fun syncManagerPresence(userId: Long, isTarget: Boolean) {
        val current = managerDataJpaEntityRepository.findByUserId(userId)
        when {
            isTarget && current == null ->
                managerDataJpaEntityRepository.save(ManagerDataJpaEntity(userId = userId))
            !isTarget && current != null -> managerDataJpaEntityRepository.delete(current)
        }
    }

    private fun syncMemberships(userId: Long, roles: List<CompatibleUserRole>) {
        val targetIds: List<MultipleRoleToUserId> = roles.flatMap { role ->
            val roleEnum = roleEnumOf(role)
            role.memberOf.ids.map {
                MultipleRoleToUserId(multipleRole = roleEnum, userId = userId, communityId = it.value)
            }
        }
        syncJoinTable(
            existing = multipleRoleToUserJpaEntityRepository.findAllByUserId(userId),
            targetKeys = targetIds,
            keyOf = { it.id },
            buildAssociation = ::MultipleRoleToUserJpaEntity,
            deleteAll = { multipleRoleToUserJpaEntityRepository.deleteAll(it) },
            saveAll = { multipleRoleToUserJpaEntityRepository.saveAll(it) },
        )
    }

    private fun roleEnumOf(role: CompatibleUserRole): UserMultipleRoleJpaEnum = when (role) {
        is Administrator -> UserMultipleRoleJpaEnum.ADMINISTRATOR
        is Developer -> UserMultipleRoleJpaEnum.DEVELOPER
        is Student -> UserMultipleRoleJpaEnum.STUDENT
        is Judge -> UserMultipleRoleJpaEnum.JUDGE
        is Manager -> UserMultipleRoleJpaEnum.MANAGER
    }
}
