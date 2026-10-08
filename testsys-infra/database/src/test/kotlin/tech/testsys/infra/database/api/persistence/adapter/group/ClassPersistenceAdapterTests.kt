package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.`class`
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassInviteRepository
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.domain.model.group.InviteCodeHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.infra.database.api.persistence.adapter.UpdatablePersistenceAdapterContractTests
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.group.ContestToClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.StudentToClassJpaEntityRepository
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
}
