package tech.testsys.domain.builder

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import kotlin.test.Test
import java.time.Instant
import tech.testsys.domain.model.DomainEntity
import tech.testsys.domain.model.EntityVersion

abstract class DomainEntityBuilderTests<Entity : DomainEntity<*>, Data, DataBuilder : Builder<Data>>(
    private val entityBuilder: DomainEntityWithDataBuilder<Entity, Data, DataBuilder>,
    private val dataBuilder: DataBuilder
) {
    abstract fun buildDataWithAllFields(): List<Data>

    private fun buildDataWithMissingFields(): Data {
        return dataBuilder.build()
    }

    open fun buildEntityWithAllFields(data: Data): Entity {
        entityBuilder.data = data
        return entityBuilder.apply {
            id = 42
            createdAt = Instant.ofEpochSecond(1L)
        }.build()
    }

    private fun buildEntityWithVersion(data: Data, version: EntityVersion): Entity {
        entityBuilder.data = data
        return entityBuilder.apply {
            id = 42
            createdAt = Instant.ofEpochSecond(1L)
            this.version = version
        }.build()
    }


    private fun buildEntityWithMissingField(data: Data): Entity {
        entityBuilder.data = data
        return entityBuilder.apply {
            id = 42
        }.build()
    }

    @Nested
    inner class DataBuilderTests {

        @Test
        fun `should build data if all required fields are specified`() {
            Assertions.assertDoesNotThrow { buildDataWithAllFields() }
        }


        @Test
        fun `should throw IllegalArgumentException if a required data field is missing`() {
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                buildDataWithMissingFields()
            }
        }

    }

    @Nested
    inner class EntityBuilderTests {

        @Test
        fun `should build entity if all required fields are specified`() {
            for (data in buildDataWithAllFields()) {
                Assertions.assertDoesNotThrow { buildEntityWithAllFields(data) }
            }
        }


        @Test
        fun `should leave version null if version is not specified`() {
            val entity = buildEntityWithAllFields(buildDataWithAllFields().first())

            Assertions.assertNull(entity.version)
        }


        @Test
        fun `should set version if version is specified`() {
            val entity = buildEntityWithVersion(buildDataWithAllFields().first(), EntityVersion(7))

            Assertions.assertEquals(EntityVersion(7), entity.version)
        }


        @Test
        fun `should throw IllegalArgumentException if a required entity field is missing`() {
            for (data in buildDataWithAllFields()) {
                Assertions.assertThrows(IllegalArgumentException::class.java) {
                    buildEntityWithMissingField(data)
                }
            }
        }
    }
}
