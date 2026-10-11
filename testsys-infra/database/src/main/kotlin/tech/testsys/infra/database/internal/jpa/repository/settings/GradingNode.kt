package tech.testsys.infra.database.internal.jpa.repository.settings

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.settings.GradingNodeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Repository of the technical checking node registry records.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
interface GradingNodeJpaEntityRepository : SnowflakeJpaEntityRepository<GradingNodeJpaEntity> {
    /**
     * Reads the record of [address], or null if the address is not registered.
     *
     * @since %CURRENT_VERSION%
     */
    fun findByAddress(address: String): GradingNodeJpaEntity?

    /**
     * Reads all records ordered by address.
     *
     * @since %CURRENT_VERSION%
     */
    fun findAllByOrderByAddressAsc(): List<GradingNodeJpaEntity>
}
