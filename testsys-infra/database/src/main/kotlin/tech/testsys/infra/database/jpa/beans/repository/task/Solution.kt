package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.SolutionJpaEntity

/**
 * Spring Data repository for [SolutionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface SolutionJpaEntityRepository : SequenceJpaEntityRepository<SolutionJpaEntity>
