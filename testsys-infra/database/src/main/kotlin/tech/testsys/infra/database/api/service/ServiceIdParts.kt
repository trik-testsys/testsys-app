package tech.testsys.infra.database.api.service

import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.id.SnowflakeIdGenerator
import java.time.Instant

/**
 * Parts of an entity id for the service cabinet: the moment of issue at second resolution, the node id of the instance
 * that issued it and the per-second counter.
 *
 * @since 1.0.0-beta.1
 */
data class ServiceIdParts(val issuedAt: Instant, val nodeId: Int, val counter: Int) {
    companion object {
        /**
         * Decodes [id] by the layout of the entity id generator.
         *
         * @return the parts of [id], or `null` if the generator cannot issue it: the sign bit or a reserved bit is set.
         * @since 1.0.0-beta.1
         */
        @OptIn(InternalDatabaseApi::class)
        fun decode(id: Long): ServiceIdParts? {
            if (id ushr (SnowflakeIdGenerator.TIMESTAMP_SHIFT + SnowflakeIdGenerator.TIMESTAMP_BITS) != 0L) return null
            return ServiceIdParts(
                issuedAt = SnowflakeIdGenerator.instantOf(id),
                nodeId = SnowflakeIdGenerator.nodeIdOf(id),
                counter = SnowflakeIdGenerator.counterOf(id),
            )
        }
    }
}
