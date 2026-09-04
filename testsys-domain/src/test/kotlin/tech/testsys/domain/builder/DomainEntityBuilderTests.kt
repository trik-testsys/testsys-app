package tech.testsys.domain.builder

import org.junit.jupiter.api.Assertions
import kotlin.test.Test
import java.time.Instant
import tech.testsys.domain.model.EntityVersion

abstract class DomainEntityBuilderTests<Entity, Data, DataBuilder : Builder<Data>>(
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
            version = EntityVersion(0)
        }.build()
    }


    private fun buildEntityWithMissingField(data: Data): Entity {
        entityBuilder.data = data
        return entityBuilder.apply {
            id = 42
        }.build()
    }

    @Test
    fun `data builder should create object if all required fields are specified`() {
        Assertions.assertDoesNotThrow { buildDataWithAllFields() }
    }

    @Test
    fun `data builder should throw IllegalArgumentException if any required field is missing`() {
        Assertions.assertThrows(IllegalArgumentException::class.java) {
            buildDataWithMissingFields()
        }
    }

    @Test
    fun `entity builder should create object if all required fields are specified`() {
        for (data in buildDataWithAllFields()) {
            Assertions.assertDoesNotThrow { buildEntityWithAllFields(data) }
        }
    }

    @Test
    fun `entity builder should throw IllegalArgumentException if any required field is missing`() {
        for (data in buildDataWithAllFields()) {
            Assertions.assertThrows(IllegalArgumentException::class.java) {
                buildEntityWithMissingField(data)
            }
        }
    }

}
