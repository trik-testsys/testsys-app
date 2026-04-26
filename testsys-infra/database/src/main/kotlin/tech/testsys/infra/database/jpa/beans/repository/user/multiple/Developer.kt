package tech.testsys.infra.database.jpa.beans.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.multiple.DeveloperDataJpaEntity

/**
 * Spring Data repository for [DeveloperDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface DeveloperDataJpaEntityRepository : SequenceJpaEntityRepository<DeveloperDataJpaEntity>
