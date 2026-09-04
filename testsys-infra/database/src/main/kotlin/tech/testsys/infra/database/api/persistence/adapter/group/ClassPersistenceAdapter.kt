package tech.testsys.infra.database.api.persistence.adapter.group

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.ClassJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.group.ClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.ContestToClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.StudentToClassJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.group.ClassMapping
import tech.testsys.infra.database.internal.utils.findByIdOrError
import tech.testsys.infra.database.internal.utils.requireId
import tech.testsys.infra.database.internal.utils.syncJoinTable

/**
 * Persistence adapter of [Class] entities backed by [ClassJpaEntity].
 * Student and contest membership is synced through the join tables on save and update and dropped on remove.
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class ClassPersistenceAdapter(
    jpaEntityRepository: ClassJpaEntityRepository,
    private val studentToClassJpaEntityRepository: StudentToClassJpaEntityRepository,
    private val contestToClassJpaEntityRepository: ContestToClassJpaEntityRepository,
) : AbstractPersistenceAdapter<ClassData, ClassId, Class, ClassJpaEntity>(jpaEntityRepository),
    ClassRepository {

    @Transactional
    override fun save(data: ClassData): Class {
        val jpaEntity = ClassMapping.toJpaEntity(data)
        val saved = jpaEntityRepository.save(jpaEntity)
        val classId = saved.requireId()

        val studentAssociations = ClassMapping.toStudentAssociations(classId, data.students.ids)
        val contestAssociations = ClassMapping.toContestAssociations(classId, data.contests.ids)

        studentToClassJpaEntityRepository.saveAll(studentAssociations)
        contestToClassJpaEntityRepository.saveAll(contestAssociations)

        val domainEntity = ClassMapping.toDomain(saved, data.students.ids, data.contests.ids)
        return domainEntity
    }

    @Transactional
    override fun update(entity: Class): Class {
        val currentJpaEntity = jpaEntityRepository.findByIdOrError(entity.id.value)
        val updatedJpaEntity = ClassMapping.toJpaEntity(entity, currentJpaEntity)
        val savedJpaEntity = jpaEntityRepository.saveAndFlush(updatedJpaEntity)

        val classId = savedJpaEntity.requireId()

        syncStudents(classId, entity.data.students.ids)
        syncContests(classId, entity.data.contests.ids)

        val domainEntity = ClassMapping.toDomain(savedJpaEntity, entity.data.students.ids, entity.data.contests.ids)
        return domainEntity
    }

    @Transactional
    override fun removeById(id: ClassId) {
        val jpaEntity = jpaEntityRepository.findByIdOrNull(id.value) ?: return
        val classId = jpaEntity.requireId()
        studentToClassJpaEntityRepository.deleteAll(studentToClassJpaEntityRepository.findAllByClassId(classId))
        contestToClassJpaEntityRepository.deleteAll(contestToClassJpaEntityRepository.findAllByClassId(classId))
        jpaEntityRepository.delete(jpaEntity)
    }

    @Transactional
    override fun removeByIds(ids: List<ClassId>) = ids.forEach(::removeById)

    override fun assemble(jpaEntity: ClassJpaEntity): Class {
        val classId = jpaEntity.requireId()
        val studentIds = studentToClassJpaEntityRepository.findAllByClassId(classId)
            .map { MultipleRoleUserId(it.id.studentId) }
        val contestIds = contestToClassJpaEntityRepository.findAllByClassId(classId)
            .map { ContestId(it.id.contestId) }

        val domainEntity = ClassMapping.toDomain(jpaEntity, studentIds, contestIds)
        return domainEntity
    }

    private fun syncStudents(classId: Long, target: List<MultipleRoleUserId>) = syncJoinTable(
        existing = studentToClassJpaEntityRepository.findAllByClassId(classId),
        targetKeys = target,
        keyOf = { MultipleRoleUserId(it.id.studentId) },
        buildAssociation = { ClassMapping.toStudentAssociations(classId, listOf(it)).single() },
        deleteAll = { studentToClassJpaEntityRepository.deleteAll(it) },
        saveAll = { studentToClassJpaEntityRepository.saveAll(it) },
    )

    private fun syncContests(classId: Long, target: List<ContestId>) = syncJoinTable(
        existing = contestToClassJpaEntityRepository.findAllByClassId(classId),
        targetKeys = target,
        keyOf = { ContestId(it.id.contestId) },
        buildAssociation = { ClassMapping.toContestAssociations(classId, listOf(it)).single() },
        deleteAll = { contestToClassJpaEntityRepository.deleteAll(it) },
        saveAll = { contestToClassJpaEntityRepository.saveAll(it) },
    )
}
