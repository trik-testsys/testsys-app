package tech.testsys.infra.database.jpa.beans.repository.user

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.jpa.beans.repository.SequenceJpaEntityRepository
import tech.testsys.infra.database.jpa.entity.user.UserJpaEntity

/**
 * Spring Data repository for [UserJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
interface UserJpaEntityRepository : SequenceJpaEntityRepository<UserJpaEntity>
