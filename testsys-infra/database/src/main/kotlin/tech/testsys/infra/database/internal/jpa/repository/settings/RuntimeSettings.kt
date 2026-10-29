package tech.testsys.infra.database.internal.jpa.repository.settings

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.settings.RuntimeSettingsJpaEntity
import tech.testsys.infra.database.internal.jpa.repository.SnowflakeJpaEntityRepository

/**
 * Repository of the single technical runtime settings row.
 *
 * @since 1.0.0-beta.1
 */
@InternalDatabaseApi
interface RuntimeSettingsJpaEntityRepository : SnowflakeJpaEntityRepository<RuntimeSettingsJpaEntity> {
    /**
     * Reads the set with [setKey], or null before its first write.
     *
     * @since 1.0.0-beta.1
     */
    fun findBySetKey(setKey: String): RuntimeSettingsJpaEntity?
}
