package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.JudgmentOrderJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [JudgmentOrderJpaEntity].
 *
 * @since 1.0.0-beta.1
 */
@Repository
@InternalDatabaseApi
interface JudgmentOrderJpaEntityRepository : SnowflakeJpaEntityRepository<JudgmentOrderJpaEntity> {

    /**
     * Finds the judgment orders issued for any of the submissions [submissionIds] in one query.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllBySubmissionIdIn(submissionIds: Collection<Long>): List<JudgmentOrderJpaEntity>

    /**
     * Finds the ids of the judges [judgeIds] paired with the ids of the judgment orders they issued in one query.
     *
     * @since 1.0.0-beta.1
     */
    @Query(
        "select new tech.testsys.infra.database.internal.jpa.repository.LinkedIdRow(e.judgeId, e.id) " +
            "from JudgmentOrderJpaEntity e where e.judgeId in :judgeIds",
    )
    fun findLinkedIdsByJudgeIdIn(@Param("judgeIds") judgeIds: Collection<Long>): List<LinkedIdRow>
}
