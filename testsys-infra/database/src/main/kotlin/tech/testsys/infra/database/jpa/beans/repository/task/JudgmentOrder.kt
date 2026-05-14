package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.JudgmentOrderJpaEntity

/**
 * Spring Data repository for [JudgmentOrderJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface JudgmentOrderJpaEntityRepository : SequenceJpaEntityRepository<JudgmentOrderJpaEntity> {

    @Query(
        "select jo from JudgmentOrderJpaEntity jo, VerdictJpaEntity v " +
            "where jo.verdictId = v.id and v.submissionId = :submissionId",
    )
    fun findAllBySubmissionId(@Param("submissionId") submissionId: Long): List<JudgmentOrderJpaEntity>
}
