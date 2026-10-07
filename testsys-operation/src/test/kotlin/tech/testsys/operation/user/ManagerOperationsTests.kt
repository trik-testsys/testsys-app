package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.contest
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.multipleRoleUser
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.group.ClassDataBuilder
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.task.Contest
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.MissedManagerRoleError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testMultipleRoleUser
import java.time.Duration
import java.time.Instant

class ManagerOperationsTests {

    private val repository = mockk<ClassRepository>()
    private val multipleRoleUserRepository = mockk<MultipleRoleUserRepository>()
    private val contestRepository = mockk<ContestRepository>()
    private val operations = ManagerOperations(
        classRepository = repository,
        multipleRoleUserRepository = multipleRoleUserRepository,
        contestRepository = contestRepository,
    )

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

    @Nested
    inner class ViewClassTests {

        private val classId = ClassId(11)
        private val manager = testManager { data = managerData {} }

        @Test
        fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) { operations.viewClass(user = user, classId = classId) }

            verify { listOf(repository, multipleRoleUserRepository, contestRepository) wasNot Called }
        }

        @Test
        fun `should raise ClassNotExistsError without loading related data if class does not exist`() {
            every { repository.findById(classId) } returns null

            assertRaises(ClassNotExistsError(classId)) { operations.viewClass(user = manager, classId = classId) }

            verify { listOf(multipleRoleUserRepository, contestRepository) wasNot Called }
        }

        @Test
        fun `should raise ClassAccessDeniedError without loading the membership of another owner`() {
            val foreign = testClass { owner = MultipleRoleUserId(99) }
            every { repository.findById(classId) } returns foreign

            assertRaises(ClassAccessDeniedError(classId)) { operations.viewClass(user = manager, classId = classId) }

            verify { listOf(multipleRoleUserRepository, contestRepository) wasNot Called }
        }

        @Test
        fun `should return owned class details with its students and assigned contest details`() {
            val first = student(id = 1, name = "Alice")
            val second = student(id = 2, name = "Bob")
            val contest = testContest {
                name = "First contest"
                startsAt = Instant.EPOCH
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
            }
            val unscheduled = contest {
                id = 20
                createdAt = Instant.EPOCH
                data = testContest { name = "Second contest" }.data
            }
            val original = testClass { contests(listOf(19, 20)) }
            prepareView(original = original, students = listOf(first, second), contests = listOf(contest, unscheduled))

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertSame(original, actual.studyClass)
            assertEquals(ClassId(11), actual.studyClass.id)
            assertEquals("Viewed class", actual.studyClass.data.name)
            assertEquals(2, actual.studyClass.data.students.ids.size)
            assertEquals(2, actual.students.size)
            assertEquals(listOf(MultipleRoleUserId(1), MultipleRoleUserId(2)), actual.students.map { it.id })
            assertEquals(listOf("Alice", "Bob"), actual.students.map { it.data.name })
            assertEquals(2, actual.contests.size)
            val returnedContests = actual.contests.associateBy { it.id }
            val scheduled = returnedContests.getValue(ContestId(19))
            val secondContest = returnedContests.getValue(ContestId(20))
            assertEquals("First contest", scheduled.data.name)
            assertEquals(Instant.EPOCH, scheduled.data.startsAt)
            assertEquals(Instant.ofEpochSecond(7200), scheduled.data.endsAt)
            assertEquals(Duration.ofMinutes(30), scheduled.data.attemptDuration)
            assertEquals("Second contest", secondContest.data.name)
            assertNull(secondContest.data.startsAt)
            assertNull(secondContest.data.endsAt)
            assertNull(secondContest.data.attemptDuration)
        }

        @Test
        fun `should return lists that can be read without further storage calls`() {
            val original = testClass { contests(listOf(19)) }
            prepareView(
                original = original,
                students = listOf(student(id = 1, name = "Alice"), student(id = 2, name = "Bob")),
                contests = listOf(testContest()),
            )

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            clearMocks(repository, multipleRoleUserRepository, contestRepository)
            assertEquals(listOf("Alice", "Bob"), actual.students.map { it.data.name })
            assertEquals(listOf("Contest"), actual.contests.map { it.data.name })
            verify { listOf(repository, multipleRoleUserRepository, contestRepository) wasNot Called }
        }

        @Test
        fun `should return empty lists if no students or contests are assigned`() {
            val original = testClass { students(emptyList()) }
            prepareView(original)

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertEquals(emptyList<MultipleRoleUser>(), actual.students)
            assertEquals(emptyList<Contest>(), actual.contests)
            assertEquals(0, actual.studyClass.data.students.ids.size)
        }

        @Test
        fun `should preserve absent contest dates and individual time limit`() {
            val original = testClass {
                students(emptyList())
                contests(listOf(19))
            }
            prepareView(original = original, contests = listOf(testContest()))

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertNull(actual.contests.single().data.startsAt)
            assertNull(actual.contests.single().data.endsAt)
            assertNull(actual.contests.single().data.attemptDuration)
        }

        @Test
        fun `should preserve a contest start and individual limit when no end is set`() {
            val original = testClass {
                students(emptyList())
                contests(listOf(19))
            }
            val contest = testContest {
                startsAt = Instant.EPOCH
                attemptDuration = Duration.ofMinutes(15)
            }
            prepareView(original = original, contests = listOf(contest))

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertEquals(Instant.EPOCH, actual.contests.single().data.startsAt)
            assertNull(actual.contests.single().data.endsAt)
            assertEquals(Duration.ofMinutes(15), actual.contests.single().data.attemptDuration)
        }

        @Test
        fun `should allow a Manager who also has other roles`() {
            val user = testMultipleRoleUser {
                roles {
                    manager { data = managerData {} }
                    student { data = studentData {} }
                    administrator {}
                }
            }
            val original = testClass { students(emptyList()) }
            prepareView(original)

            val actual = operations.viewClass(user = user, classId = classId).getOrThrow()

            assertSame(original, actual.studyClass)
        }

        @Test
        fun `should allow an owned class missing from the Manager class snapshot`() {
            val user = testManager { data = managerData { classes(listOf(99)) } }
            val original = testClass { students(emptyList()) }
            prepareView(original)

            val actual = operations.viewClass(user = user, classId = classId).getOrThrow()

            assertSame(original, actual.studyClass)
        }

        @Test
        fun `should deny a foreign class incorrectly included in the Manager class snapshot`() {
            val user = testManager { data = managerData { classes(listOf(11)) } }
            val foreign = testClass { owner = MultipleRoleUserId(99) }
            every { repository.findById(classId) } returns foreign

            assertRaises(ClassAccessDeniedError(classId)) { operations.viewClass(user = user, classId = classId) }

            verify { listOf(multipleRoleUserRepository, contestRepository) wasNot Called }
        }

        @Test
        fun `should return an enrolled user regardless of their current Student role`() {
            val enrolled = testAdministrator {}
            val original = testClass { students(listOf(0)) }
            prepareView(original = original, students = listOf(enrolled))

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertEquals(1, actual.students.size)
            assertSame(enrolled, actual.students.single())
        }

        @Test
        fun `should include assigned contests no longer shared to the Manager communities`() {
            val user = testManager {
                memberOf(listOf(4))
                data = managerData {}
            }
            val assigned = testContest {
                owner = MultipleRoleUserId(99)
                sharedTo(listOf(5))
            }
            val original = testClass {
                students(emptyList())
                contests(listOf(19))
            }
            prepareView(original = original, contests = listOf(assigned))

            val actual = operations.viewClass(user = user, classId = classId).getOrThrow()

            assertEquals(1, actual.contests.size)
            assertSame(assigned, actual.contests.single())
        }

        @Test
        fun `should preserve stored class data and version without writing any entities`() {
            val first = student(id = 1, name = "Alice")
            val second = student(id = 2, name = "Bob")
            val assigned = testContest()
            val original = testClass { contests(listOf(19)) }
            prepareView(original = original, students = listOf(first, second), contests = listOf(assigned))

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertSame(original, actual.studyClass)
            assertSame(original.data, actual.studyClass.data)
            assertEquals(MultipleRoleUserId(0), actual.studyClass.data.owner.id)
            assertEquals("Viewed class", actual.studyClass.data.name)
            assertEquals("Class description", actual.studyClass.data.description)
            assertEquals(Instant.EPOCH, actual.studyClass.createdAt)
            assertEquals(listOf(MultipleRoleUserId(1), MultipleRoleUserId(2)), actual.studyClass.data.students.ids)
            assertEquals(listOf(ContestId(19)), actual.studyClass.data.contests.ids)
            assertEquals(0L, actual.studyClass.version?.value)
            assertSame(first, actual.students[0])
            assertSame(second, actual.students[1])
            assertSame(assigned, actual.contests.single())
            verify {
                repository.findById(classId)
                multipleRoleUserRepository.load(original.data.students)
                contestRepository.load(original.data.contests)
            }
            confirmVerified(repository, multipleRoleUserRepository, contestRepository)
        }

        @Test
        fun `should propagate a technical exception when reading the class`() {
            val failure = IllegalStateException("Class storage unavailable")
            every { repository.findById(classId) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewClass(user = manager, classId = classId)
            }

            assertSame(failure, actual)
        }

        @Test
        fun `should propagate a technical exception when loading enrolled students`() {
            val original = testClass()
            val failure = IllegalStateException("User storage unavailable")
            every { repository.findById(classId) } returns original
            every { multipleRoleUserRepository.load(original.data.students) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewClass(user = manager, classId = classId)
            }

            assertSame(failure, actual)
        }

        @Test
        fun `should propagate a technical exception when loading assigned contests`() {
            val original = testClass {
                students(emptyList())
                contests(listOf(19))
            }
            val failure = IllegalStateException("Contest storage unavailable")
            every { repository.findById(classId) } returns original
            every { multipleRoleUserRepository.load(original.data.students) } returns emptyList()
            every { contestRepository.load(original.data.contests) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewClass(user = manager, classId = classId)
            }

            assertSame(failure, actual)
        }

        @Test
        fun `should preserve the loader failure if an enrolled student is missing`() {
            val original = testClass()
            val failure = IllegalArgumentException("User 1 does not exist")
            every { repository.findById(classId) } returns original
            every { multipleRoleUserRepository.load(original.data.students) } throws failure

            val actual = assertThrows(IllegalArgumentException::class.java) {
                operations.viewClass(user = manager, classId = classId)
            }

            assertSame(failure, actual)
        }

        @Test
        fun `should preserve the loader failure if an assigned contest is missing`() {
            val original = testClass {
                students(emptyList())
                contests(listOf(19))
            }
            val failure = IllegalArgumentException("Contest 19 does not exist")
            every { repository.findById(classId) } returns original
            every { multipleRoleUserRepository.load(original.data.students) } returns emptyList()
            every { contestRepository.load(original.data.contests) } throws failure

            val actual = assertThrows(IllegalArgumentException::class.java) {
                operations.viewClass(user = manager, classId = classId)
            }

            assertSame(failure, actual)
        }

        private fun prepareView(original: Class, students: List<MultipleRoleUser> = emptyList(), contests: List<Contest> = emptyList()) {
            every { repository.findById(classId) } returns original
            every { multipleRoleUserRepository.load(original.data.students) } returns students
            every { contestRepository.load(original.data.contests) } returns contests
        }

        private fun student(id: Long, name: String): MultipleRoleUser = multipleRoleUser {
            this.id = id
            createdAt = Instant.EPOCH
            version = EntityVersion(0)
            data = testMultipleRoleUser {
                this.name = name
                roles { student { data = studentData {} } }
            }.data
        }
    }

    private fun testClass(builder: ClassDataBuilder.() -> Unit = {}): Class = `class` {
        id = 11
        createdAt = Instant.EPOCH
        version = EntityVersion(0)
        data = classData {
            owner = MultipleRoleUserId(0)
            name = "Viewed class"
            description = "Class description"
            students(listOf(1, 2))
            builder()
        }
    }
}
