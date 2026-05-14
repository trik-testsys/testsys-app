package tech.testsys.infra.database.internal.jpa.repository.user.single

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.single.SupervisorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [SupervisorDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface SupervisorDataJpaEntityRepository : SequenceJpaEntityRepository<SupervisorDataJpaEntity> {

    fun findByUserId(userId: Long): SupervisorDataJpaEntity?
}
