package tech.testsys.operation.user

import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.classInvite
import tech.testsys.domain.builder.api.classInviteData
import tech.testsys.domain.builder.api.competition
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.builder.api.managerData
import tech.testsys.domain.builder.api.participant
import tech.testsys.domain.builder.api.participantData
import tech.testsys.domain.builder.api.studentData
import tech.testsys.domain.builder.group.ClassDataBuilder
import tech.testsys.domain.builder.group.CompetitionDataBuilder
import tech.testsys.domain.contract.persistence.ClassFilter
import tech.testsys.domain.contract.persistence.CompetitionFilter
import tech.testsys.domain.contract.persistence.ContestTaskResult
import tech.testsys.domain.contract.persistence.Page
import tech.testsys.domain.contract.persistence.Pagination
import tech.testsys.domain.contract.persistence.Sort
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.contract.persistence.repository.SubmissionRepository
import tech.testsys.domain.model.EntityVersion
import tech.testsys.domain.model.LazyEntity
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.ClassInvite
import tech.testsys.domain.model.group.ClassInviteData
import tech.testsys.domain.model.group.ClassInviteId
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.task.Score
import tech.testsys.domain.model.task.TaskId
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.domain.model.user.Participant
import tech.testsys.domain.model.user.SingleRoleUserId
import tech.testsys.domain.model.user.UserId
import tech.testsys.operation.config.ClassInviteConfig
import tech.testsys.operation.config.CompetitionConfig
import tech.testsys.operation.error.ClassAccessDeniedError
import tech.testsys.operation.error.ClassNameBlankError
import tech.testsys.operation.error.ClassNameTooLongError
import tech.testsys.operation.error.ClassNotExistsError
import tech.testsys.operation.error.CompetitionAccessDeniedError
import tech.testsys.operation.error.CompetitionNameBlankError
import tech.testsys.operation.error.CompetitionNameTooLongError
import tech.testsys.operation.error.CompetitionNotExistsError
import tech.testsys.operation.error.CompetitionParticipantLimitExceededError
import tech.testsys.operation.error.ContestAccessDeniedError
import tech.testsys.operation.error.ContestAlreadyAddedToClassError
import tech.testsys.operation.error.ContestAlreadyAddedToCompetitionError
import tech.testsys.operation.error.ContestNotAddedToClassError
import tech.testsys.operation.error.ContestNotAddedToCompetitionError
import tech.testsys.operation.error.ContestNotExistsError
import tech.testsys.operation.error.MissedManagerRoleError
import tech.testsys.operation.error.NonPositiveParticipantCountError
import tech.testsys.operation.error.getOrThrow
import tech.testsys.operation.util.assertRaises
import tech.testsys.operation.util.testAdministrator
import tech.testsys.operation.util.testContest
import tech.testsys.operation.util.testDeveloper
import tech.testsys.operation.util.testManager
import tech.testsys.operation.util.testMultipleRoleUser
import java.time.Clock
import java.time.Duration
import java.time.Instant

class ManagerOperationsTests {

    private val repository = mockk<ClassRepository>()
    private val competitionRepository = mockk<CompetitionRepository>()
    private val contestRepository = mockk<ContestRepository>()
    private val submissionRepository = mockk<SubmissionRepository>()
    private val participantRepository = mockk<ParticipantRepository>()
    private val competitionConfig = mockk<CompetitionConfig>()
    private val classInviteRepository = mockk<ClassInviteRepository>()
    private val classInviteConfig = mockk<ClassInviteConfig>()
    private val clock = mockk<Clock>()
    private val operations = ManagerOperations(
        classRepository = repository,
        competitionRepository = competitionRepository,
        contestRepository = contestRepository,
        submissionRepository = submissionRepository,
        participantRepository = participantRepository,
        competitionConfig = competitionConfig,
        classInviteRepository = classInviteRepository,
        classInviteConfig = classInviteConfig,
        clock = clock,
    )

