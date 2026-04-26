package tech.testsys.infra.database.jpa.beans.repository.user.single

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.single.SupervisorDataJpaEntity

/**
 * Spring Data repository for [SupervisorDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface SupervisorDataJpaEntityRepository : SequenceJpaEntityRepository<SupervisorDataJpaEntity>
