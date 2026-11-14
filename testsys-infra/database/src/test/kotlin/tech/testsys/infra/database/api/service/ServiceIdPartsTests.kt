package tech.testsys.infra.database.api.service

import org.junit.jupiter.api.Test
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.id.SnowflakeIdGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(InternalDatabaseApi::class)
class ServiceIdPartsTests {

    @Test
    fun `should decode an issued id into its moment, node id and counter`() {
        val issuedAt = Instant.parse("2026-10-30T12:34:56Z")
        val generator = SnowflakeIdGenerator(nodeId = 7, clock = Clock.fixed(issuedAt, ZoneOffset.UTC))
        generator.next()

        val parts = ServiceIdParts.decode(generator.next())

        assertEquals(ServiceIdParts(issuedAt = issuedAt, nodeId = 7, counter = 1), parts)
    }

    @Test
    fun `should decode the largest id the generator can issue`() {
        val id = (SnowflakeIdGenerator.MAX_TIMESTAMP shl SnowflakeIdGenerator.TIMESTAMP_SHIFT) or
            (SnowflakeIdGenerator.MAX_NODE_ID.toLong() shl SnowflakeIdGenerator.NODE_ID_SHIFT) or
            SnowflakeIdGenerator.MAX_COUNTER.toLong()

        val parts = ServiceIdParts.decode(id)

        assertEquals(
            ServiceIdParts(
                issuedAt = Instant.ofEpochSecond(SnowflakeIdGenerator.MAX_TIMESTAMP),
                nodeId = SnowflakeIdGenerator.MAX_NODE_ID,
                counter = SnowflakeIdGenerator.MAX_COUNTER,
            ),
            parts,
        )
    }

    @Test
    fun `should not decode an id with the sign bit or a reserved bit set`() {
        assertNull(ServiceIdParts.decode(-1))
        assertNull(ServiceIdParts.decode(1L shl (SnowflakeIdGenerator.TIMESTAMP_SHIFT + SnowflakeIdGenerator.TIMESTAMP_BITS)))
    }
}
