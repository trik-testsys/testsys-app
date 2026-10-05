package tech.testsys.infra.database.internal.mapping.task

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.api.contestData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.task.ContestData
import tech.testsys.domain.model.task.TrikStudioVersion
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.entity.task.ContestJpaEntity
import tech.testsys.infra.database.internal.mapping.EntityMappingTests
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@InternalDatabaseApi
class ContestMappingTests : EntityMappingTests<ContestMapping>() {

    override val mapping = ContestMapping

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun `should map absent and finite limits in both directions`(hasTotal: Boolean, hasAttempt: Boolean) {
        val total = Duration.ofMillis(1234).takeIf { hasTotal }
        val attempt = Duration.ofMillis(567).takeIf { hasAttempt }
        val data = dataWithLimits(total = total, attempt = attempt)

        val row = withId(mapping.toJpaEntity(data = data, trikStudioVersionId = 3))

        val restored = mapping.toDomain(row, TrikStudioVersion("3.0.0"), emptyList(), emptyList())
        assertEquals(1234L.takeIf { hasTotal }, row.contestDurationMillis)
        assertEquals(567L.takeIf { hasAttempt }, row.attemptDurationMillis)
        assertEquals(total, restored.data.contestDuration)
        assertEquals(attempt, restored.data.attemptDuration)
    }

    @ParameterizedTest
    @CsvSource("false,false", "false,true", "true,false", "true,true")
    fun `should map updates with absent and finite limits`(hasTotal: Boolean, hasAttempt: Boolean) {
        val total = Duration.ofMillis(1234).takeIf { hasTotal }
        val attempt = Duration.ofMillis(567).takeIf { hasAttempt }
        val entity = contest {
            id = 4
            createdAt = Instant.EPOCH
            version = EntityVersion(7)
            data = dataWithLimits(total = total, attempt = attempt)
        }
        val current = mapping.toJpaEntity(data = dataWithLimits(), trikStudioVersionId = 3)

        val row = mapping.toJpaEntity(entity = entity, current = current, trikStudioVersionId = 3)

        assertEquals(1234L.takeIf { hasTotal }, row.contestDurationMillis)
        assertEquals(567L.takeIf { hasAttempt }, row.attemptDurationMillis)
        assertEquals(7L, row.version)
        assertEquals(current.createdAt, row.createdAt)
    }

    @ParameterizedTest
    @ValueSource(longs = [Long.MIN_VALUE, Long.MAX_VALUE])
    fun `should preserve both Long millisecond boundaries`(millis: Long) {
        val duration = Duration.ofMillis(millis)
        val data = dataWithLimits(total = duration, attempt = duration)

        val row = withId(mapping.toJpaEntity(data = data, trikStudioVersionId = 3))

        val restored = mapping.toDomain(row, TrikStudioVersion("3.0.0"), emptyList(), emptyList())
        assertEquals(millis, row.contestDurationMillis)
        assertEquals(millis, row.attemptDurationMillis)
        assertEquals(duration, restored.data.contestDuration)
        assertEquals(duration, restored.data.attemptDuration)
    }

    @ParameterizedTest
    @CsvSource(
        "true,PT0.000000001S",
        "false,PT0.000000001S",
        "true,PT-0.000000001S",
        "false,PT-0.000000001S",
        "true,PT9223372036854775.808S",
        "false,PT9223372036854775.808S",
        "true,PT-9223372036854775.809S",
        "false,PT-9223372036854775.809S",
    )
    fun `should reject inexact or overflowing limits when mapping new rows`(isTotal: Boolean, value: String) {
        val duration = Duration.parse(value)
        val data = dataWithLimits(total = duration.takeIf { isTotal }, attempt = duration.takeUnless { isTotal })

        assertFailsWith<IllegalArgumentException> { mapping.toJpaEntity(data = data, trikStudioVersionId = 3) }
    }

    @ParameterizedTest
    @CsvSource(
        "true,PT0.000000001S",
        "false,PT0.000000001S",
        "true,PT9223372036854775.808S",
        "false,PT9223372036854775.808S",
    )
    fun `should reject inexact or overflowing limits when mapping updates`(isTotal: Boolean, value: String) {
        val duration = Duration.parse(value)
        val current: ContestJpaEntity = mapping.toJpaEntity(data = dataWithLimits(), trikStudioVersionId = 3)
        val entity = contest {
            id = 4
            createdAt = Instant.EPOCH
            version = EntityVersion(7)
            data = dataWithLimits()
        }.withData {
            contestDuration = duration.takeIf { isTotal }
            attemptDuration = duration.takeUnless { isTotal }
        }

        assertFailsWith<IllegalArgumentException> {
            mapping.toJpaEntity(entity = entity, current = current, trikStudioVersionId = 3)
        }
    }

    private fun dataWithLimits(total: Duration? = null, attempt: Duration? = null): ContestData = contestData {
        owner(1)
        name = "Contest"
        description = "Description"
        trikStudioVersion("3.0.0")
        contestDuration = total
        attemptDuration = attempt
    }

    private fun withId(row: ContestJpaEntity): ContestJpaEntity = ContestJpaEntity(
        id = 4,
        name = row.name,
        description = row.description,
        ownerId = row.ownerId,
        startsAt = row.startsAt,
        contestDurationMillis = row.contestDurationMillis,
        attemptDurationMillis = row.attemptDurationMillis,
        trikStudioVersionId = row.trikStudioVersionId,
    )
}
