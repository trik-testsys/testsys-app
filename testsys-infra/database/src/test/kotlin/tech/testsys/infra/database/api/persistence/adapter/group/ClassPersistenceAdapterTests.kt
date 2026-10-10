package tech.testsys.infra.database.api.persistence.adapter.group

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.support.TransactionOperations
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.task.ContestId
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.domain.model.user.MultipleRoleUserId
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.group.ContestToClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.StudentToClassJpaEntityRepository
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(InternalDatabaseApi::class)
class ClassPersistenceAdapterTests : UpdatablePersistenceAdapterContractTests<ClassData, ClassId, Class>() {

    @Autowired
    override lateinit var repository: ClassRepository

    @Autowired
    private lateinit var studentToClassJpaEntityRepository: StudentToClassJpaEntityRepository

    @Autowired
    private lateinit var contestToClassJpaEntityRepository: ContestToClassJpaEntityRepository

    @Autowired
    private lateinit var classInvites: ClassInviteRepository

    @Autowired
    private lateinit var transactions: TransactionOperations

    @Autowired
    private lateinit var multipleRoleUsers: MultipleRoleUserRepository

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    override fun newData(): ClassData {
        val ownerId = fixtures.manager().id.value
        val studentIds = listOf(fixtures.student().id.value, fixtures.student().id.value)
        val contestIds = listOf(fixtures.contest().id.value)
        val inviteId = fixtures.classInvite().id
        return classData {
            owner(ownerId)
            name = fixtures.unique("Class")
            description = "Class description"
            students(studentIds)
            contests(contestIds)
            invite = inviteId
        }
    }

    override fun modified(entity: Class): Class {
        val keptStudent = entity.data.students.ids.first()
        val newStudent = fixtures.student().id
        return entity.withData {
            name = fixtures.unique("Renamed class")
            description = "Updated description"
            students = mutableListOf(keptStudent, newStudent)
            contests = mutableListOf()
        }
    }

