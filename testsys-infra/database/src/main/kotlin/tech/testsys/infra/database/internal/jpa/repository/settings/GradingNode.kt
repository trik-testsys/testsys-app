package tech.testsys.infra.database.internal.jpa.repository.settings

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.settings.GradingNodeJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Repository of the technical checking node registry records.
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
interface GradingNodeJpaEntityRepository : SnowflakeJpaEntityRepository<GradingNodeJpaEntity> {
    /**
     * Reads the record of [address], or null if the address is not registered.
     *
     * @since 1.0.0-beta.1
     */
    fun findByAddress(address: String): GradingNodeJpaEntity?

    /**
     * Reads all records ordered by address.
     *
     * @since 1.0.0-beta.1
     */
    fun findAllByOrderByAddressAsc(): List<GradingNodeJpaEntity>
}