    @Nested
    inner class CreateClassTests {

        private val manager = testManager { data = managerData {} }

        init {
            every { clock.instant() } returns Instant.parse("2026-01-01T10:00:00Z")
            every { classInviteConfig.ttl } returns Duration.ofDays(7)
        }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should create an owned empty class and save it exactly once`() {
                prepareSaving()

                val actual = operations.createClass(user = manager, className = "New class").getOrThrow()

                assertEquals(manager.id, actual.data.owner.id)
                assertEquals("New class", actual.data.name)
                assertEquals("", actual.data.description)
                assertEquals(emptyList<MultipleRoleUserId>(), actual.data.students.ids)
                assertEquals(emptyList<ContestId>(), actual.data.contests.ids)
                assertEquals(ClassInviteId(31), actual.data.invite.id)
                verify(exactly = 1) { repository.saveWithInvite(any(), any()) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @Test
            fun `should create the class with a new identity-stored invite code expiring after the ttl`() {
                val invite = slot<ClassInviteData>()
                prepareSaving(invite)

                operations.createClass(user = manager, className = "New class").getOrThrow()

                assertEquals(HashAlgorithm.Identity, invite.captured.codeHash.algorithm)
                assertTrue(INVITE_CODE_FORMAT.matches(invite.captured.codeHash.value), "unexpected code ${invite.captured.codeHash.value}")
                assertEquals(Instant.parse("2026-01-08T10:00:00Z"), invite.captured.expiresAt)
            }

            @Test
            fun `should return the class stored by the repository`() {
                val stored = testClass { name = "New class" }
                every { repository.saveWithInvite(any(), any()) } returns stored

                val actual = operations.createClass(user = manager, className = "New class").getOrThrow()

                assertSame(stored, actual)
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

            @Test
            fun `should create a class with an already used name without reading or updating existing classes`() {
                prepareSaving()

                val actual = operations.createClass(user = manager, className = "Same name").getOrThrow()

                assertEquals("Same name", actual.data.name)
                verify(exactly = 1) { repository.saveWithInvite(any(), any()) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @ParameterizedTest
            @ValueSource(strings = ["a", "😀"])
            fun `should accept a name with exactly 255 Unicode code points`(symbol: String) {
                val name = symbol.repeat(255)
                prepareSaving()

                val actual = operations.createClass(user = manager, className = name).getOrThrow()

                assertEquals(name, actual.data.name)
            }
        }

        @Nested
        inner class RefusalTests {

            @ParameterizedTest
            @ValueSource(strings = ["", "Valid name", "a"])
            fun `should raise MissedManagerRoleError before saving or validating the name if user is not a Manager`(name: String) {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) { operations.createClass(user = user, className = name) }

                verify {
                    listOf(repository, competitionRepository, contestRepository, submissionRepository, classInviteConfig, clock) wasNot
                        Called
                }
            }

            @ParameterizedTest
            @ValueSource(strings = ["", " ", "\t", "\n", " \t\r\n", " "])
            fun `should raise ClassNameBlankError without saving if the name is blank`(name: String) {
                assertRaises(ClassNameBlankError) { operations.createClass(user = manager, className = name) }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }

            @ParameterizedTest
            @ValueSource(strings = ["a", "😀"])
            fun `should raise ClassNameTooLongError without saving if the name has 256 Unicode code points`(symbol: String) {
                val name = symbol.repeat(256)

                assertRaises(ClassNameTooLongError(name)) { operations.createClass(user = manager, className = name) }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should save only the new class without updating other classes, students or contests`() {
                prepareSaving()

                operations.createClass(user = manager, className = "New class").getOrThrow()

                verify(exactly = 1) { repository.saveWithInvite(any(), any()) }
                confirmVerified(
                    repository,
                    competitionRepository,
                    contestRepository,
                    submissionRepository,
                    participantRepository,
                    classInviteRepository,
                )
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the time and the ttl once`() {
                prepareSaving()

                operations.createClass(user = manager, className = "New class").getOrThrow()

                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { classInviteConfig.ttl }
            }

            @Test
            fun `should propagate a technical storage exception when saving the class`() {
                val failure = IllegalStateException("Class storage unavailable")
                every { repository.saveWithInvite(any(), any()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.createClass(user = manager, className = "New class")
                }

                assertSame(failure, actual)
            }
        }

        private fun prepareSaving(invite: CapturingSlot<ClassInviteData> = slot()) {
            every { repository.saveWithInvite(capture(invite), any()) } answers {
                val data = secondArg<(ClassInviteId) -> ClassData>()
                `class` {
                    id = 12
                    createdAt = Instant.EPOCH
                    version = EntityVersion(0)
                    this.data = data(ClassInviteId(31))
                }
            }
        }
    }

    @Nested
    inner class CreateCompetitionTests {

        private val manager = testManager { data = managerData {} }

        @Nested
        inner class HappyPathTests {

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
            fun `should return the competition stored by the repository`() {
                val stored = testCompetition { name = "New competition" }
                every { competitionRepository.save(any<CompetitionData>()) } returns stored

                val actual = operations.createCompetition(user = manager, competitionName = "New competition").getOrThrow()

                assertSame(stored, actual)
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

            @Test
            fun `should create a competition with an already used name without reading or updating existing competitions`() {
                prepareSaving()

                val actual = operations.createCompetition(user = manager, competitionName = "Same name").getOrThrow()

                assertEquals("Same name", actual.data.name)
                verify(exactly = 1) { competitionRepository.save(match<CompetitionData> { data -> data.name == "Same name" }) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @ParameterizedTest
            @ValueSource(strings = ["a", "😀"])
            fun `should accept a name with exactly 255 Unicode code points`(symbol: String) {
                val name = symbol.repeat(255)
                prepareSaving()

                val actual = operations.createCompetition(user = manager, competitionName = name).getOrThrow()

                assertEquals(name, actual.data.name)
            }
        }

        @Nested
        inner class RefusalTests {

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

            @ParameterizedTest
            @ValueSource(strings = ["", " ", "\t", "\n", " \t\r\n", " "])
            fun `should raise CompetitionNameBlankError without saving if the name is blank`(name: String) {
                assertRaises(CompetitionNameBlankError) { operations.createCompetition(user = manager, competitionName = name) }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }

            @ParameterizedTest
            @ValueSource(strings = ["a", "😀"])
            fun `should raise CompetitionNameTooLongError without saving if the name has 256 Unicode code points`(symbol: String) {
                val name = symbol.repeat(256)

                assertRaises(CompetitionNameTooLongError(name)) { operations.createCompetition(user = manager, competitionName = name) }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should save only the new competition without updating other competitions, users or contests`() {
                prepareSaving()

                operations.createCompetition(user = manager, competitionName = "New competition").getOrThrow()

                verify(exactly = 1) { competitionRepository.save(any<CompetitionData>()) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository, participantRepository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate a technical storage exception when saving the competition`() {
                val failure = IllegalStateException("Competition storage unavailable")
                every { competitionRepository.save(any<CompetitionData>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.createCompetition(user = manager, competitionName = "New competition")
                }

                assertSame(failure, actual)
            }
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

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the page of owned classes found by the repository`() {
                val expected = Page(content = listOf(testClass()), pagination = pagination, totalElements = 1)
                every { repository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } returns expected

                val actual = operations.viewClasses(user = manager, pagination = pagination).getOrThrow()

                assertSame(expected, actual)
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
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) { operations.viewClasses(user = user, pagination = pagination) }

                verify(exactly = 0) { repository.findAvailableToManager(any(), any(), any()) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should query current classes independently of the Manager class snapshot`() {
                val user = testManager { data = managerData { classes(listOf(99)) } }
                val original = testClass()
                every { repository.findAvailableToManager(ownerId = user.id, pagination = pagination) } returns
                    Page(content = listOf(original), pagination = pagination, totalElements = 1)

                val actual = operations.viewClasses(user = user, pagination = pagination).getOrThrow()

                assertEquals(listOf(original), actual.content)
            }

            @Test
            fun `should only read the owned classes without writing them`() {
                every { repository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } returns
                    Page(content = listOf(testClass()), pagination = pagination, totalElements = 1)

                operations.viewClasses(user = manager, pagination = pagination).getOrThrow()

                verify { repository.findAvailableToManager(ownerId = manager.id, pagination = pagination) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository, classInviteRepository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

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
    }

    @Nested
    inner class ViewClassTests {

        private val classId = ClassId(11)
        private val manager = testManager { data = managerData {} }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the stored class owned by the user`() {
                val original = testClass { contests(listOf(19, 20)) }
                every { repository.findById(classId) } returns original

                val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

                assertSame(original, actual)
            }

            @Test
            fun `should return the stored invite reference without reading or writing invites`() {
                every { repository.findById(classId) } returns testClass { invite(32) }

                val actual = operations.viewClass(user = manager, classId = classId).getOrThrow()

                assertEquals(ClassInviteId(32), actual.data.invite.id)
                verify { listOf(classInviteRepository, clock) wasNot Called }
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
        }

        @Nested
        inner class RefusalTests {

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
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should allow an owned class missing from the Manager class snapshot`() {
                val user = testManager { data = managerData { classes(listOf(99)) } }
                val original = testClass()
                every { repository.findById(classId) } returns original

                val actual = operations.viewClass(user = user, classId = classId).getOrThrow()

                assertSame(original, actual)
            }

            @Test
            fun `should only read the class without writing it or loading its students and contests`() {
                every { repository.findById(classId) } returns testClass { contests(listOf(19)) }

                operations.viewClass(user = manager, classId = classId).getOrThrow()

                verify { repository.findById(classId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository, classInviteRepository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

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
    }

    @Nested
    inner class CreateClassInviteTests {

        private val classId = ClassId(11)
        private val manager = testManager { data = managerData {} }
        private val now = Instant.parse("2026-01-01T10:00:00Z")
        private val ttl = Duration.ofDays(7)

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should replace the referenced code with a different one and restart its validity period`() {
                val existing = testClassInvite(code = "abcdefghjkmn", expiresAt = Instant.parse("2030-01-01T00:00:00Z"))
                prepareCreation(existing = existing)
                val updated = slot<ClassInvite>()
                every { classInviteRepository.update(capture(updated)) } answers { firstArg() }

                val actual = operations.createClassInvite(user = manager, classId = classId).getOrThrow()

                assertEquals(existing.id, updated.captured.id)
                assertEquals(existing.version, updated.captured.version)
                assertEquals(HashAlgorithm.Identity, updated.captured.data.codeHash.algorithm)
                assertTrue(INVITE_CODE_FORMAT.matches(updated.captured.data.codeHash.value))
                assertNotEquals(existing.data.codeHash, updated.captured.data.codeHash)
                assertEquals(Instant.parse("2026-01-08T10:00:00Z"), updated.captured.data.expiresAt)
                assertSame(updated.captured, actual)
                verify(exactly = 0) { classInviteRepository.save(any<ClassInviteData>()) }
            }

            @Test
            fun `should truncate the expiration moment to microseconds`() {
                prepareCreation(existing = testClassInvite(), now = Instant.parse("2026-01-01T10:00:00.123456789Z"))
                every { classInviteRepository.update(any<ClassInvite>()) } answers { firstArg() }

                val actual = operations.createClassInvite(user = manager, classId = classId).getOrThrow()

                assertEquals(Instant.parse("2026-01-08T10:00:00.123456Z"), actual.data.expiresAt)
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
                prepareCreation(existing = testClassInvite())
                every { classInviteRepository.update(any<ClassInvite>()) } answers { firstArg() }

                val actual = operations.createClassInvite(user = user, classId = classId).getOrThrow()

                assertEquals(Instant.parse("2026-01-08T10:00:00Z"), actual.data.expiresAt)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage, time or configuration if user is not a Manager`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) { operations.createClassInvite(user = user, classId = classId) }

                verify { listOf(repository, classInviteRepository, classInviteConfig, clock) wasNot Called }
            }

            @Test
            fun `should raise ClassNotExistsError without writing an invite if class does not exist`() {
                every { repository.findById(classId) } returns null

                assertRaises(ClassNotExistsError(classId)) { operations.createClassInvite(user = manager, classId = classId) }

                verify { listOf(classInviteRepository, classInviteConfig, clock) wasNot Called }
            }

            @Test
            fun `should raise ClassAccessDeniedError without writing an invite if class belongs to another owner`() {
                every { repository.findById(classId) } returns testClass { owner = MultipleRoleUserId(99) }

                assertRaises(ClassAccessDeniedError(classId)) { operations.createClassInvite(user = manager, classId = classId) }

                verify { listOf(classInviteRepository, classInviteConfig, clock) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not update the class, its students or contests when replacing its invite code`() {
                prepareCreation(existing = testClassInvite())
                every { classInviteRepository.update(any<ClassInvite>()) } answers { firstArg() }

                operations.createClassInvite(user = manager, classId = classId).getOrThrow()

                verify { repository.findById(classId) }
                confirmVerified(repository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the time and the ttl once`() {
                prepareCreation(existing = testClassInvite())
                every { classInviteRepository.update(any<ClassInvite>()) } answers { firstArg() }

                operations.createClassInvite(user = manager, classId = classId).getOrThrow()

                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { classInviteConfig.ttl }
            }

            @Test
            fun `should propagate a storage exception such as a code collision`() {
                prepareCreation(existing = testClassInvite())
                val failure = IllegalStateException("Duplicate invite code")
                every { classInviteRepository.update(any<ClassInvite>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.createClassInvite(user = manager, classId = classId)
                }

                assertSame(failure, actual)
            }
        }

        private fun prepareCreation(existing: ClassInvite, now: Instant = this.now) {
            every { repository.findById(classId) } returns testClass()
            every { classInviteRepository.load(any<LazyEntity<ClassInviteId, ClassInvite>>()) } returns existing
            every { clock.instant() } returns now
            every { classInviteConfig.ttl } returns ttl
        }
    }

    @Nested
    inner class ExtendClassInviteTests {

        private val classId = ClassId(11)
        private val manager = testManager { data = managerData {} }
        private val now = Instant.parse("2026-01-01T10:00:00Z")

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should keep the code and restart the validity period from the current time`() {
                val existing = testClassInvite(code = "abcdefghjkmn", expiresAt = Instant.parse("2026-01-02T00:00:00Z"))
                prepareExtension(existing = existing, ttl = Duration.ofDays(7))

                val actual = operations.extendClassInvite(user = manager, classId = classId).getOrThrow()

                assertEquals(existing.data.codeHash, actual.data.codeHash)
                assertEquals(existing.id, actual.id)
                assertEquals(Instant.parse("2026-01-08T10:00:00Z"), actual.data.expiresAt)
                verify(exactly = 1) { classInviteRepository.update(any<ClassInvite>()) }
            }

            @Test
            fun `should extend an invite code whose validity period has already ended`() {
                val existing = testClassInvite(code = "abcdefghjkmn", expiresAt = now)
                prepareExtension(existing = existing, ttl = Duration.ofHours(1))

                val actual = operations.extendClassInvite(user = manager, classId = classId).getOrThrow()

                assertEquals(existing.data.codeHash, actual.data.codeHash)
                assertEquals(Instant.parse("2026-01-01T11:00:00Z"), actual.data.expiresAt)
            }

            @Test
            fun `should move the expiration moment earlier if the ttl was reduced`() {
                val existing = testClassInvite(expiresAt = Instant.parse("2026-02-01T00:00:00Z"))
                prepareExtension(existing = existing, ttl = Duration.ofHours(1))

                val actual = operations.extendClassInvite(user = manager, classId = classId).getOrThrow()

                assertEquals(Instant.parse("2026-01-01T11:00:00Z"), actual.data.expiresAt)
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
                prepareExtension(existing = testClassInvite(), ttl = Duration.ofDays(7))

                val actual = operations.extendClassInvite(user = user, classId = classId).getOrThrow()

                assertEquals(Instant.parse("2026-01-08T10:00:00Z"), actual.data.expiresAt)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage, time or configuration if user is not a Manager`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) { operations.extendClassInvite(user = user, classId = classId) }

                verify { listOf(repository, classInviteRepository, classInviteConfig, clock) wasNot Called }
            }

            @Test
            fun `should raise ClassNotExistsError if class does not exist`() {
                every { repository.findById(classId) } returns null

                assertRaises(ClassNotExistsError(classId)) { operations.extendClassInvite(user = manager, classId = classId) }

                verify { listOf(classInviteRepository, classInviteConfig, clock) wasNot Called }
            }

            @Test
            fun `should raise ClassAccessDeniedError if class belongs to another owner`() {
                every { repository.findById(classId) } returns testClass { owner = MultipleRoleUserId(99) }

                assertRaises(ClassAccessDeniedError(classId)) { operations.extendClassInvite(user = manager, classId = classId) }

                verify { listOf(classInviteRepository, classInviteConfig, clock) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not update the class, its students or contests when extending its invite code`() {
                prepareExtension(existing = testClassInvite(), ttl = Duration.ofDays(7))

                operations.extendClassInvite(user = manager, classId = classId).getOrThrow()

                verify { repository.findById(classId) }
                confirmVerified(repository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the time and the ttl once`() {
                prepareExtension(existing = testClassInvite(), ttl = Duration.ofDays(7))

                operations.extendClassInvite(user = manager, classId = classId).getOrThrow()

                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { classInviteConfig.ttl }
            }

            @Test
            fun `should propagate a storage exception of the update`() {
                val failure = IllegalStateException("Stale invite version")
                prepareExtension(existing = testClassInvite(), ttl = Duration.ofDays(7))
                every { classInviteRepository.update(any<ClassInvite>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.extendClassInvite(user = manager, classId = classId)
                }

                assertSame(failure, actual)
            }
        }

        private fun prepareExtension(existing: ClassInvite, ttl: Duration) {
            every { repository.findById(classId) } returns testClass()
            every { classInviteRepository.load(any<LazyEntity<ClassInviteId, ClassInvite>>()) } returns existing
            every { clock.instant() } returns now
            every { classInviteConfig.ttl } returns ttl
            every { classInviteRepository.update(any<ClassInvite>()) } answers { firstArg() }
        }
    }

    @Nested
    inner class RefreshClassInviteTests {

        private val classId = ClassId(11)
        private val manager = testManager { data = managerData {} }
        private val now = Instant.parse("2026-01-01T10:00:00Z")

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return a still valid invite unchanged without writing`() {
                val invite = testClassInvite(expiresAt = now.plusNanos(1_000))
                prepareRefresh(invite)

                val actual = operations.refreshClassInvite(user = manager, classId = classId).getOrThrow()

                assertSame(invite, actual)
                verify(exactly = 0) { classInviteRepository.update(any<ClassInvite>()) }
                verify { classInviteConfig wasNot Called }
            }

            @Test
            fun `should replace the code and restart the validity period if the invite expires at the current time`() {
                val invite = testClassInvite(code = "abcdefghjkmn", expiresAt = now)
                prepareRefresh(invite)
                every { classInviteConfig.ttl } returns Duration.ofDays(7)
                val updated = slot<ClassInvite>()
                every { classInviteRepository.update(capture(updated)) } answers { firstArg() }

                val actual = operations.refreshClassInvite(user = manager, classId = classId).getOrThrow()

                assertSame(updated.captured, actual)
                assertEquals(invite.id, updated.captured.id)
                assertEquals(HashAlgorithm.Identity, updated.captured.data.codeHash.algorithm)
                assertTrue(INVITE_CODE_FORMAT.matches(updated.captured.data.codeHash.value))
                assertNotEquals(invite.data.codeHash, updated.captured.data.codeHash)
                assertEquals(Instant.parse("2026-01-08T10:00:00Z"), updated.captured.data.expiresAt)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage if the owner lost the Manager role`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) { operations.refreshClassInvite(user = user, classId = classId) }

                verify { listOf(repository, classInviteRepository, classInviteConfig, clock) wasNot Called }
            }

            @Test
            fun `should raise ClassNotExistsError if class does not exist`() {
                every { repository.findById(classId) } returns null

                assertRaises(ClassNotExistsError(classId)) { operations.refreshClassInvite(user = manager, classId = classId) }

                verify { listOf(classInviteRepository, classInviteConfig, clock) wasNot Called }
            }

            @Test
            fun `should raise ClassAccessDeniedError if class belongs to another owner`() {
                every { repository.findById(classId) } returns testClass { owner = MultipleRoleUserId(99) }

                assertRaises(ClassAccessDeniedError(classId)) { operations.refreshClassInvite(user = manager, classId = classId) }

                verify { listOf(classInviteRepository, classInviteConfig, clock) wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should not update the class, its students or contests when replacing an expired invite code`() {
                prepareRefresh(testClassInvite(expiresAt = Instant.EPOCH))
                every { classInviteConfig.ttl } returns Duration.ofDays(7)
                every { classInviteRepository.update(any<ClassInvite>()) } answers { firstArg() }

                operations.refreshClassInvite(user = manager, classId = classId).getOrThrow()

                verify { repository.findById(classId) }
                confirmVerified(repository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the time and the ttl once when replacing an expired invite code`() {
                prepareRefresh(testClassInvite(expiresAt = Instant.EPOCH))
                every { classInviteConfig.ttl } returns Duration.ofDays(7)
                every { classInviteRepository.update(any<ClassInvite>()) } answers { firstArg() }

                operations.refreshClassInvite(user = manager, classId = classId).getOrThrow()

                verify(exactly = 1) { clock.instant() }
                verify(exactly = 1) { classInviteConfig.ttl }
            }

            @ParameterizedTest
            @ValueSource(longs = [0, -1])
            fun `should fail without writing if the configured ttl is not positive`(ttlSeconds: Long) {
                prepareRefresh(testClassInvite(expiresAt = Instant.EPOCH))
                every { classInviteConfig.ttl } returns Duration.ofSeconds(ttlSeconds)

                assertThrows(IllegalStateException::class.java) { operations.refreshClassInvite(user = manager, classId = classId) }

                verify(exactly = 0) { classInviteRepository.update(any<ClassInvite>()) }
            }

            @Test
            fun `should propagate a storage exception of the update`() {
                val failure = IllegalStateException("Stale invite version")
                prepareRefresh(testClassInvite(expiresAt = Instant.EPOCH))
                every { classInviteConfig.ttl } returns Duration.ofDays(7)
                every { classInviteRepository.update(any<ClassInvite>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.refreshClassInvite(user = manager, classId = classId)
                }

                assertSame(failure, actual)
            }
        }

        private fun prepareRefresh(invite: ClassInvite) {
            every { repository.findById(classId) } returns testClass()
            every { classInviteRepository.load(any<LazyEntity<ClassInviteId, ClassInvite>>()) } returns invite
            every { clock.instant() } returns now
        }
    }

    @Nested
    inner class ViewCompetitionsTests {

        private val pagination = Pagination(page = 0, size = 10)
        private val manager = testManager { data = managerData {} }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the page of owned competitions found by the repository`() {
                val original = testCompetition { participants(listOf(1, 2)) }
                val expected = Page(content = listOf(original), pagination = pagination, totalElements = 1)
                every { competitionRepository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } returns expected

                val actual = operations.viewCompetitions(user = manager, pagination = pagination).getOrThrow()

                assertSame(expected, actual)
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
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) { operations.viewCompetitions(user = user, pagination = pagination) }

                verify(exactly = 0) { competitionRepository.findAvailableToManager(any(), any(), any()) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should query current competitions independently of the Manager competition snapshot`() {
                val user = testManager { data = managerData { competitions(listOf(99)) } }
                val original = testCompetition()
                every { competitionRepository.findAvailableToManager(ownerId = user.id, pagination = pagination) } returns
                    Page(content = listOf(original), pagination = pagination, totalElements = 1)

                val actual = operations.viewCompetitions(user = user, pagination = pagination).getOrThrow()

                assertEquals(listOf(original), actual.content)
            }

            @Test
            fun `should only read the owned competitions without writing them or their participants`() {
                every { competitionRepository.findAvailableToManager(ownerId = manager.id, pagination = pagination) } returns
                    Page(content = listOf(testCompetition()), pagination = pagination, totalElements = 1)

                operations.viewCompetitions(user = manager, pagination = pagination).getOrThrow()

                verify { competitionRepository.findAvailableToManager(ownerId = manager.id, pagination = pagination) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository, participantRepository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

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
    }

    @Nested
    inner class ViewCompetitionTests {

        private val competitionId = CompetitionId(21)
        private val manager = testManager { data = managerData {} }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should return the stored competition owned by the user`() {
                val original = testCompetition {
                    participants(listOf(31, 32))
                    contests(listOf(19, 20))
                }
                every { competitionRepository.findById(competitionId) } returns original

                val actual = operations.viewCompetition(user = manager, competitionId = competitionId).getOrThrow()

                assertSame(original, actual)
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
        }

        @Nested
        inner class RefusalTests {

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
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should allow an owned competition missing from the Manager competition snapshot`() {
                val user = testManager { data = managerData { competitions(listOf(99)) } }
                val original = testCompetition()
                every { competitionRepository.findById(competitionId) } returns original

                val actual = operations.viewCompetition(user = user, competitionId = competitionId).getOrThrow()

                assertSame(original, actual)
            }

            @Test
            fun `should only read the competition without writing it or loading its participants and contests`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition {
                    participants(listOf(31, 32))
                    contests(listOf(19))
                }

                operations.viewCompetition(user = manager, competitionId = competitionId).getOrThrow()

                verify { competitionRepository.findById(competitionId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository, participantRepository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

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
    }

    @Nested
    inner class ViewClassContestTests {

        private val classId = ClassId(11)
        private val contestId = ContestId(19)
        private val manager = testManager { data = managerData {} }

        @Nested
        inner class HappyPathTests {

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
            fun `should return an added contest of another owner that is not shared to any community of the Manager`() {
                val contest = testContest {
                    owner = MultipleRoleUserId(77)
                    sharedTo(listOf(4))
                }
                every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
                every { contestRepository.findById(contestId) } returns contest
                every { submissionRepository.findContestResults(any(), any(), any()) } returns emptyList()

                val actual = operations.viewClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

                assertSame(contest, actual.contest)
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
        }

        @Nested
        inner class RefusalTests {

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
        }

        @Nested
        inner class InvariantTests {

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
        }

        @Nested
        inner class ModuleRuleTests {

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
    }

    @Nested
    inner class ViewCompetitionContestTests {

        private val competitionId = CompetitionId(21)
        private val contestId = ContestId(19)
        private val manager = testManager { data = managerData {} }

        @Nested
        inner class HappyPathTests {

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
            fun `should query empty task ids if the contest has no tasks`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition {
                    participants(listOf(31))
                    contests(listOf(19))
                }
                every { contestRepository.findById(contestId) } returns testContest()
                every {
                    submissionRepository.findContestResults(
                        contestId = contestId,
                        authorIds = setOf(SingleRoleUserId(31)),
                        taskIds = emptySet(),
                    )
                } returns emptyList()

                val actual = operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertEquals(listOf(SingleRoleUserId(31)), actual.memberIds)
                assertEquals(emptyList<ContestTaskResult>(), actual.results)
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
            fun `should return an added contest of another owner that is not shared to any community of the Manager`() {
                val contest = testContest {
                    owner = MultipleRoleUserId(77)
                    sharedTo(listOf(4))
                }
                every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
                every { contestRepository.findById(contestId) } returns contest
                every { submissionRepository.findContestResults(any(), any(), any()) } returns emptyList()

                val actual = operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertSame(contest, actual.contest)
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

                val actual = operations.viewCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertSame(contest, actual.contest)
            }
        }

        @Nested
        inner class RefusalTests {

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
            fun `should raise ContestNotExistsError if contest does not exist`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
                every { contestRepository.findById(contestId) } returns null

                assertRaises(ContestNotExistsError(contestId)) {
                    operations.viewCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify { submissionRepository wasNot Called }
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
        }

        @Nested
        inner class InvariantTests {

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
        }

        @Nested
        inner class ModuleRuleTests {

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
    }

    @Nested
    inner class AddClassContestTests {

        private val classId = ClassId(11)
        private val contestId = ContestId(19)
        private val manager = testManager {
            memberOf(listOf(4))
            data = managerData {}
        }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should append the contest to the class and save it once with the other data unchanged`() {
                val saved = testClass { contests(listOf(17, 18, 19)) }
                every { repository.findById(classId) } returns testClass { contests(listOf(17, 18)) }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { repository.update(any<Class>()) } returns saved

                val actual = operations.addClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

                assertSame(saved, actual)
                verify(exactly = 1) {
                    repository.update(
                        match<Class> { updated ->
                            updated.id == ClassId(11) &&
                                updated.createdAt == Instant.EPOCH &&
                                updated.version == EntityVersion(0) &&
                                updated.data.owner.id == MultipleRoleUserId(0) &&
                                updated.data.name == "Viewed class" &&
                                updated.data.description == "Class description" &&
                                updated.data.students.ids == listOf(MultipleRoleUserId(1), MultipleRoleUserId(2)) &&
                                updated.data.contests.ids == listOf(ContestId(17), ContestId(18), ContestId(19))
                        },
                    )
                }
            }

            @Test
            fun `should allow a contest shared to several communities if only one of them has the Manager`() {
                val user = testManager {
                    memberOf(listOf(4, 6))
                    data = managerData {}
                }
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(3, 4, 5)) }
                every { repository.update(any<Class>()) } answers { firstArg() }

                val actual = operations.addClassContest(user = user, classId = classId, contestId = contestId).getOrThrow()

                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            }

            @Test
            fun `should add a contest without tasks and schedule`() {
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { repository.update(any<Class>()) } answers { firstArg() }

                val actual = operations.addClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            }

            @Test
            fun `should add a contest that is already added to another class`() {
                val otherClass = `class` {
                    id = 12
                    createdAt = Instant.EPOCH
                    version = EntityVersion(0)
                    data = classData {
                        owner = MultipleRoleUserId(0)
                        name = "Other class"
                        description = ""
                        invite(32)
                        contests(listOf(19))
                    }
                }
                every { repository.findById(otherClass.id) } returns otherClass
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { repository.update(any<Class>()) } answers { firstArg() }

                val actual = operations.addClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

                assertEquals(classId, actual.id)
                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
                verify(exactly = 0) { repository.update(match<Class> { updated -> updated.id == otherClass.id }) }
            }

            @Test
            fun `should allow a Manager who also has other roles`() {
                val user = testMultipleRoleUser {
                    roles {
                        manager {
                            memberOf(listOf(4))
                            data = managerData {}
                        }
                        student { data = studentData {} }
                        administrator {}
                    }
                }
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { repository.update(any<Class>()) } answers { firstArg() }

                val actual = operations.addClassContest(user = user, classId = classId, contestId = contestId).getOrThrow()

                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) { operations.addClassContest(user = user, classId = classId, contestId = contestId) }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }

            @Test
            fun `should raise MissedManagerRoleError if user is only a Developer of the community the contest is shared to`() {
                val user = testDeveloper {
                    memberOf(listOf(4))
                    data = developerData {}
                }

                assertRaises(MissedManagerRoleError) { operations.addClassContest(user = user, classId = classId, contestId = contestId) }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }

            @Test
            fun `should raise ClassNotExistsError before reading the contest if class does not exist`() {
                every { repository.findById(classId) } returns null

                assertRaises(ClassNotExistsError(classId)) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                verify { repository.findById(classId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @Test
            fun `should raise ContestNotExistsError without saving if contest does not exist`() {
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns null

                assertRaises(ContestNotExistsError(contestId)) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                verify { repository.findById(classId) }
                verify { contestRepository.findById(contestId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @Test
            fun `should raise ContestNotExistsError before checking access if contest for a foreign class does not exist`() {
                every { repository.findById(classId) } returns testClass { owner = MultipleRoleUserId(99) }
                every { contestRepository.findById(contestId) } returns null

                assertRaises(ContestNotExistsError(contestId)) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                verify(exactly = 0) { repository.update(any<Class>()) }
            }

            @Test
            fun `should raise ClassAccessDeniedError without saving if class belongs to another owner`() {
                every { repository.findById(classId) } returns testClass { owner = MultipleRoleUserId(99) }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }

                assertRaises(ClassAccessDeniedError(classId)) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                verify(exactly = 0) { repository.update(any<Class>()) }
            }

            @Test
            fun `should deny a foreign class incorrectly included in the Manager class snapshot`() {
                val user = testManager {
                    memberOf(listOf(4))
                    data = managerData { classes(listOf(11)) }
                }
                every { repository.findById(classId) } returns testClass { owner = MultipleRoleUserId(99) }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }

                assertRaises(ClassAccessDeniedError(classId)) {
                    operations.addClassContest(user = user, classId = classId, contestId = contestId)
                }

                verify(exactly = 0) { repository.update(any<Class>()) }
            }

            @Test
            fun `should raise ContestAccessDeniedError without saving if contest is not shared to any community`() {
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest()

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                verify(exactly = 0) { repository.update(any<Class>()) }
            }

            @Test
            fun `should raise ContestAccessDeniedError if contest is shared only to communities of other roles of the user`() {
                val user = testMultipleRoleUser {
                    roles {
                        manager {
                            memberOf(listOf(3))
                            data = managerData {}
                        }
                        developer {
                            memberOf(listOf(4))
                            data = developerData {}
                        }
                        student {
                            memberOf(listOf(5))
                            data = studentData {}
                        }
                    }
                }
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4, 5)) }

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addClassContest(user = user, classId = classId, contestId = contestId)
                }

                verify(exactly = 0) { repository.update(any<Class>()) }
            }

            @Test
            fun `should raise ContestAccessDeniedError for an unshared contest created by the Manager as a Developer`() {
                val user = testMultipleRoleUser {
                    roles {
                        manager {
                            memberOf(listOf(4))
                            data = managerData {}
                        }
                        developer { data = developerData { contests(listOf(19)) } }
                    }
                }
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { owner = MultipleRoleUserId(0) }

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addClassContest(user = user, classId = classId, contestId = contestId)
                }

                verify(exactly = 0) { repository.update(any<Class>()) }
            }

            @Test
            fun `should raise ContestAlreadyAddedToClassError without saving if contest is already added to the class`() {
                every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }

                assertRaises(ContestAlreadyAddedToClassError(classId = classId, contestId = contestId)) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                verify { repository.findById(classId) }
                verify { contestRepository.findById(contestId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @Test
            fun `should raise ContestAccessDeniedError for an already added contest that is no longer available`() {
                every { repository.findById(classId) } returns testClass { contests(listOf(19)) }
                every { contestRepository.findById(contestId) } returns testContest()

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                verify(exactly = 0) { repository.update(any<Class>()) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should save only the class without writing the contest`() {
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { repository.update(any<Class>()) } answers { firstArg() }

                operations.addClassContest(user = manager, classId = classId, contestId = contestId).getOrThrow()

                verify { repository.findById(classId) }
                verify { contestRepository.findById(contestId) }
                verify { repository.update(any<Class>()) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate a technical exception when saving the class`() {
                val failure = IllegalStateException("Class storage unavailable")
                every { repository.findById(classId) } returns testClass()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { repository.update(any<Class>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.addClassContest(user = manager, classId = classId, contestId = contestId)
                }

                assertSame(failure, actual)
            }
        }
    }

    @Nested
    inner class AddCompetitionContestTests {

        private val competitionId = CompetitionId(21)
        private val contestId = ContestId(19)
        private val manager = testManager {
            memberOf(listOf(4))
            data = managerData {}
        }

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should append the contest to the competition and save it once with the other data unchanged`() {
                val saved = testCompetition {
                    participants(listOf(31, 32))
                    contests(listOf(17, 18, 19))
                }
                every { competitionRepository.findById(competitionId) } returns testCompetition {
                    participants(listOf(31, 32))
                    contests(listOf(17, 18))
                }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { competitionRepository.update(any<Competition>()) } returns saved

                val actual = operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertSame(saved, actual)
                verify(exactly = 1) {
                    competitionRepository.update(
                        match<Competition> { updated ->
                            updated.id == CompetitionId(21) &&
                                updated.createdAt == Instant.EPOCH &&
                                updated.version == EntityVersion(0) &&
                                updated.data.owner.id == MultipleRoleUserId(0) &&
                                updated.data.name == "Existing competition" &&
                                updated.data.description == "Competition description" &&
                                updated.data.participants.ids == listOf(SingleRoleUserId(31), SingleRoleUserId(32)) &&
                                updated.data.contests.ids == listOf(ContestId(17), ContestId(18), ContestId(19))
                        },
                    )
                }
            }

            @Test
            fun `should allow a contest shared to several communities if only one of them has the Manager`() {
                val user = testManager {
                    memberOf(listOf(4, 6))
                    data = managerData {}
                }
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(3, 4, 5)) }
                every { competitionRepository.update(any<Competition>()) } answers { firstArg() }

                val actual = operations.addCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            }

            @Test
            fun `should add a contest without tasks and schedule`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { competitionRepository.update(any<Competition>()) } answers { firstArg() }

                val actual = operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            }

            @Test
            fun `should add a contest that is already added to another competition`() {
                val otherCompetition = competition {
                    id = 22
                    createdAt = Instant.EPOCH
                    version = EntityVersion(0)
                    data = competitionData {
                        owner = MultipleRoleUserId(0)
                        name = "Other competition"
                        description = ""
                        contests(listOf(19))
                    }
                }
                every { competitionRepository.findById(otherCompetition.id) } returns otherCompetition
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { competitionRepository.update(any<Competition>()) } answers { firstArg() }

                val actual = operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertEquals(competitionId, actual.id)
                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
                verify(exactly = 0) { competitionRepository.update(match<Competition> { updated -> updated.id == otherCompetition.id }) }
            }

            @Test
            fun `should allow a Manager who also has other roles`() {
                val user = testMultipleRoleUser {
                    roles {
                        manager {
                            memberOf(listOf(4))
                            data = managerData {}
                        }
                        student { data = studentData {} }
                        administrator {}
                    }
                }
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { competitionRepository.update(any<Competition>()) } answers { firstArg() }

                val actual = operations.addCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                    .getOrThrow()

                assertEquals(listOf(ContestId(19)), actual.data.contests.ids)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage if user is not a Manager`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) {
                    operations.addCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }

            @Test
            fun `should raise MissedManagerRoleError if user is only a Developer of the community the contest is shared to`() {
                val user = testDeveloper {
                    memberOf(listOf(4))
                    data = developerData {}
                }

                assertRaises(MissedManagerRoleError) {
                    operations.addCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                }

                verify { listOf(repository, competitionRepository, contestRepository, submissionRepository) wasNot Called }
            }

            @Test
            fun `should raise CompetitionNotExistsError before reading the contest if competition does not exist`() {
                every { competitionRepository.findById(competitionId) } returns null

                assertRaises(CompetitionNotExistsError(competitionId)) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify { competitionRepository.findById(competitionId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @Test
            fun `should raise ContestNotExistsError without saving if contest does not exist`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns null

                assertRaises(ContestNotExistsError(contestId)) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify { competitionRepository.findById(competitionId) }
                verify { contestRepository.findById(contestId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @Test
            fun `should raise ContestNotExistsError before checking access if contest for a foreign competition does not exist`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { owner = MultipleRoleUserId(99) }
                every { contestRepository.findById(contestId) } returns null

                assertRaises(ContestNotExistsError(contestId)) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            }

            @Test
            fun `should raise CompetitionAccessDeniedError without saving if competition belongs to another owner`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { owner = MultipleRoleUserId(99) }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }

                assertRaises(CompetitionAccessDeniedError(competitionId)) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            }

            @Test
            fun `should deny a foreign competition incorrectly included in the Manager competition snapshot`() {
                val user = testManager {
                    memberOf(listOf(4))
                    data = managerData { competitions(listOf(21)) }
                }
                every { competitionRepository.findById(competitionId) } returns testCompetition { owner = MultipleRoleUserId(99) }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }

                assertRaises(CompetitionAccessDeniedError(competitionId)) {
                    operations.addCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                }

                verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            }

            @Test
            fun `should raise ContestAccessDeniedError without saving if contest is not shared to any community`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest()

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            }

            @Test
            fun `should raise ContestAccessDeniedError if contest is shared only to communities of other roles of the user`() {
                val user = testMultipleRoleUser {
                    roles {
                        manager {
                            memberOf(listOf(3))
                            data = managerData {}
                        }
                        developer {
                            memberOf(listOf(4))
                            data = developerData {}
                        }
                        student {
                            memberOf(listOf(5))
                            data = studentData {}
                        }
                    }
                }
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4, 5)) }

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                }

                verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            }

            @Test
            fun `should raise ContestAccessDeniedError for an unshared contest created by the Manager as a Developer`() {
                val user = testMultipleRoleUser {
                    roles {
                        manager {
                            memberOf(listOf(4))
                            data = managerData {}
                        }
                        developer { data = developerData { contests(listOf(19)) } }
                    }
                }
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { owner = MultipleRoleUserId(0) }

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addCompetitionContest(user = user, competitionId = competitionId, contestId = contestId)
                }

                verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            }

            @Test
            fun `should raise ContestAlreadyAddedToCompetitionError without saving if contest is already added to the competition`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }

                assertRaises(ContestAlreadyAddedToCompetitionError(competitionId = competitionId, contestId = contestId)) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify { competitionRepository.findById(competitionId) }
                verify { contestRepository.findById(contestId) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }

            @Test
            fun `should raise ContestAccessDeniedError for an already added contest that is no longer available`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { contests(listOf(19)) }
                every { contestRepository.findById(contestId) } returns testContest()

                assertRaises(ContestAccessDeniedError(contestId)) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                verify(exactly = 0) { competitionRepository.update(any<Competition>()) }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should save only the competition without writing the contest`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { competitionRepository.update(any<Competition>()) } answers { firstArg() }

                operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId).getOrThrow()

                verify { competitionRepository.findById(competitionId) }
                verify { contestRepository.findById(contestId) }
                verify { competitionRepository.update(any<Competition>()) }
                confirmVerified(repository, competitionRepository, contestRepository, submissionRepository)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should propagate a technical exception when saving the competition`() {
                val failure = IllegalStateException("Competition storage unavailable")
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { contestRepository.findById(contestId) } returns testContest { sharedTo(listOf(4)) }
                every { competitionRepository.update(any<Competition>()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.addCompetitionContest(user = manager, competitionId = competitionId, contestId = contestId)
                }

                assertSame(failure, actual)
            }
        }
    }

    @Nested
    inner class CreateParticipantsTests {

        private val competitionId = CompetitionId(21)
        private val manager = testManager { data = managerData {} }
        private val canonicalUuid = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        private val allMocks = arrayOf(
            repository,
            competitionRepository,
            contestRepository,
            submissionRepository,
            participantRepository,
            competitionConfig,
        )

        @Nested
        inner class HappyPathTests {

            @Test
            fun `should save the requested number of canonical UUID access codes hashed by Identity in the competition once`() {
                val accessTokenHashes = slot<List<AccessTokenHash>>()
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { competitionConfig.maxParticipants } returns 10
                every { participantRepository.saveToCompetition(competitionId, capture(accessTokenHashes), any()) } returns emptyList()

                operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 2).getOrThrow()

                assertEquals(2, accessTokenHashes.captured.size)
                assertCanonicalIdentityUuid(accessTokenHashes.captured[0])
                assertCanonicalIdentityUuid(accessTokenHashes.captured[1])
                verify(exactly = 1) { participantRepository.saveToCompetition(competitionId, any(), any()) }
            }

            @Test
            fun `should name each participant st followed by its decimal id`() {
                val nameOf = slot<(SingleRoleUserId) -> String>()
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { competitionConfig.maxParticipants } returns 10
                every { participantRepository.saveToCompetition(competitionId, any(), capture(nameOf)) } returns emptyList()

                operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 1).getOrThrow()

                assertEquals("st42", nameOf.captured(SingleRoleUserId(42)))
            }

            @Test
            fun `should return exactly the participants saved by the port`() {
                val saved = listOf(testParticipant(41), testParticipant(42))
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { competitionConfig.maxParticipants } returns 10
                every { participantRepository.saveToCompetition(competitionId, any(), any()) } returns saved

                val actual = operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 2).getOrThrow()

                assertSame(saved, actual)
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
                val saved = listOf(testParticipant(41))
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { competitionConfig.maxParticipants } returns 10
                every { participantRepository.saveToCompetition(competitionId, any(), any()) } returns saved

                val actual = operations.createParticipants(user = user, competitionId = competitionId, participantCount = 1).getOrThrow()

                assertSame(saved, actual)
            }

            @Test
            fun `should create participants if the total equals the maximum`() {
                val accessTokenHashes = slot<List<AccessTokenHash>>()
                val saved = listOf(testParticipant(41), testParticipant(42))
                every { competitionRepository.findById(competitionId) } returns testCompetition { participants(listOf(31, 32)) }
                every { competitionConfig.maxParticipants } returns 4
                every { participantRepository.saveToCompetition(competitionId, capture(accessTokenHashes), any()) } returns saved

                val actual = operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 2).getOrThrow()

                assertSame(saved, actual)
                assertEquals(2, accessTokenHashes.captured.size)
            }
        }

        @Nested
        inner class RefusalTests {

            @Test
            fun `should raise MissedManagerRoleError before reading storage or configuration if user is not a Manager`() {
                val user = testAdministrator {}

                assertRaises(MissedManagerRoleError) {
                    operations.createParticipants(user = user, competitionId = competitionId, participantCount = 1)
                }

                verify { allMocks.toList() wasNot Called }
            }

            @Test
            fun `should raise CompetitionNotExistsError without creating participants if competition does not exist`() {
                every { competitionRepository.findById(competitionId) } returns null

                assertRaises(CompetitionNotExistsError(competitionId)) {
                    operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 1)
                }

                verify { competitionRepository.findById(competitionId) }
                confirmVerified(*allMocks)
            }

            @Test
            fun `should raise CompetitionAccessDeniedError without creating participants if competition belongs to another owner`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { owner = MultipleRoleUserId(99) }

                assertRaises(CompetitionAccessDeniedError(competitionId)) {
                    operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 1)
                }

                verify { competitionRepository.findById(competitionId) }
                confirmVerified(*allMocks)
            }

            @ParameterizedTest
            @ValueSource(ints = [0, -1])
            fun `should raise NonPositiveParticipantCountError without creating participants if the count is not positive`(count: Int) {
                every { competitionRepository.findById(competitionId) } returns testCompetition()

                assertRaises(NonPositiveParticipantCountError(count)) {
                    operations.createParticipants(user = manager, competitionId = competitionId, participantCount = count)
                }

                verify { competitionRepository.findById(competitionId) }
                confirmVerified(*allMocks)
            }

            @Test
            fun `should raise CompetitionParticipantLimitExceededError without creating participants if the total exceeds the maximum`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { participants(listOf(31, 32)) }
                every { competitionConfig.maxParticipants } returns 3

                assertRaises(
                    CompetitionParticipantLimitExceededError(
                        competitionId = competitionId,
                        currentParticipantCount = 2,
                        participantCount = 2,
                        maxParticipants = 3,
                    ),
                ) {
                    operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 2)
                }

                verify { participantRepository wasNot Called }
            }

            @Test
            fun `should raise CompetitionParticipantLimitExceededError if the competition already exceeds the maximum`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { participants(listOf(31, 32, 33)) }
                every { competitionConfig.maxParticipants } returns 2

                assertRaises(
                    CompetitionParticipantLimitExceededError(
                        competitionId = competitionId,
                        currentParticipantCount = 3,
                        participantCount = 1,
                        maxParticipants = 2,
                    ),
                ) {
                    operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 1)
                }

                verify { participantRepository wasNot Called }
            }

