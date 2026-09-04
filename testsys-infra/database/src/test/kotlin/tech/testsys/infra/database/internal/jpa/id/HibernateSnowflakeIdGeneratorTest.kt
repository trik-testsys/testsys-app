package tech.testsys.infra.database.internal.jpa.id

import org.hibernate.engine.config.spi.ConfigurationService
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext
import org.hibernate.service.ServiceRegistry
import org.hibernate.type.Type
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class HibernateSnowflakeIdGeneratorTest {

    @Test
    fun `reads node id from hibernate settings`() {
        val generator = generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to "42"))

        val id = generator.generate(mock(SharedSessionContractImplementor::class.java), Any()) as Long

        assertEquals(EXPECTED_NODE_ID, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `defaults node id to zero when the setting is absent`() {
        val generator = generator(emptyMap())

        val id = generator.generate(mock(SharedSessionContractImplementor::class.java), Any()) as Long

        assertEquals(0, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `accepts a numeric node id value`() {
        val generator = generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to EXPECTED_NODE_ID))

        val id = generator.generate(mock(SharedSessionContractImplementor::class.java), Any()) as Long

        assertEquals(EXPECTED_NODE_ID, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `trims whitespace around the node id`() {
        val generator = generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to " 42 "))

        val id = generator.generate(mock(SharedSessionContractImplementor::class.java), Any()) as Long

        assertEquals(EXPECTED_NODE_ID, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `rejects a non-numeric node id`() {
        val error = assertFailsWith<IllegalArgumentException> {
            generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to "node-1"))
        }

        assertTrue(
            error.message.orEmpty().contains(HibernateSnowflakeIdGenerator.NODE_ID_SETTING),
            "the message must name the setting, got '${error.message}'",
        )
    }

    @Test
    fun `rejects a blank node id`() {
        assertFailsWith<IllegalArgumentException> {
            generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to ""))
        }
    }

    @Test
    fun `rejects a node id out of range`() {
        assertFailsWith<IllegalArgumentException> {
            generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to (SnowflakeIdGenerator.MAX_NODE_ID + 1).toString()))
        }
    }

    private fun generator(settings: Map<String, Any>): HibernateSnowflakeIdGenerator {
        val configurationService = mock(ConfigurationService::class.java)
        `when`(configurationService.settings).thenReturn(settings)
        val serviceRegistry = mock(ServiceRegistry::class.java)
        `when`(serviceRegistry.requireService(ConfigurationService::class.java)).thenReturn(configurationService)

        val idField = Holder::class.java.getDeclaredField("id")
        val annotation = idField.getAnnotation(SnowflakeId::class.java)
        val generator = HibernateSnowflakeIdGenerator(annotation, idField, mock(CustomIdGeneratorCreationContext::class.java))
        generator.configure(mock(Type::class.java), Properties(), serviceRegistry)
        return generator
    }

    /** Carries a [SnowflakeId]-annotated field so the test can hand Hibernate-shaped arguments to the constructor. */
    @Suppress("unused")
    private class Holder {
        @SnowflakeId
        val id: Long? = null
    }

    private companion object {
        const val EXPECTED_NODE_ID = 42
    }
}
