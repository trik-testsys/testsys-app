package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.JudgmentOrderJpaEntity

/**
 * Spring Data repository for [JudgmentOrderJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface JudgmentOrderJpaEntityRepository : SequenceJpaEntityRepository<JudgmentOrderJpaEntity>
