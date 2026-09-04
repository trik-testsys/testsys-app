package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.classData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.model.group.Class
import tech.testsys.domain.model.group.ClassData
import tech.testsys.domain.model.group.ClassId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import tech.testsys.infra.database.internal.InternalDatabaseApi
import tech.testsys.infra.database.internal.jpa.repository.group.ContestToClassJpaEntityRepository
import tech.testsys.infra.database.internal.jpa.repository.group.StudentToClassJpaEntityRepository
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(InternalDatabaseApi::class)
class ClassPersistenceAdapterTest : PersistenceAdapterContractTest<ClassData, ClassId, Class>() {

    @Autowired
    override lateinit var repository: ClassRepository

    @Autowired
    private lateinit var studentToClassJpaEntityRepository: StudentToClassJpaEntityRepository

    @Autowired
    private lateinit var contestToClassJpaEntityRepository: ContestToClassJpaEntityRepository

    override fun newData(): ClassData {
        val ownerId = fixtures.manager().id.value
        val studentIds = listOf(fixtures.student().id.value, fixtures.student().id.value)
        val contestIds = listOf(fixtures.contest().id.value)
        return classData {
            owner(ownerId)
            name = fixtures.unique("Class")
            description = "Class description"
            students(studentIds)
            contests(contestIds)
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

    override fun idOf(value: Long) = ClassId(value)

    override fun assertSameData(expected: Class, actual: Class) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.students.ids.toSet(), actual.data.students.ids.toSet())
        assertEquals(expected.data.contests.ids.toSet(), actual.data.contests.ids.toSet())
    }

    @Test
    fun `update reconciles the student and contest join rows`() {
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
    fun `a class without members survives a round trip`() {
        val ownerId = fixtures.manager().id.value

        val saved = repository.save(
            classData {
                owner(ownerId)
                name = fixtures.unique("Empty class")
                description = "No students yet"
            },
        )

        val found = assertNotNull(repository.findById(saved.id))
        assertEquals(emptyList(), found.data.students.ids)
        assertEquals(emptyList(), found.data.contests.ids)
    }
}
