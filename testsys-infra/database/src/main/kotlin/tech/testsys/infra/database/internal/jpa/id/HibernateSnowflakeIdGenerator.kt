package tech.testsys.infra.database.internal.jpa.id

import org.hibernate.engine.config.spi.ConfigurationService
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.generator.BeforeExecutionGenerator
import org.hibernate.generator.EventType
import org.hibernate.generator.EventTypeSets
import org.hibernate.generator.GeneratorCreationContext
import org.hibernate.id.Configurable
import org.hibernate.service.ServiceRegistry
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.group.CommunityJpaEntity
import java.lang.reflect.Member
import java.util.EnumSet
import java.util.Properties

/**
 * Hibernate [BeforeExecutionGenerator] behind [SnowflakeId], reading [NODE_ID_SETTING] in [configure].
 * It keeps a supplied id only for [CommunityJpaEntity]; other entities receive generated ids.
 *
 * @since %CURRENT_VERSION%
 */
@InternalDatabaseApi
class HibernateSnowflakeIdGenerator(
    annotation: SnowflakeId,
    member: Member,
    context: GeneratorCreationContext,
) : BeforeExecutionGenerator, Configurable {

    @Volatile private lateinit var generator: SnowflakeIdGenerator

    override fun configure(creationContext: GeneratorCreationContext, parameters: Properties) {
        generator = SnowflakeIdGenerator(nodeId = nodeIdFrom(creationContext.serviceRegistry))
    }

    override fun generate(session: SharedSessionContractImplementor, owner: Any, currentValue: Any?, eventType: EventType): Any =
        if (owner is CommunityJpaEntity && currentValue != null) currentValue else generator.next()

    override fun allowAssignedIdentifiers(): Boolean = true

    override fun getEventTypes(): EnumSet<EventType> = EventTypeSets.INSERT_ONLY

    companion object {

        const val NODE_ID_SETTING = "testsys.id.node-id"

        private const val DEFAULT_NODE_ID = 0

        private fun nodeIdFrom(serviceRegistry: ServiceRegistry): Int {
            val raw = serviceRegistry.requireService(ConfigurationService::class.java).settings[NODE_ID_SETTING]
                ?: return DEFAULT_NODE_ID
            return requireNotNull(raw.toString().trim().toIntOrNull()) {
                "Setting $NODE_ID_SETTING must be an integer in 0..${SnowflakeIdGenerator.MAX_NODE_ID}, got '$raw'"
            }
        }
    }
}
