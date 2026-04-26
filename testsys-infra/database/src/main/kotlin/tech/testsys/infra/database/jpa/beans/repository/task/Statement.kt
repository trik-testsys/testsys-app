package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.StatementJpaEntity

/**
 * Spring Data repository for [StatementJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface StatementJpaEntityRepository : SequenceJpaEntityRepository<StatementJpaEntity>
