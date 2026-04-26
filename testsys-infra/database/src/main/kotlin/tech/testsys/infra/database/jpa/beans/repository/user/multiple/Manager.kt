package tech.testsys.infra.database.jpa.beans.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.multiple.ManagerDataJpaEntity

/**
 * Spring Data repository for [ManagerDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface ManagerDataJpaEntityRepository : SequenceJpaEntityRepository<ManagerDataJpaEntity>
