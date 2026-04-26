package tech.testsys.infra.database.jpa.beans.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.multiple.AdministratorDataJpaEntity

/**
 * Spring Data repository for [AdministratorDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface AdministratorDataJpaEntityRepository : SequenceJpaEntityRepository<AdministratorDataJpaEntity>
