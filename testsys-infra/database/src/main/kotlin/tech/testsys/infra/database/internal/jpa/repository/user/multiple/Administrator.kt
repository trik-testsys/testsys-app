package tech.testsys.infra.database.internal.jpa.repository.user.multiple

import org.springframework.stereotype.Repository
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.user.multiple.AdministratorDataJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SequenceJpaEntityRepository

/**
 * Spring Data repository for [AdministratorDataJpaEntity].
 *
 * @since %CURRENT_VERSION%
 */
@Repository
@InternalDatabaseApi
interface AdministratorDataJpaEntityRepository : SequenceJpaEntityRepository<AdministratorDataJpaEntity> {

    fun findByUserId(userId: Long): AdministratorDataJpaEntity?
}
