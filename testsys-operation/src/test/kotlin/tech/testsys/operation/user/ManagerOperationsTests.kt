package tech.testsys.operation.user

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.operation.error.MissedManagerRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testMultipleRoleUser
import java.time.Instant

class ManagerOperationsTests {

    private val repository = mockk<ClassRepository>()
    private val operations = ManagerOperations(classRepository = repository)

    @Nested
    inner class ViewClassesTests {

        private val pagination = Pagination(page = 0, size = 10)
        private val manager = testManager { data = managerData {} }

        @Test
        fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) { operations.viewClasses(user = user, pagination = pagination) }

            verify(exactly = 0) { repository.findAvailableToManager(any(), any(), any()) }
        }

        @Test
        fun `should allow a Manager who also has other roles`() {
            val user = testMultipleRoleUser {
                roles {
                    student { data = studentData {} }
                    manager { data = managerData {} }
                    administrator {}
                }
            }
            val expected = Page(content = listOf(testClass()), pagination = pagination, totalElements = 1)
            every { repository.findAvailableToManager(ownerId = user.id, pagination = pagination) } returns expected

            val actual = operations.viewClasses(user = user, pagination = pagination).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should return class details and student count without saving changes`() {
            val original = testClass()
            val expected = Page(content = listOf(original), pagination = pagination, totalElements = 1)
            every { repository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } returns expected

            val actual = operations.viewClasses(user = manager, pagination = pagination).getOrThrow()

            assertSame(expected, actual)
            assertSame(original, actual.content.single())
            assertEquals(ClassId(11), actual.content.single().id)
            assertEquals("Viewed class", actual.content.single().data.name)
            assertEquals(2, actual.content.single().data.students.ids.size)
            assertEquals(EntityVersion(0), actual.content.single().version)
            verify(exactly = 0) { repository.save(any<ClassData>()) }
            verify(exactly = 0) { repository.update(any<Class>()) }
            verify(exactly = 0) { repository.removeById(any()) }
            verify(exactly = 0) { repository.removeByIds(any()) }
        }

        @Test
        fun `should query current classes independently of the Manager class snapshot`() {
            val user = testManager { data = managerData { classes(listOf(99)) } }
            val original = testClass()
            every { repository.findAvailableToManager(ownerId = user.id, pagination = pagination) } returns
                Page(content = listOf(original), pagination = pagination, totalElements = 1)

            val actual = operations.viewClasses(user = user, pagination = pagination).getOrThrow()

            assertEquals(listOf(original), actual.content)
        }

        @ParameterizedTest
        @CsvSource("0,0", "3,2")
        fun `should preserve an empty page and total for absent matches or a page beyond the end`(index: Int, total: Long) {
            val request = Pagination(page = index, size = 1)
            val expected = Page<Class>(content = emptyList(), pagination = request, totalElements = total)
            every { repository.findAvailableToManager(ownerId = manager.id, pagination = request) } returns expected

            val actual = operations.viewClasses(user = manager, pagination = request).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should forward pagination and every filter unchanged`() {
            val request =
                Pagination(page = 3, size = 2, sort = Sort(listOf(Sort.Order(field = "createdAt", direction = Sort.Direction.DESC))))
            val filter =
                ClassFilter(name = "  Alpha%_  ", createdFrom = Instant.EPOCH, createdTo = Instant.ofEpochSecond(20))
            val expected = Page(content = listOf(testClass()), pagination = request, totalElements = 7)
            every { repository.findAvailableToManager(ownerId = manager.id, pagination = request, filter = filter) } returns expected

            val actual = operations.viewClasses(user = manager, pagination = request, filter = filter).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should propagate a technical storage exception when listing classes`() {
            val failure = IllegalStateException("Class storage unavailable")
            every { repository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewClasses(user = manager, pagination = pagination)
            }

            assertSame(failure, actual)
        }
    }

    private fun testClass(): Class = `class` {
        id = 11
        createdAt = Instant.EPOCH
        version = EntityVersion(0)
        data = classData {
            owner = MultipleRoleUserId(0)
            name = "Viewed class"
            description = "Class description"
            students(listOf(1, 2))
        }
    }
}
