package tech.testsys.infra.database.jpa.beans.repository.group

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.group.CommunityJpaEntity

/**
 * Spring Data repository for [CommunityJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface CommunityJpaEntityRepository : SequenceJpaEntityRepository<CommunityJpaEntity>
