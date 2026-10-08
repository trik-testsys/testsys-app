package tech.testsys.domain.builder.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassInviteId

class ClassBuilderTests : DomainEntityBuilderTests<Class, ClassData, ClassDataBuilder>(
    ClassBuilder(),
    ClassDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        classData {
            owner(42)
            name = "Class A"
            description = "Class A description"
            invite(30L)
        },
        classData {
            owner(42)
            name = "Class B"
            description = "Class B description"
            students(listOf(1L, 2L))
            contests(listOf(10L, 20L))
            invite(31L)
        },
    )

    @Test
    fun `should reference the invite by id`() {
        assertEquals(ClassInviteId(30), buildDataWithAllFields().first().invite.id)
    }

    @Test
    fun `should throw IllegalArgumentException if the invite is not set`() {
        assertThrows(IllegalArgumentException::class.java) {
            classData {
                owner(42)
                name = "Class A"
                description = "Class A description"
            }
        }
    }
}
