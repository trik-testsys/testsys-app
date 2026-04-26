package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.SubmissionJpaEntity
import tech.testsys.infra.database.jpa.entity.task.VerdictJpaEntity

/**
 * Spring Data repository for [VerdictJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface VerdictJpaEntityRepository : SequenceJpaEntityRepository<VerdictJpaEntity>

/**
 * Spring Data repository for [SubmissionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface SubmissionJpaEntityRepository : SequenceJpaEntityRepository<SubmissionJpaEntity>
