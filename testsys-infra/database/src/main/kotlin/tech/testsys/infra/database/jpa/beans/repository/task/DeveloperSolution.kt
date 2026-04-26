package tech.testsys.infra.database.jpa.beans.repository.task

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.task.DeveloperSolutionJpaEntity

/**
 * Spring Data repository for [DeveloperSolutionJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface DeveloperSolutionJpaEntityRepository : SequenceJpaEntityRepository<DeveloperSolutionJpaEntity>
