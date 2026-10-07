package tech.testsys.infra.database.internal.mapping.entry

import tech.testsys.domain.builder.api.studentContestEntry
import tech.testsys.domain.builder.data
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryData
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.entry.StudentContestEntryJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMapping
import tech.testsys.infra.database.internal.utils.populateFields

/**
 * Mapping between [StudentContestEntry] and [StudentContestEntryJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
object StudentContestEntryMapping : EntityMapping<StudentContestEntry, StudentContestEntryJpaEntity> {

    /**
     * Assembles an entry from [jpaEntity].
     *
     * @since %CURRENT_VERSION%
     */
    fun toDomain(jpaEntity: StudentContestEntryJpaEntity): StudentContestEntry = studentContestEntry {
        populateFields(jpaEntity)
        data {
            user(jpaEntity.userId)
            studyClass(jpaEntity.classId)
            contest(jpaEntity.contestId)
            enteredAt = jpaEntity.enteredAt
        }
    }

    /**
     * Creates a new row from [data].
     *
     * @since %CURRENT_VERSION%
     */
    fun toJpaEntity(data: StudentContestEntryData): StudentContestEntryJpaEntity = StudentContestEntryJpaEntity(
        userId = data.user.id.value,
        classId = data.studyClass.id.value,
        contestId = data.contest.id.value,
        enteredAt = data.enteredAt,
    )
}
