package tech.testsys.infra.database.api.persistence.adapter.entry

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import tech.testsys.domain.builder.api.studentContestEntryData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.ContestRepository
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.contract.persistence.repository.StudentContestEntryRepository
import tech.testsys.domain.model.entry.StudentContestEntry
import tech.testsys.domain.model.entry.StudentContestEntryData
import tech.testsys.domain.model.entry.StudentContestEntryId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StudentContestEntryPersistenceAdapterTests :
    PersistenceAdapterContractTests<StudentContestEntryData, StudentContestEntryId, StudentContestEntry>() {

    @Autowired
    override lateinit var repository: StudentContestEntryRepository

    @Autowired
    private lateinit var classRepository: ClassRepository

    @Autowired
    private lateinit var contestRepository: ContestRepository

    @Autowired
    private lateinit var multipleRoleUserRepository: MultipleRoleUserRepository

    override fun newData(): StudentContestEntryData {
        val user = fixtures.student()
        val studyClass = fixtures.studentClass()
        val contest = fixtures.contest()
        return studentContestEntryData {
            this.user = user.id
            this.studyClass = studyClass.id
            this.contest = contest.id
            enteredAt = Instant.parse("2026-01-01T00:00:00.123456Z")
        }
    }

    override fun idOf(value: Long) = StudentContestEntryId(value)

    override fun assertSameData(expected: StudentContestEntry, actual: StudentContestEntry) {
        assertEquals(expected.data.user.id, actual.data.user.id)
        assertEquals(expected.data.studyClass.id, actual.data.studyClass.id)
        assertEquals(expected.data.contest.id, actual.data.contest.id)
        assertEquals(expected.data.enteredAt, actual.data.enteredAt)
    }

    @Test
    fun `should reject update without changing the first moment`() {
        val saved = repository.save(newData())
        val changed = saved.withData { enteredAt = Instant.ofEpochSecond(70) }

        assertFailsWith<UnsupportedOperationException> { repository.update(changed) }

        assertSameData(saved, assertNotNull(repository.findById(saved.id)))
    }

    @Test
    fun `should find only the full context and preserve microsecond precision`() {
        val saved = repository.save(newData())

        val found = assertNotNull(
            repository.findByContext(
                userId = saved.data.user.id,
                studyClassId = saved.data.studyClass.id,
                contestId = saved.data.contest.id,
            ),
        )

        assertSameEntity(saved, found)
    }

    @Test
    fun `should return null for a context without an entry`() {
        val data = newData()

        val found = repository.findByContext(
            userId = data.user.id,
            studyClassId = data.studyClass.id,
            contestId = data.contest.id,
        )

        assertNull(found)
    }

    @Test
    fun `should keep the first moment when creation is repeated`() {
        val first = repository.findOrCreate(newData())
        val changed = studentContestEntryData {
            user = first.data.user.id
            studyClass = first.data.studyClass.id
            contest = first.data.contest.id
            enteredAt = Instant.ofEpochSecond(99)
        }

        val repeated = repository.findOrCreate(changed)

        assertSameEntity(first, repeated)
    }

    @Test
    fun `should reject a second direct save of the same context`() {
        val data = newData()
        repository.save(data)

        assertFailsWith<DataIntegrityViolationException> { repository.save(data) }
    }

    @Test
    fun `should atomically create one entry for concurrent calls`() {
        val data = newData()
        val gate = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)

        val entries = executor.use { pool ->
            pool.invokeAll(
                listOf(
                    Callable {
                        gate.countDown()
                        check(gate.await(10, TimeUnit.SECONDS))
                        repository.findOrCreate(data)
                    },
                    Callable {
                        gate.countDown()
                        check(gate.await(10, TimeUnit.SECONDS))
                        repository.findOrCreate(data)
                    },
                ),
            ).map { result -> result.get() }
        }

        assertEquals(entries[0].id, entries[1].id)
        assertEquals(data.enteredAt, entries[0].data.enteredAt)
        assertEquals(entries[0].data.enteredAt, entries[1].data.enteredAt)
    }

    @Test
    fun `should select entries only for requested contests in the selected context`() {
        val first = repository.save(newData())
        repository.save(newData())

        val found = repository.findByContests(
            userId = first.data.user.id,
            studyClassId = first.data.studyClass.id,
            contestIds = listOf(first.data.contest.id),
        )

        assertEquals(listOf(first.id), found.map { it.id })
    }

    @Test
    fun `should return an empty list for no selected contests`() {
        val data = newData()

        val found = repository.findByContests(
            userId = data.user.id,
            studyClassId = data.studyClass.id,
            contestIds = emptyList(),
        )

        assertEquals(emptyList(), found)
    }

    @Test
    fun `should keep first entries independent for another user`() {
        val first = repository.findOrCreate(newData())
        val otherId = fixtures.student().id
        val otherData = studentContestEntryData {
            enteredAt = Instant.ofEpochSecond(30)
            user = otherId
            studyClass = first.data.studyClass.id
            contest = first.data.contest.id
        }

        val other = repository.findOrCreate(otherData)

        assertEquals(Instant.ofEpochSecond(30), other.data.enteredAt)
        assertEquals(first.data.enteredAt, assertNotNull(repository.findById(first.id)).data.enteredAt)
        kotlin.test.assertNotEquals(first.id, other.id)
    }

    @Test
    fun `should keep first entries independent for another studyClass`() {
        val first = repository.findOrCreate(newData())
        val otherId = fixtures.studentClass().id
        val otherData = studentContestEntryData {
            enteredAt = Instant.ofEpochSecond(30)
            user = first.data.user.id
            studyClass = otherId
            contest = first.data.contest.id
        }

        val other = repository.findOrCreate(otherData)

        assertEquals(Instant.ofEpochSecond(30), other.data.enteredAt)
        assertEquals(first.data.enteredAt, assertNotNull(repository.findById(first.id)).data.enteredAt)
        kotlin.test.assertNotEquals(first.id, other.id)
    }

    @Test
    fun `should keep first entries independent for another contest`() {
        val first = repository.findOrCreate(newData())
        val otherId = fixtures.contest().id
        val otherData = studentContestEntryData {
            enteredAt = Instant.ofEpochSecond(30)
            user = first.data.user.id
            studyClass = first.data.studyClass.id
            contest = otherId
        }

        val other = repository.findOrCreate(otherData)

        assertEquals(Instant.ofEpochSecond(30), other.data.enteredAt)
        assertEquals(first.data.enteredAt, assertNotNull(repository.findById(first.id)).data.enteredAt)
        kotlin.test.assertNotEquals(first.id, other.id)
    }

    @Test
    fun `should cascade entry deletion when its class is deleted`() {
        val entry = repository.save(newData())

        classRepository.removeById(entry.data.studyClass.id)

        assertNull(repository.findById(entry.id))
    }

    @Test
    fun `should cascade entry deletion when its student is deleted`() {
        val entry = repository.save(newData())

        multipleRoleUserRepository.removeById(entry.data.user.id)

        assertNull(repository.findById(entry.id))
    }

    @Test
    fun `should retain entry when membership and contest links are removed and restored`() {
        val user = fixtures.student()
        val studyClass = fixtures.studentClass(students = listOf(user))
        val contest = fixtures.contest()
        val assigned = classRepository.update(studyClass.withData { contests(listOf(contest.id.value)) })
        val entry = fixtures.studentContestEntry(user = user, studyClass = assigned, contest = contest)
        val removed = classRepository.update(
            assigned.withData {
                students = mutableListOf()
                contests = mutableListOf()
            },
        )

        classRepository.update(
            removed.withData {
                students(listOf(user.id.value))
                contests(listOf(contest.id.value))
            },
        )

        assertEquals(entry.data.enteredAt, assertNotNull(repository.findById(entry.id)).data.enteredAt)
    }

    @Test
    fun `should cascade entry deletion when its contest is deleted`() {
        val entry = repository.save(newData())

        contestRepository.removeById(entry.data.contest.id)

        assertNull(repository.findById(entry.id))
    }
}
