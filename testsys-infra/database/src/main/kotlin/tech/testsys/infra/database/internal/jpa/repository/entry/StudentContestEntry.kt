package tech.testsys.infra.database.internal.jpa.repository.entry

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.entry.StudentContestEntryJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [StudentContestEntryJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface StudentContestEntryJpaEntityRepository : SnowflakeJpaEntityRepository<StudentContestEntryJpaEntity> {

    /**
     * Finds an entry by its complete context.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByUserIdAndClassIdAndContestId(userId: Long, classId: Long, contestId: Long): StudentContestEntryJpaEntity?

    /**
     * Finds entries for the selected contests and context.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByUserIdAndClassIdAndContestIdIn(userId: Long, classId: Long, contestIds: List<Long>): List<StudentContestEntryJpaEntity>
}
