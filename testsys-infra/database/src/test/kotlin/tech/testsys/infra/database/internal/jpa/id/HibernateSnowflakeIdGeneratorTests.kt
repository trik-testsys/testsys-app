package tech.testsys.infra.database.internal.jpa.id

import io.mockk.every
import io.mockk.mockk
import org.hibernate.engine.config.spi.ConfigurationService
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.id.factory.spi.CustomIdGeneratorCreationContext
import org.hibernate.service.ServiceRegistry
import org.hibernate.type.Type
import org.junit.jupiter.api.Test
import tech.testsys.infra.database.internal.InternalDatabaseApi
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(InternalDatabaseApi::class)
class HibernateSnowflakeIdGeneratorTests {

    private val session = mockk<SharedSessionContractImplementor>()
    private val configurationService = mockk<ConfigurationService>()
    private val serviceRegistry = mockk<ServiceRegistry>()
    private val creationContext = mockk<CustomIdGeneratorCreationContext>()
    private val type = mockk<Type>()

    @Test
    fun `should read node id from hibernate settings`() {
        val generator = generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to "42"))

        val id = generator.generate(session, Any()) as Long

        assertEquals(EXPECTED_NODE_ID, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `should default node id to zero if the setting is absent`() {
        val generator = generator(emptyMap())

        val id = generator.generate(session, Any()) as Long

        assertEquals(0, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `should accept a numeric node id value`() {
        val generator = generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to EXPECTED_NODE_ID))

        val id = generator.generate(session, Any()) as Long

        assertEquals(EXPECTED_NODE_ID, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `should trim whitespace around the node id`() {
        val generator = generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to " 42 "))

        val id = generator.generate(session, Any()) as Long

        assertEquals(EXPECTED_NODE_ID, SnowflakeIdGenerator.nodeIdOf(id))
    }

    @Test
    fun `should reject a non-numeric node id`() {
        val error = assertFailsWith<IllegalArgumentException> {
            generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to "node-1"))
        }

        assertTrue(
            error.message.orEmpty().contains(HibernateSnowflakeIdGenerator.NODE_ID_SETTING),
            "the message must name the setting, got '${error.message}'",
        )
    }

    @Test
    fun `should reject a blank node id`() {
        assertFailsWith<IllegalArgumentException> {
            generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to ""))
        }
    }

    @Test
    fun `should reject a node id out of range`() {
        assertFailsWith<IllegalArgumentException> {
            generator(mapOf(HibernateSnowflakeIdGenerator.NODE_ID_SETTING to (SnowflakeIdGenerator.MAX_NODE_ID + 1).toString()))
        }
    }

    private fun generator(settings: Map<String, Any>): HibernateSnowflakeIdGenerator {
        every { configurationService.settings } returns settings
        every { serviceRegistry.requireService(ConfigurationService::class.java) } returns configurationService

        val idField = Holder::class.java.getDeclaredField("id")
        val annotation = idField.getAnnotation(SnowflakeId::class.java)
        val generator = HibernateSnowflakeIdGenerator(annotation, idField, creationContext)
        generator.configure(type, Properties(), serviceRegistry)
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
