package tech.testsys.infra.database.jpa.beans.repository.user.single

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.single.ParticipantDataJpaEntity

/**
 * Spring Data repository for [ParticipantDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface ParticipantDataJpaEntityRepository : SequenceJpaEntityRepository<ParticipantDataJpaEntity>