            @Test
            fun `should raise CompetitionParticipantLimitExceededError without overflow for Int MAX_VALUE new participants`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { participants(listOf(31)) }
                every { competitionConfig.maxParticipants } returns Int.MAX_VALUE

                assertRaises(
                    CompetitionParticipantLimitExceededError(
                        competitionId = competitionId,
                        currentParticipantCount = 1,
                        participantCount = Int.MAX_VALUE,
                        maxParticipants = Int.MAX_VALUE,
                    ),
                ) {
                    operations.createParticipants(user = manager, competitionId = competitionId, participantCount = Int.MAX_VALUE)
                }

                verify { participantRepository wasNot Called }
            }
        }

        @Nested
        inner class InvariantTests {

            @Test
            fun `should only read the competition and save the participants without updating other entities`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { participants(listOf(31)) }
                every { competitionConfig.maxParticipants } returns 10
                every { participantRepository.saveToCompetition(competitionId, any(), any()) } returns emptyList()

                operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 2).getOrThrow()

                verify { competitionRepository.findById(competitionId) }
                verify { competitionConfig.maxParticipants }
                verify { participantRepository.saveToCompetition(competitionId, any(), any()) }
                confirmVerified(*allMocks)
            }
        }

        @Nested
        inner class ModuleRuleTests {

            @Test
            fun `should read the maximum number of participants exactly once`() {
                every { competitionRepository.findById(competitionId) } returns testCompetition { participants(listOf(31)) }
                every { competitionConfig.maxParticipants } returns 10
                every { participantRepository.saveToCompetition(competitionId, any(), any()) } returns emptyList()

                operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 2).getOrThrow()

                verify(exactly = 1) { competitionConfig.maxParticipants }
            }

            @Test
            fun `should propagate a technical exception when saving the participants`() {
                val failure = IllegalStateException("Duplicate access code")
                every { competitionRepository.findById(competitionId) } returns testCompetition()
                every { competitionConfig.maxParticipants } returns 10
                every { participantRepository.saveToCompetition(competitionId, any(), any()) } throws failure

                val actual = assertThrows(IllegalStateException::class.java) {
                    operations.createParticipants(user = manager, competitionId = competitionId, participantCount = 1)
                }

                assertSame(failure, actual)
            }
        }

        private fun assertCanonicalIdentityUuid(hash: AccessTokenHash) {
            assertEquals(HashAlgorithm.Identity, hash.algorithm)
            assertTrue(canonicalUuid.matches(hash.value)) { "Access code is not a canonical UUID: ${hash.value}" }
        }

        private fun testParticipant(participantId: Long): Participant = participant {
            id = participantId
            createdAt = Instant.EPOCH
            version = EntityVersion(0)
            data = participantData {
                accessToken("code-$participantId", algorithm = HashAlgorithm.Identity)
                competition(21)
                name = "st$participantId"
            }
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
            invite(31)
            builder()
        }
    }

    private fun testClassInvite(code: String = "pqrstuvwxyz2", expiresAt: Instant = FAR_FUTURE): ClassInvite = classInvite {
        id = 31
        createdAt = Instant.EPOCH
        version = EntityVersion(0)
        data = classInviteData {
            this.code(code, HashAlgorithm.Identity)
            this.expiresAt = expiresAt
        }
    }

    private companion object {
        val INVITE_CODE_FORMAT = Regex("[a-hjkmnp-z2-9]{12}")
        val FAR_FUTURE: Instant = Instant.parse("2030-01-01T00:00:00Z")
    }
}
