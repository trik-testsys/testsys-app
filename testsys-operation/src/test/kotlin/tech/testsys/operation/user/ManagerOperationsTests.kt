package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.confirmVerified
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
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.competition
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.group.ClassDataBuilder
import tech.testsys.domain.builder.group.CompetitionDataBuilder
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.Manager
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNameBlankError
import tech.testsys.operation.error.ClassNameTooLongError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionAccessDeniedError
import tech.testsys.operation.error.CompetitionNameBlankError
import tech.testsys.operation.error.CompetitionNameTooLongError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.ContestNotAddedToClassError
import tech.testsys.operation.error.ContestNotAddedToCompetitionError
import tech.testsys.operation.error.ContestNotExistsError
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
    private val competitionRepository = mockk<CompetitionRepository>()
    private val contestRepository = mockk<ContestRepository>()
    private val submissionRepository = mockk<SubmissionRepository>()
    private val operations = ManagerOperations(
        classRepository = repository,
        competitionRepository = competitionRepository,
        contestRepository = contestRepository,
        submissionRepository = submissionRepository,
    )

    @Nested
    inner class CreateClassTests {

        private val manager = testManager { data = managerData {} }

        @ParameterizedTest
        @ValueSource(strings = ["", "Valid name", "a"])
        fun `should raise MissedManagerRoleError before saving or validating the name if user is not a Manager`(name: String) {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) { operations.createClass(user = user, className = name) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should create an owned empty class and save it exactly once`() {
            prepareSaving()

            val actual = operations.createClass(user = manager, className = "New class").getOrThrow()

            assertEquals(manager.id, actual.data.owner.id)
            assertEquals("New class", actual.data.name)
            assertEquals("", actual.data.description)
            assertEquals(emptyList<MultipleRoleUserId>(), actual.data.students.ids)
            assertEquals(emptyList<ContestId>(), actual.data.contests.ids)
            verify(exactly = 1) {
                repository.save(
                    match<ClassData> { data ->
                        data.owner.id == manager.id && data.name == "New class" && data.description == "" &&
                            data.students.ids.isEmpty() && data.contests.ids.isEmpty()
                    },
                )
            }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should return the stored class with its identity timestamp and version unchanged`() {
            val stored = testClass { name = "New class" }
            every { repository.save(any<ClassData>()) } returns stored

            val actual = operations.createClass(user = manager, className = "New class").getOrThrow()

            assertSame(stored, actual)
            assertEquals(ClassId(11), actual.id)
            assertEquals(Instant.EPOCH, actual.createdAt)
            assertEquals(0L, actual.version?.value)
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
            prepareSaving()

            val actual = operations.createClass(user = user, className = "New class").getOrThrow()

            assertEquals(user.id, actual.data.owner.id)
            assertEquals("New class", actual.data.name)
        }

        @ParameterizedTest
        @ValueSource(strings = ["  Mixed CASE  ", "\tA\n", "é", "😀"])
        fun `should preserve a nonblank name including its whitespace case and Unicode characters`(name: String) {
            prepareSaving()

            val actual = operations.createClass(user = manager, className = name).getOrThrow()

            assertEquals(name, actual.data.name)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "\t", "\n", " \t\r\n", "\u00A0"])
        fun `should raise ClassNameBlankError without saving if the name is blank`(name: String) {
            assertRaises(ClassNameBlankError) { operations.createClass(user = manager, className = name) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @ParameterizedTest
        @ValueSource(strings = ["a", "😀"])
        fun `should accept a name with exactly 255 Unicode code points`(symbol: String) {
            val name = symbol.repeat(255)
            prepareSaving()

            val actual = operations.createClass(user = manager, className = name).getOrThrow()

            assertEquals(name, actual.data.name)
        }

        @ParameterizedTest
        @ValueSource(strings = ["a", "😀"])
        fun `should raise ClassNameTooLongError without saving if the name has 256 Unicode code points`(symbol: String) {
            val name = symbol.repeat(256)

            assertRaises(ClassNameTooLongError(name)) { operations.createClass(user = manager, className = name) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should allow duplicate names without changing the existing class`() {
            val first = testClass {
                name = "Same name"
                students(emptyList())
            }
            val second = `class` {
                id = 12
                createdAt = Instant.EPOCH
                version = EntityVersion(0)
                data = first.data
            }
            every { repository.save(any<ClassData>()) } returnsMany listOf(first, second)
            val existing = operations.createClass(user = manager, className = "Same name").getOrThrow()
            val existingData = existing.data

            val actual = operations.createClass(user = manager, className = "Same name").getOrThrow()

            assertSame(second, actual)
            assertEquals(ClassId(12), actual.id)
            assertEquals("Same name", actual.data.name)
            assertSame(existingData, existing.data)
            assertEquals(ClassId(11), existing.id)
            verify(exactly = 2) { repository.save(match<ClassData> { data -> data.name == "Same name" }) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should preserve the user and Manager class snapshot without saving other entities`() {
            val user = testManager { data = managerData { classes(listOf(99)) } }
            val userData = user.data
            val managerRole = user.data.roles.filterIsInstance<Manager>().single()
            prepareSaving()

            val actual = operations.createClass(user = user, className = "New class").getOrThrow()

            assertEquals(user.id, actual.data.owner.id)
            assertSame(userData, user.data)
            assertSame(managerRole, user.data.roles.filterIsInstance<Manager>().single())
            assertEquals(listOf(ClassId(99)), managerRole.data.classes.ids)
            assertEquals(0L, user.version?.value)
            verify(exactly = 1) { repository.save(any<ClassData>()) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should propagate a technical storage exception when saving the class`() {
            val failure = IllegalStateException("Class storage unavailable")
            every { repository.save(any<ClassData>()) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.createClass(user = manager, className = "New class")
            }

            assertSame(failure, actual)
        }

        private fun prepareSaving() {
            every { repository.save(any<ClassData>()) } answers {
                `class` {
                    id = 12
                    createdAt = Instant.EPOCH
                    version = EntityVersion(0)
                    data = firstArg<ClassData>()
                }
            }
        }
    }

    @Nested
    inner class CreateCompetitionTests {

        private val manager = testManager { data = managerData {} }

        @ParameterizedTest
        @ValueSource(strings = ["", "Valid name", "a"])
        fun `should raise MissedManagerRoleError before saving or validating the name if user is not a Manager`(name: String) {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) { operations.createCompetition(user = user, competitionName = name) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should raise MissedManagerRoleError before validating a name with 256 Unicode code points`() {
            val user = testAdministrator {}
            val name = "a".repeat(256)

            assertRaises(MissedManagerRoleError) { operations.createCompetition(user = user, competitionName = name) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should create an owned empty competition and save it exactly once`() {
            prepareSaving()

            val actual = operations.createCompetition(user = manager, competitionName = "New competition").getOrThrow()

            assertEquals(manager.id, actual.data.owner.id)
            assertEquals("New competition", actual.data.name)
            assertEquals("", actual.data.description)
            assertEquals(emptyList<SingleRoleUserId>(), actual.data.participants.ids)
            assertEquals(emptyList<ContestId>(), actual.data.contests.ids)
            verify(exactly = 1) {
                competitionRepository.save(
                    match<CompetitionData> { data ->
                        data.owner.id == manager.id && data.name == "New competition" && data.description == "" &&
                            data.participants.ids.isEmpty() && data.contests.ids.isEmpty()
                    },
                )
            }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should return the stored competition with its identity timestamp and version unchanged`() {
            val stored = testCompetition { name = "New competition" }
            every { competitionRepository.save(any<CompetitionData>()) } returns stored

            val actual = operations.createCompetition(user = manager, competitionName = "New competition").getOrThrow()

            assertSame(stored, actual)
            assertEquals(CompetitionId(21), actual.id)
            assertEquals(Instant.EPOCH, actual.createdAt)
            assertEquals(0L, actual.version?.value)
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
            prepareSaving()

            val actual = operations.createCompetition(user = user, competitionName = "New competition").getOrThrow()

            assertEquals(user.id, actual.data.owner.id)
            assertEquals("New competition", actual.data.name)
        }

        @ParameterizedTest
        @ValueSource(strings = ["  Mixed CASE  ", "\tA\n", "é", "😀"])
        fun `should preserve a nonblank name including its whitespace case and Unicode characters`(name: String) {
            prepareSaving()

            val actual = operations.createCompetition(user = manager, competitionName = name).getOrThrow()

            assertEquals(name, actual.data.name)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "\t", "\n", " \t\r\n", "\u00A0"])
        fun `should raise CompetitionNameBlankError without saving if the name is blank`(name: String) {
            assertRaises(CompetitionNameBlankError) { operations.createCompetition(user = manager, competitionName = name) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @ParameterizedTest
        @ValueSource(strings = ["a", "😀"])
        fun `should accept a name with exactly 255 Unicode code points`(symbol: String) {
            val name = symbol.repeat(255)
            prepareSaving()

            val actual = operations.createCompetition(user = manager, competitionName = name).getOrThrow()

            assertEquals(name, actual.data.name)
        }

        @ParameterizedTest
        @ValueSource(strings = ["a", "😀"])
        fun `should raise CompetitionNameTooLongError without saving if the name has 256 Unicode code points`(symbol: String) {
            val name = symbol.repeat(256)

            assertRaises(CompetitionNameTooLongError(name)) { operations.createCompetition(user = manager, competitionName = name) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should allow duplicate names without changing the existing competition`() {
            val first = testCompetition {
                name = "Same name"
                participants(emptyList())
            }
            val second = competition {
                id = 22
                createdAt = Instant.EPOCH
                version = EntityVersion(0)
                data = first.data
            }
            every { competitionRepository.save(any<CompetitionData>()) } returnsMany listOf(first, second)
            val existing = operations.createCompetition(user = manager, competitionName = "Same name").getOrThrow()
            val existingData = existing.data

            val actual = operations.createCompetition(user = manager, competitionName = "Same name").getOrThrow()

            assertSame(second, actual)
            assertEquals(CompetitionId(22), actual.id)
            assertEquals("Same name", actual.data.name)
            assertSame(existingData, existing.data)
            assertEquals(CompetitionId(21), existing.id)
            verify(exactly = 2) { competitionRepository.save(match<CompetitionData> { data -> data.name == "Same name" }) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should preserve the user and Manager competition snapshot without saving other entities`() {
            val user = testManager { data = managerData { competitions(listOf(99)) } }
            val userData = user.data
            val managerRole = user.data.roles.filterIsInstance<Manager>().single()
            prepareSaving()

            val actual = operations.createCompetition(user = user, competitionName = "New competition").getOrThrow()

            assertEquals(user.id, actual.data.owner.id)
            assertSame(userData, user.data)
            assertSame(managerRole, user.data.roles.filterIsInstance<Manager>().single())
            assertEquals(listOf(CompetitionId(99)), managerRole.data.competitions.ids)
            assertEquals(0L, user.version?.value)
            verify(exactly = 1) { competitionRepository.save(any<CompetitionData>()) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should propagate a technical storage exception when saving the competition`() {
            val failure = IllegalStateException("Competition storage unavailable")
            every { competitionRepository.save(any<CompetitionData>()) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.createCompetition(user = manager, competitionName = "New competition")
            }

            assertSame(failure, actual)
        }

        private fun prepareSaving() {
            every { competitionRepository.save(any<CompetitionData>()) } answers {
                competition {
                    id = 22
                    createdAt = Instant.EPOCH
                    version = EntityVersion(0)
                    data = firstArg<CompetitionData>()
                }
            }
        }
    }

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

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should raise ClassNotExistsError if class does not exist`() {
            every { repository.findById(classId) } returns null

            assertRaises(ClassNotExistsError(classId)) { operations.viewClass(user = manager, classId = classId) }

            verify { repository.findById(classId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should raise ClassAccessDeniedError if class belongs to another owner`() {
            val foreign = testClass { owner = MultipleRoleUserId(99) }
            every { repository.findById(classId) } returns foreign

            assertRaises(ClassAccessDeniedError(classId)) { operations.viewClass(user = manager, classId = classId) }

            verify { repository.findById(classId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should deny a foreign class incorrectly included in the Manager class snapshot`() {
            val user = testManager { data = managerData { classes(listOf(11)) } }
            val foreign = testClass { owner = MultipleRoleUserId(99) }
            every { repository.findById(classId) } returns foreign

            assertRaises(ClassAccessDeniedError(classId)) { operations.viewClass(user = user, classId = classId) }
        }

        @Test
        fun `should return the stored owned class with its student and contest ids without loading them`() {
            val original = testClass { contests(listOf(19, 20)) }
            every { repository.findById(classId) } returns original

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertSame(original, actual)
            assertEquals(ClassId(11), actual.id)
            assertEquals("Viewed class", actual.data.name)
            assertEquals(listOf(MultipleRoleUserId(1), MultipleRoleUserId(2)), actual.data.students.ids)
            assertEquals(listOf(ContestId(19), ContestId(20)), actual.data.contests.ids)
            verify { repository.findById(classId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
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
            val original = testClass()
            every { repository.findById(classId) } returns original

            val actual = operations.viewClass(user = user, classId = classId).getOrThrow()

            assertSame(original, actual)
        }

        @Test
        fun `should allow an owned class missing from the Manager class snapshot`() {
            val user = testManager { data = managerData { classes(listOf(99)) } }
            val original = testClass()
            every { repository.findById(classId) } returns original

            val actual = operations.viewClass(user = user, classId = classId).getOrThrow()

            assertSame(original, actual)
        }

        @Test
        fun `should preserve stored class data and version without writing any entities`() {
            val original = testClass { contests(listOf(19)) }
            every { repository.findById(classId) } returns original

            val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

            assertSame(original.data, actual.data)
            assertEquals(MultipleRoleUserId(0), actual.data.owner.id)
            assertEquals("Viewed class", actual.data.name)
            assertEquals("Class description", actual.data.description)
            assertEquals(Instant.EPOCH, actual.createdAt)
            assertEquals(listOf(MultipleRoleUserId(1), MultipleRoleUserId(2)), actual.data.students.ids)
            assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            assertEquals(0L, actual.version?.value)
            verify { repository.findById(classId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
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
    }

    @Nested
    inner class ViewCompetitionsTests {

        private val pagination = Pagination(page = 0, size = 10)
        private val manager = testManager { data = managerData {} }

        @Test
        fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) { operations.viewCompetitions(user = user, pagination = pagination) }

            verify(exactly = 0) { competitionRepository.findAvailableToManager(any(), any(), any()) }
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
            val expected = Page(content = listOf(testCompetition()), pagination = pagination, totalElements = 1)
            every { competitionRepository.findAvailableToManager(ownerId = user.id, pagination = pagination) } returns expected

            val actual = operations.viewCompetitions(user = user, pagination = pagination).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should return competition details and participant count without saving changes`() {
            val original = testCompetition { participants(listOf(1, 2)) }
            val expected = Page(content = listOf(original), pagination = pagination, totalElements = 1)
            every { competitionRepository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } returns expected

            val actual = operations.viewCompetitions(user = manager, pagination = pagination).getOrThrow()

            assertSame(expected, actual)
            assertSame(original, actual.content.single())
            assertEquals(CompetitionId(21), actual.content.single().id)
            assertEquals("Existing competition", actual.content.single().data.name)
            assertEquals(2, actual.content.single().data.participants.ids.size)
            assertEquals(EntityVersion(0), actual.content.single().version)
            verify(exactly = 0) { competitionRepository.save(any<CompetitionData>()) }
            verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            verify(exactly = 0) { competitionRepository.removeById(any()) }
            verify(exactly = 0) { competitionRepository.removeByIds(any()) }
        }

        @Test
        fun `should query current competitions independently of the Manager competition snapshot`() {
            val user = testManager { data = managerData { competitions(listOf(99)) } }
            val original = testCompetition()
            every { competitionRepository.findAvailableToManager(ownerId = user.id, pagination = pagination) } returns
                Page(content = listOf(original), pagination = pagination, totalElements = 1)

            val actual = operations.viewCompetitions(user = user, pagination = pagination).getOrThrow()

            assertEquals(listOf(original), actual.content)
        }

        @ParameterizedTest
        @CsvSource("0,0", "3,2")
        fun `should preserve an empty page and total for absent matches or a page beyond the end`(index: Int, total: Long) {
            val request = Pagination(page = index, size = 1)
            val expected = Page<Competition>(content = emptyList(), pagination = request, totalElements = total)
            every { competitionRepository.findAvailableToManager(ownerId = manager.id, pagination = request) } returns expected

            val actual = operations.viewCompetitions(user = manager, pagination = request).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should forward pagination and every filter unchanged`() {
            val request =
                Pagination(page = 3, size = 2, sort = Sort(listOf(Sort.Order(field = "createdAt", direction = Sort.Direction.DESC))))
            val filter =
                CompetitionFilter(name = "  Alpha%_  ", createdFrom = Instant.EPOCH, createdTo = Instant.ofEpochSecond(20))
            val expected = Page(content = listOf(testCompetition()), pagination = request, totalElements = 7)
            every {
                competitionRepository.findAvailableToManager(ownerId = manager.id, pagination = request, filter = filter)
            } returns expected

            val actual = operations.viewCompetitions(user = manager, pagination = request, filter = filter).getOrThrow()

            assertSame(expected, actual)
        }

        @Test
        fun `should propagate a technical storage exception when listing competitions`() {
            val failure = IllegalStateException("Competition storage unavailable")
            every { competitionRepository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewCompetitions(user = manager, pagination = pagination)
            }

            assertSame(failure, actual)
        }
    }

    @Nested
    inner class ViewCompetitionTests {

        private val competitionId = CompetitionId(21)
        private val manager = testManager { data = managerData {} }

        @Test
        fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) { operations.viewCompetition(user = user, competitionId = competitionId) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should raise CompetitionNotExistsError if competition does not exist`() {
            every { competitionRepository.findById(competitionId) } returns null

            assertRaises(CompetitionNotExistsError(competitionId)) {
                operations.viewCompetition(user = manager, competitionId = competitionId)
            }

            verify { competitionRepository.findById(competitionId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should raise CompetitionAccessDeniedError if competition belongs to another owner`() {
            val foreign = testCompetition {
                owner = MultipleRoleUserId(99)
                participants(listOf(31))
                contests(listOf(19))
            }
            every { competitionRepository.findById(competitionId) } returns foreign

            assertRaises(CompetitionAccessDeniedError(competitionId)) {
                operations.viewCompetition(user = manager, competitionId = competitionId)
            }

            verify { competitionRepository.findById(competitionId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should deny a foreign competition incorrectly included in the Manager competition snapshot`() {
            val user = testManager { data = managerData { competitions(listOf(21)) } }
            val foreign = testCompetition {
                owner = MultipleRoleUserId(99)
                participants(listOf(31))
            }
            every { competitionRepository.findById(competitionId) } returns foreign

            assertRaises(CompetitionAccessDeniedError(competitionId)) {
                operations.viewCompetition(user = user, competitionId = competitionId)
            }
        }

        @Test
        fun `should return the stored owned competition with its participant and contest ids without loading them`() {
            val original = testCompetition {
                participants(listOf(31, 32))
                contests(listOf(19, 20))
            }
            every { competitionRepository.findById(competitionId) } returns original

            val actual = operations.viewCompetition(user = manager, competitionId = competitionId).getOrThrow()

            assertSame(original, actual)
            assertEquals(CompetitionId(21), actual.id)
            assertEquals("Existing competition", actual.data.name)
            assertEquals(listOf(SingleRoleUserId(31), SingleRoleUserId(32)), actual.data.participants.ids)
            assertEquals(listOf(ContestId(19), ContestId(20)), actual.data.contests.ids)
            verify { competitionRepository.findById(competitionId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
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
            val original = testCompetition()
            every { competitionRepository.findById(competitionId) } returns original

            val actual = operations.viewCompetition(user = user, competitionId = competitionId).getOrThrow()

            assertSame(original, actual)
        }

        @Test
        fun `should allow an owned competition missing from the Manager competition snapshot`() {
            val user = testManager { data = managerData { competitions(listOf(99)) } }
            val original = testCompetition()
            every { competitionRepository.findById(competitionId) } returns original

            val actual = operations.viewCompetition(user = user, competitionId = competitionId).getOrThrow()

            assertSame(original, actual)
        }

        @Test
        fun `should preserve stored competition data and version without writing any entities`() {
            val original = testCompetition {
                participants(listOf(31, 32))
                contests(listOf(19))
            }
            every { competitionRepository.findById(competitionId) } returns original

            val actual = operations.viewCompetition(user = manager, competitionId = competitionId).getOrThrow()

            assertSame(original.data, actual.data)
            assertEquals(MultipleRoleUserId(0), actual.data.owner.id)
            assertEquals("Existing competition", actual.data.name)
            assertEquals("Competition description", actual.data.description)
            assertEquals(Instant.EPOCH, actual.createdAt)
            assertEquals(listOf(SingleRoleUserId(31), SingleRoleUserId(32)), actual.data.participants.ids)
            assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            assertEquals(0L, actual.version?.value)
            verify { competitionRepository.findById(competitionId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should propagate a technical exception when reading the competition`() {
            val failure = IllegalStateException("Competition storage unavailable")
            every { competitionRepository.findById(competitionId) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewCompetition(user = manager, competitionId = competitionId)
            }

            assertSame(failure, actual)
        }
    }

    @Nested
    inner class ViewClassContestTests {

        private val classId = ClassId(11)
        private val contestId = ContestId(19)
        private val manager = testManager { data = managerData {} }

        @Test
        fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) { operations.viewClassContest(user = user, classId = classId, contestId = contestId) }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should raise ClassNotExistsError before reading the contest if class does not exist`() {
            every { repository.findById(classId) } returns null

            assertRaises(ClassNotExistsError(classId)) {
                operations.viewClassContest(user = manager, classId = classId, contestId = contestId)
            }

            verify { repository.findById(classId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should raise ContestNotExistsError if contest does not exist`() {
            every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                operations.viewClassContest(user = manager, classId = classId, contestId = contestId)
            }

            verify { submissionRepository wasNot Called }
        }

        @Test
        fun `should raise ContestNotExistsError before checking access if contest of a foreign class does not exist`() {
            every { repository.findById(classId) } returns testClass { owner = MultipleRoleUserId(99) }
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                operations.viewClassContest(user = manager, classId = classId, contestId = contestId)
            }
        }

        @Test
        fun `should raise ClassAccessDeniedError if class belongs to another owner`() {
            every { repository.findById(classId) } returns testClass {
                owner = MultipleRoleUserId(99)
                contests(listOf(19))
            }
            every { contestRepository.findById(contestId) } returns testContest()

            assertRaises(ClassAccessDeniedError(classId)) {
                operations.viewClassContest(user = manager, classId = classId, contestId = contestId)
            }

            verify { submissionRepository wasNot Called }
        }

        @Test
        fun `should deny a foreign class incorrectly included in the Manager class snapshot`() {
            val user = testManager { data = managerData { classes(listOf(11)) } }
            every { repository.findById(classId) } returns testClass {
                owner = MultipleRoleUserId(99)
                contests(listOf(19))
            }
            every { contestRepository.findById(contestId) } returns testContest()

            assertRaises(ClassAccessDeniedError(classId)) {
                operations.viewClassContest(user = user, classId = classId, contestId = contestId)
            }
        }

        @Test
        fun `should raise ContestNotAddedToClassError if contest is not added to the class`() {
            every { repository.findById(classId) } returns testClass { contests(listOf(20)) }
            every { contestRepository.findById(contestId) } returns testContest()

            assertRaises(ContestNotAddedToClassError(classId = classId, contestId = contestId)) {
                operations.viewClassContest(user = manager, classId = classId, contestId = contestId)
            }

            verify { submissionRepository wasNot Called }
        }

        @Test
        fun `should return the contest student ids in stored order and results for the contest tasks`() {
            val contest = testContest { tasks(listOf(5, 6)) }
            val results = listOf(
                ContestTaskResult(authorId = MultipleRoleUserId(2), taskId = TaskId(5), bestScore = Score(70), submissionCount = 3),
            )
            every { repository.findById(classId) } returns testClass {
                students(listOf(2, 1))
                contests(listOf(19))
            }
            every { contestRepository.findById(contestId) } returns contest
            every {
                submissionRepository.findContestResults(
                    contestId = contestId,
                    authorIds = setOf(MultipleRoleUserId(1), MultipleRoleUserId(2)),
                    taskIds = setOf(TaskId(5), TaskId(6)),
                )
            } returns results

            val actual = operations.viewClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

            assertSame(contest, actual.contest)
            assertEquals(listOf(MultipleRoleUserId(2), MultipleRoleUserId(1)), actual.memberIds)
            assertSame(results, actual.results)
        }

        @Test
        fun `should return the contest schedule unchanged`() {
            val contest = testContest {
                startsAt = Instant.parse("2026-01-01T10:00:00Z")
                contestDuration = Duration.ofHours(2)
                attemptDuration = Duration.ofMinutes(30)
            }
            every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns contest
            every { submissionRepository.findContestResults(any(), any(), any()) } returns emptyList()

            val actual = operations.viewClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

            assertEquals("Contest", actual.contest.data.name)
            assertEquals(Instant.parse("2026-01-01T10:00:00Z"), actual.contest.data.startsAt)
            assertEquals(Instant.parse("2026-01-01T12:00:00Z"), actual.contest.data.endsAt)
            assertEquals(Duration.ofMinutes(30), actual.contest.data.attemptDuration)
        }

        @Test
        fun `should query empty task ids if the contest has no tasks`() {
            every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns testContest()
            every {
                submissionRepository.findContestResults(
                    contestId = contestId,
                    authorIds = setOf(MultipleRoleUserId(1), MultipleRoleUserId(2)),
                    taskIds = emptySet(),
                )
            } returns emptyList()

            val actual = operations.viewClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

            assertEquals(listOf(MultipleRoleUserId(1), MultipleRoleUserId(2)), actual.memberIds)
            assertEquals(emptyList<ContestTaskResult>(), actual.results)
        }

        @Test
        fun `should return no member ids if the class has no students`() {
            every { repository.findById(classId) } returns testClass {
                students(emptyList())
                contests(listOf(19))
            }
            every { contestRepository.findById(contestId) } returns testContest { tasks(listOf(5)) }
            every {
                submissionRepository.findContestResults(contestId = contestId, authorIds = emptySet(), taskIds = setOf(TaskId(5)))
            } returns emptyList()

            val actual = operations.viewClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

            assertEquals(emptyList<UserId>(), actual.memberIds)
            assertEquals(emptyList<ContestTaskResult>(), actual.results)
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
            val contest = testContest()
            every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns contest
            every { submissionRepository.findContestResults(any(), any(), any()) } returns emptyList()

            val actual = operations.viewClassContest(user = user, classId = classId, contestId = contestId).getOrThrow()

            assertSame(contest, actual.contest)
        }

        @Test
        fun `should only read the class contest and results without writing any entities`() {
            every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns testContest()
            every { submissionRepository.findContestResults(any(), any(), any()) } returns emptyList()

            operations.viewClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

            verify { repository.findById(classId) }
            verify { contestRepository.findById(contestId) }
            verify { submissionRepository.findContestResults(any(), any(), any()) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should propagate a technical exception when reading contest results`() {
            val failure = IllegalStateException("Submission storage unavailable")
            every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns testContest()
            every { submissionRepository.findContestResults(any(), any(), any()) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewClassContest(user = manager, classId = classId, contestId = contestId)
            }

            assertSame(failure, actual)
        }
    }

    @Nested
    inner class ViewCompetitionContestTests {

        private val competitionId = CompetitionId(21)
        private val contestId = ContestId(19)
        private val manager = testManager { data = managerData {} }

        @Test
        fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
            val user = testAdministrator {}

            assertRaises(MissedManagerRoleError) {
                operations.viewCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
            }

            verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
        }

        @Test
        fun `should raise CompetitionNotExistsError before reading the contest if competition does not exist`() {
            every { competitionRepository.findById(competitionId) } returns null

            assertRaises(CompetitionNotExistsError(competitionId)) {
                operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
            }

            verify { competitionRepository.findById(competitionId) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should raise ContestNotExistsError before checking access if contest of a foreign competition does not exist`() {
            every { competitionRepository.findById(competitionId) } returns testCompetition { owner = MultipleRoleUserId(99) }
            every { contestRepository.findById(contestId) } returns null

            assertRaises(ContestNotExistsError(contestId)) {
                operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
            }

            verify { submissionRepository wasNot Called }
        }

        @Test
        fun `should raise CompetitionAccessDeniedError if competition belongs to another owner`() {
            every { competitionRepository.findById(competitionId) } returns testCompetition {
                owner = MultipleRoleUserId(99)
                contests(listOf(19))
            }
            every { contestRepository.findById(contestId) } returns testContest()

            assertRaises(CompetitionAccessDeniedError(competitionId)) {
                operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
            }

            verify { submissionRepository wasNot Called }
        }

        @Test
        fun `should deny a foreign competition incorrectly included in the Manager competition snapshot`() {
            val user = testManager { data = managerData { competitions(listOf(21)) } }
            every { competitionRepository.findById(competitionId) } returns testCompetition {
                owner = MultipleRoleUserId(99)
                contests(listOf(19))
            }
            every { contestRepository.findById(contestId) } returns testContest()

            assertRaises(CompetitionAccessDeniedError(competitionId)) {
                operations.viewCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
            }
        }

        @Test
        fun `should raise ContestNotAddedToCompetitionError if contest is not added to the competition`() {
            every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(20)) }
            every { contestRepository.findById(contestId) } returns testContest()

            assertRaises(ContestNotAddedToCompetitionError(competitionId = competitionId, contestId = contestId)) {
                operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
            }

            verify { submissionRepository wasNot Called }
        }

        @Test
        fun `should return the contest participant ids in stored order and results for the contest tasks`() {
            val contest = testContest { tasks(listOf(5, 6)) }
            val results = listOf(
                ContestTaskResult(authorId = SingleRoleUserId(31), taskId = TaskId(6), bestScore = null, submissionCount = 2),
            )
            every { competitionRepository.findById(competitionId) } returns testCompetition {
                participants(listOf(32, 31))
                contests(listOf(19))
            }
            every { contestRepository.findById(contestId) } returns contest
            every {
                submissionRepository.findContestResults(
                    contestId = contestId,
                    authorIds = setOf(SingleRoleUserId(31), SingleRoleUserId(32)),
                    taskIds = setOf(TaskId(5), TaskId(6)),
                )
            } returns results

            val actual = operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                .getOrThrow()

            assertSame(contest, actual.contest)
            assertEquals(listOf(SingleRoleUserId(32), SingleRoleUserId(31)), actual.memberIds)
            assertSame(results, actual.results)
        }

        @Test
        fun `should return no member ids if the competition has no participants`() {
            every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns testContest { tasks(listOf(5)) }
            every {
                submissionRepository.findContestResults(contestId = contestId, authorIds = emptySet(), taskIds = setOf(TaskId(5)))
            } returns emptyList()

            val actual = operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                .getOrThrow()

            assertEquals(emptyList<UserId>(), actual.memberIds)
            assertEquals(emptyList<ContestTaskResult>(), actual.results)
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
            val contest = testContest()
            every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns contest
            every { submissionRepository.findContestResults(any(), any(), any()) } returns emptyList()

            val actual = operations.viewCompetitionContest(user = user, competitionId = competitionId, contestId = contestId).getOrThrow()

            assertSame(contest, actual.contest)
        }

        @Test
        fun `should only read the competition contest and results without writing any entities`() {
            every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } returns testContest()
            every { submissionRepository.findContestResults(any(), any(), any()) } returns emptyList()

            operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId).getOrThrow()

            verify { competitionRepository.findById(competitionId) }
            verify { contestRepository.findById(contestId) }
            verify { submissionRepository.findContestResults(any(), any(), any()) }
            confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
        }

        @Test
        fun `should propagate a technical exception when reading the contest`() {
            val failure = IllegalStateException("Contest storage unavailable")
            every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
            every { contestRepository.findById(contestId) } throws failure

            val actual = assertThrows(IllegalStateException::class.java) {
                operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
            }

            assertSame(failure, actual)
        }
    }

    private fun testCompetition(builder: CompetitionDataBuilder.() -> Unit = {}): Competition = competition {
        id = 21
        createdAt = Instant.EPOCH
        version = EntityVersion(0)
        data = competitionData {
            owner = MultipleRoleUserId(0)
            name = "Existing competition"
            description = "Competition description"
            builder()
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
