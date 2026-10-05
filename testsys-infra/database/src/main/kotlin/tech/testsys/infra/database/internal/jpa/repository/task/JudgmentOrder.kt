package tech.testsys.infra.database.internal.jpa.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.JudgmentOrderJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Spring Data repository for [JudgmentOrderJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface JudgmentOrderJpaEntityRepository : SnowflakeJpaEntityRepository<JudgmentOrderJpaEntity> {

    /**
     * Finds the judgment orders issued for the submission [submissionId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllBySubmissionId(submissionId: Long): List<JudgmentOrderJpaEntity>

    /**
     * Finds the judgment orders issued by the judge [judgeId].
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByJudgeId(judgeId: Long): List<JudgmentOrderJpaEntity>
}