    override fun detached(entity: Class) = `class` {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = ClassId(value)

    override fun assertSameData(expected: Class, actual: Class) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.students.ids.toSet(), actual.data.students.ids.toSet())
        assertEquals(expected.data.contests.ids.toSet(), actual.data.contests.ids.toSet())
        assertEquals(expected.data.invite.id, actual.data.invite.id)
    }

    @Test
    fun `should reconcile the student and contest join rows on update`() {
        val saved = repository.save(newData())
        val keptStudent = saved.data.students.ids.first()
        val addedStudent = fixtures.student().id
        val addedContest = fixtures.contest().id

        repository.update(
            saved.withData {
                students = mutableListOf(keptStudent, addedStudent)
                contests = mutableListOf(addedContest)
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(setOf(keptStudent, addedStudent), found.data.students.ids.toSet())
        assertEquals(setOf(addedContest), found.data.contests.ids.toSet())
        assertEquals(2, studentToClassJpaEntityRepository.findAllByClassId(saved.id.value).size)
        assertEquals(1, contestToClassJpaEntityRepository.findAllByClassId(saved.id.value).size)
    }

    @Test
    fun `should save one student and contest link per id when ids repeat`() {
        val studentId = fixtures.student().id
        val contestId = fixtures.contest().id

        val saved = repository.save(
            classDataOf(students = listOf(studentId, studentId), contests = listOf(contestId, contestId)),
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(listOf(studentId), found.data.students.ids)
        assertEquals(listOf(contestId), found.data.contests.ids)
    }

    @Test
    fun `should add one and twenty students on update with the same statement count`() {
        val studentIds = List(20) { fixtures.student().id }
        val oneStudentClass = repository.save(classDataOf())
        val twentyStudentsClass = repository.save(classDataOf())

        val (_, oneStudentStatements) = withStatementCount {
            repository.update(oneStudentClass.withData { students = studentIds.take(1).toMutableList() })
        }
        val (_, twentyStudentsStatements) = withStatementCount {
            repository.update(twentyStudentsClass.withData { students = studentIds.toMutableList() })
        }

        assertEquals(studentIds.toSet(), assertNotNull(repository.findById(twentyStudentsClass.id)).data.students.ids.toSet())
        assertEquals(oneStudentStatements, twentyStudentsStatements)
    }

    @Test
    fun `should keep a class without members through a round trip`() {
        val ownerId = fixtures.manager().id.value
        val inviteId = fixtures.classInvite().id

        val saved = repository.save(
            classData {
                owner(ownerId)
                name = fixtures.unique("Empty class")
                description = "No students yet"
                invite = inviteId
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(emptyList(), found.data.students.ids)
        assertEquals(emptyList(), found.data.contests.ids)
    }

    @Test
    fun `should keep the owner if another owner is passed on update`() {
        val saved = repository.save(newData())
        val otherOwner = fixtures.manager().id

        val updated = repository.update(saved.withData { owner = otherOwner })

        assertEquals(saved.data.owner.id, updated.data.owner.id)
        assertEquals(saved.data.owner.id, assertNotNull(repository.findById(saved.id)).data.owner.id)
    }

    @Test
    fun `should enroll a new student and keep other students and contests on addStudent`() {
        val saved = repository.save(newData())
        val newStudent = fixtures.student().id

        val updated = repository.addStudent(saved.id, newStudent)

        val expectedStudents = (saved.data.students.ids + newStudent).toSet()
        assertEquals(expectedStudents, updated.data.students.ids.toSet())
        assertEquals(expectedStudents, assertNotNull(repository.findById(saved.id)).data.students.ids.toSet())
        assertEquals(saved.data.contests.ids, updated.data.contests.ids)
        assertEquals(saved.data.name, updated.data.name)
    }

    @Test
    fun `should not add a second row for an already enrolled student on addStudent`() {
        val saved = repository.save(newData())
        val enrolled = saved.data.students.ids.first()

        val updated = repository.addStudent(saved.id, enrolled)

        assertEquals(saved.data.students.ids.toSet(), updated.data.students.ids.toSet())
        assertEquals(1, studentToClassJpaEntityRepository.findAllByClassId(saved.id.value).count { it.id.studentId == enrolled.value })
    }

    @Test
    fun `should increment the class version on addStudent`() {
        val saved = repository.save(newData())

        val updated = repository.addStudent(saved.id, fixtures.student().id)

        assertEquals(assertNotNull(saved.version).value + 1, assertNotNull(updated.version).value)
        assertEquals(updated.version, assertNotNull(repository.findById(saved.id)).version)
    }

    @Test
    fun `should increment the student version on addStudent`() {
        val saved = repository.save(classDataOf())
        val student = fixtures.student()

        repository.addStudent(saved.id, student.id)

        val stored = assertNotNull(multipleRoleUsers.findById(student.id))
        assertEquals(assertNotNull(student.version).value + 1, assertNotNull(stored.version).value)
    }

    @Test
    fun `should increment the versions of the students enrolled on save`() {
        val students = listOf(fixtures.student(), fixtures.student())

        repository.save(classDataOf(students = students.map { student -> student.id }))

        students.forEach { student ->
            val stored = assertNotNull(multipleRoleUsers.findById(student.id))
            assertEquals(assertNotNull(student.version).value + 1, assertNotNull(stored.version).value)
        }
    }

    @Test
    fun `should increment the versions of the added students only on update`() {
        val kept = fixtures.student()
        val saved = repository.save(classDataOf(students = listOf(kept.id)))
        val keptVersion = assertNotNull(multipleRoleUsers.findById(kept.id)).version
        val added = fixtures.student()

        repository.update(saved.withData { students = mutableListOf(kept.id, added.id) })

        assertEquals(keptVersion, assertNotNull(multipleRoleUsers.findById(kept.id)).version)
        assertEquals(assertNotNull(added.version).value + 1, assertNotNull(multipleRoleUsers.findById(added.id)?.version).value)
    }

    @Test
    fun `should accept a student token read before the enrolment in the same transaction`() {
        val saved = repository.save(classDataOf())
        val student = fixtures.student()
        val renamed = fixtures.unique("Renamed student")

        val updated = transactions.execute {
            val current = assertNotNull(multipleRoleUsers.findById(student.id))
            repository.update(saved.withData { students = mutableListOf(student.id) })
            multipleRoleUsers.update(current.withData { name = renamed })
        }

        val stored = assertNotNull(multipleRoleUsers.findById(student.id))
        assertEquals(renamed, stored.data.name)
        assertEquals(assertNotNull(student.version).value + 2, assertNotNull(stored.version).value)
        assertEquals(stored.version, updated.version)
    }

    @Test
    fun `should accept a student token read outside the enrolment transaction`() {
        val saved = repository.save(classDataOf())
        val student = fixtures.student()

        val updated = transactions.execute {
            repository.update(saved.withData { students = mutableListOf(student.id) })
            multipleRoleUsers.update(student.withData { name = "Renamed after enrolment" })
        }

        val stored = assertNotNull(multipleRoleUsers.findById(student.id))
        assertEquals("Renamed after enrolment", stored.data.name)
        assertEquals(assertNotNull(student.version).value + 2, assertNotNull(stored.version).value)
        assertEquals(stored.version, updated.version)
    }

    @Test
    fun `should increment each student once for repeated batch enrolments across a cleared context`() {
        val first = fixtures.student()
        val shared = fixtures.student()
        val last = fixtures.student()
        val firstClass = repository.save(classDataOf())
        val sameClass = repository.save(classDataOf())
        val overlappingClass = repository.save(classDataOf())

        transactions.execute {
            repository.update(firstClass.withData { students = mutableListOf(first.id, shared.id) })
            repository.update(sameClass.withData { students = mutableListOf(first.id, shared.id) })
            entityManager.flush()
            entityManager.clear()
            repository.update(overlappingClass.withData { students = mutableListOf(shared.id, last.id) })
        }

        assertEquals(assertNotNull(first.version).value + 1, assertNotNull(multipleRoleUsers.findById(first.id)?.version).value)
        assertEquals(assertNotNull(shared.version).value + 1, assertNotNull(multipleRoleUsers.findById(shared.id)?.version).value)
        assertEquals(assertNotNull(last.version).value + 1, assertNotNull(multipleRoleUsers.findById(last.id)?.version).value)
        assertEquals(setOf(first.id, shared.id), assertNotNull(repository.findById(sameClass.id)).data.students.ids.toSet())
        assertEquals(setOf(shared.id, last.id), assertNotNull(repository.findById(overlappingClass.id)).data.students.ids.toSet())
    }

    @Test
    fun `should increment the class version on an update of the students only`() {
        val saved = repository.save(classDataOf())

        val updated = repository.update(saved.withData { students = mutableListOf(fixtures.student().id) })

        assertEquals(assertNotNull(saved.version).value + 1, assertNotNull(updated.version).value)
        assertEquals(updated.version, assertNotNull(repository.findById(saved.id)).version)
    }

    @Test
    fun `should reject a stale class update after a student joined and keep the student`() {
        val saved = repository.save(classDataOf())
        val student = fixtures.student().id
        val contest = fixtures.contest().id
        repository.addStudent(saved.id, student)

        assertFailsWith<OptimisticLockingFailureException> {
            repository.update(saved.withData { contests = mutableListOf(contest) })
        }

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(listOf(student), found.data.students.ids)
        assertEquals(emptyList(), found.data.contests.ids)
    }

    @Test
    fun `should keep a student who joins while an update of the class read before is in progress`() {
        val saved = repository.save(classDataOf())
        val student = fixtures.student().id
        val contest = fixtures.contest().id
        val classRead = CountDownLatch(1)
        val studentJoined = CountDownLatch(1)

        Executors.newFixedThreadPool(2).use { pool ->
            val update = pool.submit(
                Callable {
                    transactions.execute {
                        val current = assertNotNull(repository.findById(saved.id))
                        classRead.countDown()
                        check(studentJoined.await(10, TimeUnit.SECONDS))
                        repository.update(current.withData { contests = mutableListOf(contest) })
                    }
                },
            )
            val join = pool.submit(
                Callable {
                    check(classRead.await(10, TimeUnit.SECONDS))
                    repository.addStudent(saved.id, student).also { studentJoined.countDown() }
                },
            )
            join.get()
            update.get()
        }

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(listOf(student), found.data.students.ids)
        assertEquals(listOf(contest), found.data.contests.ids)
    }

    @Test
    fun `should save the invite and the class referencing it on saveWithInvite`() {
        val ownerId = fixtures.manager().id.value

        val saved = repository.saveWithInvite(fixtures.classInviteDataOf(code = "abcdefghjkmn")) { inviteId ->
            classData {
                owner(ownerId)
                name = fixtures.unique("Class")
                description = "Class description"
                invite = inviteId
            }
        }

        val invite = assertNotNull(classInvites.findById(saved.data.invite.id))
        assertEquals("abcdefghjkmn", invite.data.codeHash.value)
        assertEquals(saved.data.invite.id, assertNotNull(repository.findById(saved.id)).data.invite.id)
    }

    @Test
    fun `should save neither the invite nor the class if saving the class fails on saveWithInvite`() {
        val ownerId = fixtures.manager().id.value
        val usedInvite = fixtures.studentClass().data.invite.id

        assertFailsWith<DataIntegrityViolationException> {
            repository.saveWithInvite(fixtures.classInviteDataOf(code = "abcdefghjkmn")) {
                classData {
                    owner(ownerId)
                    name = fixtures.unique("Class")
                    description = "Class description"
                    invite = usedInvite
                }
            }
        }

        assertNull(classInvites.findByCode(InviteCodeHash(value = "abcdefghjkmn", algorithm = HashAlgorithm.Identity)))
    }

    @Test
    fun `should find the class by its invite`() {
        val studyClass = fixtures.studentClass()
        fixtures.studentClass()

        val found = repository.findByInvite(studyClass.data.invite.id)

        assertEquals(studyClass.id, assertNotNull(found).id)
    }

    @Test
    fun `should return null if no class references the invite`() {
        val invite = fixtures.classInvite()

        assertNull(repository.findByInvite(invite.id))
    }

    @Test
    fun `should keep the invite reference and the stored invite if another invite is passed on update`() {
        val saved = repository.save(newData())
        val storedInvite = assertNotNull(classInvites.findById(saved.data.invite.id))
        val otherInvite = fixtures.classInvite()

        val updated = repository.update(saved.withData { invite = otherInvite.id })

        assertEquals(saved.data.invite.id, updated.data.invite.id)
        assertEquals(saved.data.invite.id, assertNotNull(repository.findById(saved.id)).data.invite.id)
        val reloadedInvite = assertNotNull(classInvites.findById(saved.data.invite.id))
        assertEquals(storedInvite.data.codeHash, reloadedInvite.data.codeHash)
        assertEquals(storedInvite.version, reloadedInvite.version)
    }

    @Test
    fun `should reject a class referencing an invite of another class`() {
        val usedInvite = fixtures.studentClass().data.invite.id
        val ownerId = fixtures.manager().id.value

        assertFailsWith<DataIntegrityViolationException> {
            repository.save(
                classData {
                    owner(ownerId)
                    name = fixtures.unique("Class")
                    description = "Class description"
                    invite = usedInvite
                },
            )
        }
    }

    @Test
    fun `should remove the invite together with the class`() {
        val studyClass = fixtures.studentClass()

        repository.removeById(studyClass.id)

        assertNull(classInvites.findById(studyClass.data.invite.id))
    }

    @Test
    fun `should fail to add a student to a missing class`() {
        val student = fixtures.student().id

        assertFailsWith<IllegalArgumentException> { repository.addStudent(ClassId(UNKNOWN_ID), student) }
    }

    @Test
    fun `should find classes by ids with the same statement count for one and twenty ids`() {
        val ownerId = fixtures.manager().id.value
        val studentIds = listOf(fixtures.student().id.value, fixtures.student().id.value)
        val contestIds = listOf(fixtures.contest().id.value)
        val ids = List(20) {
            val inviteId = fixtures.classInvite().id
            repository.save(
                classData {
                    owner(ownerId)
                    name = fixtures.unique("Class")
                    description = "Class description"
                    students(studentIds)
                    contests(contestIds)
                    invite = inviteId
                },
            ).id
        }

        val (one, oneIdStatements) = withStatementCount { repository.findByIds(ids.take(1)) }
        val (twenty, twentyIdsStatements) = withStatementCount { repository.findByIds(ids) }

        assertEquals(ids.take(1), one.map { studyClass -> studyClass.id })
        assertEquals(ids.toSet(), twenty.map { studyClass -> studyClass.id }.toSet())
        assertEquals(
            List(20) { studentIds.toSet() },
            twenty.map { studyClass -> studyClass.data.students.ids.map { student -> student.value }.toSet() },
        )
        assertEquals(List(20) { contestIds }, twenty.map { studyClass -> studyClass.data.contests.ids.map { contest -> contest.value } })
        assertEquals(oneIdStatements, twentyIdsStatements)
    }

    private fun classDataOf(students: List<MultipleRoleUserId> = emptyList(), contests: List<ContestId> = emptyList()): ClassData {
        val ownerId = fixtures.manager().id.value
        val inviteId = fixtures.classInvite().id
        return classData {
            owner(ownerId)
            name = fixtures.unique("Class")
            description = "Class description"
            this.students = students.toMutableList()
            this.contests = contests.toMutableList()
            invite = inviteId
        }
    }
}
