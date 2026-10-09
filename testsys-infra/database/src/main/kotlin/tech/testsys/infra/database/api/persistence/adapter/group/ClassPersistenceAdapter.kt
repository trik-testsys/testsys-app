package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.group.ClassInviteJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.ClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.ContestToClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.StudentToClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.ClassInviteMapping
import tech.testsys.infra.database.internal.mapping.group.ClassMapping
import tech.testsys.infra.database.internal.utils.findLinkedIds
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.requireVersion
import tech.testsys.infra.database.internal.utils.syncJoinTable
import java.time.Instant
import org.springframework.data.domain.Sort as JpaSort

/**
 * Persistence adapter of [Class] entities backed by [ClassJpaEntity].
 * Enrolling students also guards their user versions against concurrent removal of the student role.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ClassPersistenceAdapter(
    jpaEntityRepository: ClassJpaEntityRepository,
    private val studentToClassJpaEntityRepository: StudentToClassJpaEntityRepository,
    private val contestToClassJpaEntityRepository: ContestToClassJpaEntityRepository,
    private val classInviteJpaEntityRepository: ClassInviteJpaEntityRepository,
    private val userJpaEntityRepository: UserJpaEntityRepository,
) : AbstractPersistenceAdapter<ClassData, ClassId, Class, ClassJpaEntity>(jpaEntityRepository),
    ClassRepository {

    private val classJpaEntityRepository: ClassJpaEntityRepository = jpaEntityRepository

    @Transactional(readOnly = true)
    override fun findAvailableToManager(ownerId: MultipleRoleUserId, pagination: Pagination, filter: ClassFilter): Page<Class> {
        val specification = Specification<ClassJpaEntity> { entity, _, builder ->
            val predicates = mutableListOf(builder.equal(entity.get<Long>("ownerId"), ownerId.value))
            filter.name?.let { name ->
                predicates.add(builder.gt(builder.locate(builder.lower(entity.get("name")), name.lowercase()), 0))
            }
            filter.createdFrom?.let { lower ->
                predicates.add(builder.greaterThanOrEqualTo(entity.get<Instant>("createdAt"), lower))
            }
            filter.createdTo?.let { upper ->
                predicates.add(builder.lessThanOrEqualTo(entity.get<Instant>("createdAt"), upper))
            }
            builder.and(*predicates.toTypedArray())
        }
        val orders = pagination.sort.orders.map { order ->
            JpaSort.Order(JpaSort.Direction.valueOf(order.direction.name), order.field)
        }
        val stableOrders = if (orders.any { order -> order.property == "id" }) orders else orders + JpaSort.Order.asc("id")
        val pageable = PageRequest.of(pagination.page, pagination.size, JpaSort.by(stableOrders))
        val page = classJpaEntityRepository.findAll(specification, pageable)
        return Page(
            content = assembleAll(page.content),
            pagination = pagination,
            totalElements = page.totalElements,
        )
    }

    @Transactional
    override fun save(data: ClassData): Class {
        val jpaEntity = ClassMapping.toJpaEntity(data)
        val saved = jpaEntityRepository.save(jpaEntity)
        val classId = saved.requireId()

        val studentAssociations = ClassMapping.toStudentAssociations(classId, data.students.ids)
        val contestAssociations = ClassMapping.toContestAssociations(classId, data.contests.ids)

        touchStudents(studentAssociations.map { association -> association.id.studentId })
        studentToClassJpaEntityRepository.saveAll(studentAssociations)
        contestToClassJpaEntityRepository.saveAll(contestAssociations)

        val domainEntity = ClassMapping.toDomain(saved, data.students.ids, data.contests.ids)
        return domainEntity
    }

    // Spring AOP does not intercept the inner save call; the outer transaction makes both rows atomic.
    @Suppress("CallBeanMethodFromSameClass")
    @Transactional
    override fun saveWithInvite(invite: ClassInviteData, data: (ClassInviteId) -> ClassData): Class {
        val savedInvite = classInviteJpaEntityRepository.save(ClassInviteMapping.toJpaEntity(invite))
        return save(data(ClassInviteId(savedInvite.requireId())))
    }

    @Transactional(readOnly = true)
    override fun findByInvite(inviteId: ClassInviteId): Class? =
        classJpaEntityRepository.findByInviteId(inviteId.value)?.let { assemble(it) }

    @Transactional
    override fun update(entity: Class): Class {
        val savedJpaEntity = updateRoot(entity.id.value, entity.requireVersion()) { current ->
            ClassMapping.toJpaEntity(entity, current)
        }

        val classId = savedJpaEntity.requireId()

        syncStudents(classId, entity.data.students.ids)
        syncContests(classId, entity.data.contests.ids)

        val domainEntity = ClassMapping.toDomain(savedJpaEntity, entity.data.students.ids, entity.data.contests.ids)
        return domainEntity
    }

    @Transactional
    override fun addStudent(classId: ClassId, studentId: MultipleRoleUserId): Class {
        val jpaEntity = touchRoot(jpaEntityRepository, classId.value, changesRootData = true)
        // Removing the student role checks the classes of the student after touching the user row, so a concurrent
        // removal and this enrolment conflict on the user version.
        touchRoot(userJpaEntityRepository, studentId.value)
        val association = ClassMapping.toStudentAssociations(classId.value, listOf(studentId)).single()
        if (!studentToClassJpaEntityRepository.existsById(association.id)) {
            studentToClassJpaEntityRepository.saveAndFlush(association)
        }
        return assemble(jpaEntity)
    }

    override fun removeRoot(id: ClassId, expectedVersion: Long?) {
        jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val jpaEntity = touchRoot(jpaEntityRepository, id.value, expectedVersion, changesRootData = true)
        val classId = jpaEntity.requireId()
        studentToClassJpaEntityRepository.deleteAll(studentToClassJpaEntityRepository.findAllByClassId(classId))
        contestToClassJpaEntityRepository.deleteAll(contestToClassJpaEntityRepository.findAllByClassId(classId))
        jpaEntityRepository.delete(jpaEntity)
        jpaEntityRepository.flush()
        classInviteJpaEntityRepository.delete(touchRoot(classInviteJpaEntityRepository, jpaEntity.inviteId, changesRootData = true))
    }

    override fun assembleAll(rows: List<ClassJpaEntity>): List<Class> {
        val classIds = rows.map { row -> row.requireId() }
        val studentIds = findLinkedIds(
            ownerIds = classIds,
            find = studentToClassJpaEntityRepository::findLinkedIdsByClassIdIn,
            ownerIdOf = LinkedIdRow::ownerId,
            linkedIdOf = { link -> MultipleRoleUserId(link.linkedId) },
        )
        val contestIds = findLinkedIds(
            ownerIds = classIds,
            find = contestToClassJpaEntityRepository::findLinkedIdsByClassIdIn,
            ownerIdOf = LinkedIdRow::ownerId,
            linkedIdOf = { link -> ContestId(link.linkedId) },
        )

        return rows.map { row ->
            val classId = row.requireId()
            ClassMapping.toDomain(jpaEntity = row, studentIds = studentIds.getValue(classId), contestIds = contestIds.getValue(classId))
        }
    }

    private fun syncStudents(classId: Long, target: List<MultipleRoleUserId>) = syncJoinTable(
        existing = studentToClassJpaEntityRepository.findAllByClassId(classId),
        targetKeys = target,
        keyOf = { MultipleRoleUserId(it.id.studentId) },
        buildAssociation = { ClassMapping.toStudentAssociations(classId, listOf(it)).single() },
        deleteAll = { studentToClassJpaEntityRepository.deleteAll(it) },
        saveAll = { associations ->
            touchStudents(associations.map { association -> association.id.studentId })
            studentToClassJpaEntityRepository.saveAll(associations)
        },
    )

    /**
     * Loads and guard-writes the users [studentIds] in batches so concurrent removal of their student role conflicts.
     */
    private fun touchStudents(studentIds: List<Long>) =
        touchRoots(userJpaEntityRepository, studentIds, userJpaEntityRepository::incrementVersions)

    private fun syncContests(classId: Long, target: List<ContestId>) = syncJoinTable(
        existing = contestToClassJpaEntityRepository.findAllByClassId(classId),
        targetKeys = target,
        keyOf = { ContestId(it.id.contestId) },
        buildAssociation = { ClassMapping.toContestAssociations(classId, listOf(it)).single() },
        deleteAll = { contestToClassJpaEntityRepository.deleteAll(it) },
        saveAll = { contestToClassJpaEntityRepository.saveAll(it) },
    )
}
