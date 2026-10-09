package tech.testsys.infra.database.api.persistence.adapter.user.multiple

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.domain.model.group.CommunityInvite
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
import tech.testsys.infra.database.api.persistence.adapter.user.AbstractUserPersistenceAdapter
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
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
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
import tech.testsys.infra.database.internal.utils.findAllInChunks
import tech.testsys.infra.database.internal.utils.findLinkedIds
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.syncJoinTable

/**
 * Persistence adapter of [MultipleRoleUser] entities backed by [UserJpaEntity] rows of the multiple-role type.
 * Writes cover the user scalars, the held roles and their community memberships ([MultipleRoleToUserJpaEntity])
 * and increment the user version first; removal also increments the versions of the student's classes.
 * [update] rejects removing the student role while the user is enrolled in a class.
 * The per-role id lists (tasks, classes, submissions, ...) are read-only projections of the owning side.
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
) : AbstractUserPersistenceAdapter<MultipleRoleUserData, MultipleRoleUserId, MultipleRoleUser>(jpaEntityRepository),
    MultipleRoleUserRepository {

    private val users: UserJpaEntityRepository = jpaEntityRepository

    @Transactional
    override fun save(data: MultipleRoleUserData): MultipleRoleUser {
        val savedUserJpaEntity = jpaEntityRepository.save(MultipleRoleUserMapping.toUserJpaEntity(data))
        val userId = savedUserJpaEntity.requireId()
        syncRoles(userId, data.roles)
        val domainEntity = MultipleRoleUserMapping.toDomain(savedUserJpaEntity, assembleRoles(listOf(userId)).getValue(userId))
        return domainEntity
    }

    @Transactional
    override fun update(entity: MultipleRoleUser): MultipleRoleUser {
        val updatedUserJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            require(isMultipleRole(current)) { "User ${entity.id.value} is not a multiple-role user: ${current.type}" }
            MultipleRoleUserMapping.toUserJpaEntity(entity, current)
        }
        val userId = updatedUserJpaEntity.requireId()

        syncRoles(userId, entity.data.roles)

        val domainEntity = MultipleRoleUserMapping.toDomain(updatedUserJpaEntity, assembleRoles(listOf(userId)).getValue(userId))
        return domainEntity
    }

    @Transactional
    override fun addCommunityMembership(
        userId: MultipleRoleUserId,
        communityId: CommunityId,
        kind: CommunityInvite.Kind,
    ): MultipleRoleUser {
        requireNotNull(users.findByIdOrNull(userId.value)?.takeIf(::isMultipleRole)) {
            "Multiple-role user ${userId.value} does not exist for community membership"
        }
        val userJpaEntity = touchRoot(users, userId.value, changesRootData = true)
        val roleEnum = when (kind) {
            CommunityInvite.Kind.Manager -> {
                syncManagerPresence(userId.value, isTarget = true)
                UserMultipleRoleJpaEnum.MANAGER
            }
            CommunityInvite.Kind.Developer -> {
                syncDeveloperPresence(userId.value, isTarget = true)
                UserMultipleRoleJpaEnum.DEVELOPER
            }
        }
        val membershipId = MultipleRoleToUserId(
            multipleRole = roleEnum,
            userId = userId.value,
            communityId = communityId.value,
        )
        if (!multipleRoleToUserJpaEntityRepository.existsById(membershipId)) {
            multipleRoleToUserJpaEntityRepository.saveAndFlush(MultipleRoleToUserJpaEntity(membershipId))
        }
        return assemble(userJpaEntity)
    }

    override fun removeRoot(id: MultipleRoleUserId, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value)?.takeIf(::isMultipleRole) ?: return
        val userId = touchRoot(users, id.value, expectedVersion, changesRootData = true).requireId()
        val studentRows = studentToClassJpaEntityRepository.findAllByStudentId(userId)
        studentRows.map { row -> row.id.classId }.distinct().sorted().forEach { classId ->
            touchRoot(classJpaEntityRepository, classId, changesRootData = true)
        }

        multipleRoleToUserJpaEntityRepository.deleteAll(multipleRoleToUserJpaEntityRepository.findAllByUserId(userId))
        studentToClassJpaEntityRepository.deleteAll(studentRows)
        administratorDataJpaEntityRepository.findByUserId(userId)?.let(administratorDataJpaEntityRepository::delete)
        developerDataJpaEntityRepository.findByUserId(userId)?.let(developerDataJpaEntityRepository::delete)
        studentDataJpaEntityRepository.findByUserId(userId)?.let(studentDataJpaEntityRepository::delete)
        judgeDataJpaEntityRepository.findByUserId(userId)?.let(judgeDataJpaEntityRepository::delete)
        managerDataJpaEntityRepository.findByUserId(userId)?.let(managerDataJpaEntityRepository::delete)
        jpaEntityRepository.deleteById(userId)
    }

    @Transactional(readOnly = true)
    override fun findByEmail(email: String): MultipleRoleUser? = users.findByEmail(email)?.takeIf(::isMultipleRole)?.let { assemble(it) }

    override fun assembleSupported(rows: List<UserJpaEntity>): List<MultipleRoleUser> {
        val supported = rows.filter(::isMultipleRole)
        val roles = assembleRoles(supported.map { row -> row.requireId() })
        return supported.map { row -> MultipleRoleUserMapping.toDomain(row, roles.getValue(row.requireId())) }
    }

    private fun isMultipleRole(jpaEntity: UserJpaEntity) = jpaEntity.type == UserTypeJpaEnum.MULTIPLE_ROLE

    /**
     * Assembles the roles of [userIds]: memberships and the held roles in one query per table, then the id lists of
     * every role only for the users holding it.
     */
    private fun assembleRoles(userIds: List<Long>): Map<Long, MultipleRoleUserRoles> {
        val memberships =
            Memberships(findAllInChunks(ids = userIds, find = multipleRoleToUserJpaEntityRepository::findAllByIdUserIdIn))
        val administrators = findAllInChunks(ids = userIds, find = administratorDataJpaEntityRepository::findUserIdsByUserIdIn).toSet()
        val developers = buildDeveloperInfos(
            userIds = findAllInChunks(ids = userIds, find = developerDataJpaEntityRepository::findUserIdsByUserIdIn),
            memberships = memberships,
        )
        val students = buildStudentInfos(
            userIds = findAllInChunks(ids = userIds, find = studentDataJpaEntityRepository::findUserIdsByUserIdIn),
            memberships = memberships,
        )
        val judges = buildJudgeInfos(
            userIds = findAllInChunks(ids = userIds, find = judgeDataJpaEntityRepository::findUserIdsByUserIdIn),
            memberships = memberships,
        )
        val managers = buildManagerInfos(
            userIds = findAllInChunks(ids = userIds, find = managerDataJpaEntityRepository::findUserIdsByUserIdIn),
            memberships = memberships,
        )

        return userIds.associateWith { userId ->
            MultipleRoleUserRoles(
                administrator = if (userId in administrators) {
                    MultipleRoleUserRoles.AdministratorRoleInfo(memberOf = memberships.of(userId, UserMultipleRoleJpaEnum.ADMINISTRATOR))
                } else {
                    null
                },
                developer = developers[userId],
                student = students[userId],
                judge = judges[userId],
                manager = managers[userId],
            )
        }
    }

    private fun buildDeveloperInfos(userIds: List<Long>, memberships: Memberships): Map<Long, MultipleRoleUserRoles.DeveloperRoleInfo> {
        val tasks = findOwnedIds(userIds, taskJpaEntityRepository::findLinkedIdsByOwnerIdIn)
        val contests = findOwnedIds(userIds, contestJpaEntityRepository::findLinkedIdsByOwnerIdIn)
        return userIds.associateWith { userId ->
            MultipleRoleUserRoles.DeveloperRoleInfo(
                memberOf = memberships.of(userId, UserMultipleRoleJpaEnum.DEVELOPER),
                tasks = tasks.getValue(userId).map(::TaskId),
                contests = contests.getValue(userId).map(::ContestId),
            )
        }
    }

    private fun buildStudentInfos(userIds: List<Long>, memberships: Memberships): Map<Long, MultipleRoleUserRoles.StudentRoleInfo> {
        val classes = findOwnedIds(userIds, studentToClassJpaEntityRepository::findLinkedIdsByStudentIdIn)
        val submissions = findOwnedIds(userIds, submissionJpaEntityRepository::findLinkedIdsByAuthorIdIn)
        return userIds.associateWith { userId ->
            MultipleRoleUserRoles.StudentRoleInfo(
                memberOf = memberships.of(userId, UserMultipleRoleJpaEnum.STUDENT),
                classes = classes.getValue(userId).map(::ClassId),
                submissions = submissions.getValue(userId).map(::SubmissionId),
            )
        }
    }

    private fun buildJudgeInfos(userIds: List<Long>, memberships: Memberships): Map<Long, MultipleRoleUserRoles.JudgeRoleInfo> {
        val judgmentOrders = findOwnedIds(userIds, judgmentOrderJpaEntityRepository::findLinkedIdsByJudgeIdIn)
        return userIds.associateWith { userId ->
            MultipleRoleUserRoles.JudgeRoleInfo(
                memberOf = memberships.of(userId, UserMultipleRoleJpaEnum.JUDGE),
                judgmentOrders = judgmentOrders.getValue(userId).map(::JudgmentOrderId),
            )
        }
    }

    private fun buildManagerInfos(userIds: List<Long>, memberships: Memberships): Map<Long, MultipleRoleUserRoles.ManagerRoleInfo> {
        val classes = findOwnedIds(userIds, classJpaEntityRepository::findLinkedIdsByOwnerIdIn)
        val competitions = findOwnedIds(userIds, competitionJpaEntityRepository::findLinkedIdsByOwnerIdIn)
        return userIds.associateWith { userId ->
            MultipleRoleUserRoles.ManagerRoleInfo(
                memberOf = memberships.of(userId, UserMultipleRoleJpaEnum.MANAGER),
                classes = classes.getValue(userId).map(::ClassId),
                competitions = competitions.getValue(userId).map(::CompetitionId),
            )
        }
    }

    private fun findOwnedIds(userIds: List<Long>, find: (List<Long>) -> List<LinkedIdRow>): Map<Long, List<Long>> =
        findLinkedIds(ownerIds = userIds, find = find, ownerIdOf = LinkedIdRow::ownerId, linkedIdOf = LinkedIdRow::linkedId)

    /**
     * Reconciles the role-data rows and `(role, community)` memberships of [userId] with [roles]: kept roles and
     * unchanged memberships are left untouched, new ones are inserted and dropped ones removed.
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
            !isTarget && current != null -> {
                // The user row is already touched, and addStudent touches it too, so a concurrent enrolment conflicts.
                val classIds = studentToClassJpaEntityRepository.findAllByStudentId(userId).map { row -> row.id.classId }
                require(classIds.isEmpty()) { "Student role of user $userId cannot be removed while enrolled in classes $classIds" }
                studentDataJpaEntityRepository.delete(current)
            }
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

    /**
     * Communities of the loaded membership [rows] grouped by user and role.
     */
    private class Memberships(rows: List<MultipleRoleToUserJpaEntity>) {

        private val communities = rows.groupBy(
            keySelector = { row -> row.id.userId to row.id.multipleRole },
            valueTransform = { row -> CommunityId(row.id.communityId) },
        )

        fun of(userId: Long, role: UserMultipleRoleJpaEnum): List<CommunityId> = communities[userId to role].orEmpty()
    }
}
