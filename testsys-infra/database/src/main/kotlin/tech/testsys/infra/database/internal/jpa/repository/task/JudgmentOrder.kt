package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.JudgmentOrderJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [JudgmentOrderJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface JudgmentOrderJpaEntityRepository : SequenceJpaEntityRepository<JudgmentOrderJpaEntity> {

    /**
     * Finds the judgment orders issued for verdicts of the submission [submissionId].
     *
     * @since %CURRENT_VERSION%
     */
    @Query(
        "select jo from JudgmentOrderJpaEntity jo, VerdictJpaEntity v " +
            "where jo.verdictId = v.id and v.submissionId = :submissionId",
    )
    fun findAllBySubmissionId(@Param("submissionId") submissionId: Long): List<JudgmentOrderJpaEntity>

    /**
     * Finds the judgment orders issued by the judge [judgeId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByJudgeId(judgeId: Long): List<JudgmentOrderJpaEntity>
}
