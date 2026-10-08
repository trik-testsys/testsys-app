package tech.testsys.infra.database.api.persistence.adapter.entry

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryData
import tech.testsys.domain.model.entry.StudentContestEntryId
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.AbstractPersistenceAdapter
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.entry.StudentContestEntryJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.entry.StudentContestEntryJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.user.UserJpaEntityRepository
import tech.testsys.infra.database.internal.mapping.entry.StudentContestEntryMapping

/**
 * Persistence adapter of [StudentContestEntry] entities backed by [StudentContestEntryJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Component
@OptIn(InternalDatabaseApi::class)
class StudentContestEntryPersistenceAdapter(
    jpaEntityRepository: StudentContestEntryJpaEntityRepository,
    private val users: UserJpaEntityRepository,
) : AbstractPersistenceAdapter<
    StudentContestEntryData,
    StudentContestEntryId,
    StudentContestEntry,
    StudentContestEntryJpaEntity,
    >(jpaEntityRepository),
    StudentContestEntryRepository {

    private val entries = jpaEntityRepository

    @Transactional
    override fun save(data: StudentContestEntryData): StudentContestEntry =
        assemble(entries.save(StudentContestEntryMapping.toJpaEntity(data)))

    override fun update(entity: StudentContestEntry): StudentContestEntry = throw UnsupportedOperationException(
        "studentContestEntry ${entity.id.value} cannot be updated: every entry field is fixed on creation",
    )

    @Transactional(readOnly = true)
    override fun findByContext(userId: MultipleRoleUserId, studyClassId: ClassId, contestId: ContestId): StudentContestEntry? =
        entries.findByUserIdAndClassIdAndContestId(
            userId = userId.value,
            classId = studyClassId.value,
            contestId = contestId.value,
        )?.let { assemble(it) }

    @Transactional(readOnly = true)
    override fun findByContests(
        userId: MultipleRoleUserId,
        studyClassId: ClassId,
        contestIds: List<ContestId>,
    ): List<StudentContestEntry> {
        if (contestIds.isEmpty()) return emptyList()
        val rows = entries.findAllByUserIdAndClassIdAndContestIdIn(
            userId = userId.value,
            classId = studyClassId.value,
            contestIds = contestIds.map { it.value },
        )
        return assembleAll(rows)
    }

    @Transactional
    override fun findOrCreate(data: StudentContestEntryData): StudentContestEntry {
        requireNotNull(users.lockById(data.user.id.value)) {
            "user ${data.user.id.value} does not exist for studentContestEntry"
        }
        val existing = findByContext(
            userId = data.user.id,
            studyClassId = data.studyClass.id,
            contestId = data.contest.id,
        )
        return existing ?: save(data)
    }

    override fun assembleAll(rows: List<StudentContestEntryJpaEntity>): List<StudentContestEntry> =
        rows.map { row -> StudentContestEntryMapping.toDomain(row) }
}
