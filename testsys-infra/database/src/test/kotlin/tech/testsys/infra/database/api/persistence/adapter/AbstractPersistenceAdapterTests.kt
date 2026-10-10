package tech.testsys.infra.database.api.persistence.adapter

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionOperations
import org.springframework.transaction.support.TransactionTemplate
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.ClassRepository
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.contract.persistence.repository.ParticipantRepository
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.domain.model.user.AccessTokenHash
import tech.testsys.domain.model.user.HashAlgorithm
import tech.testsys.infra.database.DatabaseIntegrationTests
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Checks the version rule of `touchRoot` through adapters that write into their own and other aggregates
 * within one transaction.
 */
class AbstractPersistenceAdapterTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var transactions: TransactionOperations

    @Autowired
    private lateinit var competitions: CompetitionRepository

    @Autowired
    private lateinit var participants: ParticipantRepository

    @Autowired
    private lateinit var classes: ClassRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    @Test
    fun `should accept a token read before a guard write of the same transaction`() {
        val competition = fixtures.competition()
        val renamed = fixtures.unique("Renamed")

        transactions.execute {
            saveParticipant(competition.id)
            competitions.update(competition.withData { name = renamed })
        }

        assertEquals(renamed, assertNotNull(competitions.findById(competition.id)).data.name)
    }

    @Test
    fun `should reject a token read before a data change of the same transaction`() {
        val studyClass = fixtures.studentClass()
        val student = fixtures.student()

        assertFailsWith<OptimisticLockingFailureException> {
            transactions.execute {
                classes.addStudent(studyClass.id, student.id)
                classes.update(studyClass.withData { name = fixtures.unique("Renamed") })
            }
        }

        assertEquals(studyClass.data.name, assertNotNull(classes.findById(studyClass.id)).data.name)
        assertEquals(emptyList(), assertNotNull(classes.findById(studyClass.id)).data.students.ids)
    }

    @Test
    fun `should increment the root version once for several guard writes of one transaction`() {
        val competition = fixtures.competition()

        transactions.execute {
            saveParticipant(competition.id)
            saveParticipant(competition.id)
        }

        val stored = assertNotNull(competitions.findById(competition.id))
        assertEquals(assertNotNull(competition.version).value + 1, assertNotNull(stored.version).value)
    }

    @Test
    fun `should return the version stored in the database from update`() {
        val studyClass = fixtures.studentClass()
        val student = fixtures.student()

        val updated = classes.update(studyClass.withData { students = mutableListOf(student.id) })

        assertEquals(assertNotNull(classes.findById(studyClass.id)).version, updated.version)
    }

    @Test
    fun `should reject a token read before a data change after clearing the persistence context`() {
        val studyClass = fixtures.studentClass()
        val student = fixtures.student()

        assertFailsWith<OptimisticLockingFailureException> {
            transactions.execute {
                classes.addStudent(studyClass.id, student.id)
                entityManager.clear()
                classes.update(studyClass.withData { name = "Stale update" })
            }
        }

        val stored = assertNotNull(classes.findById(studyClass.id))
        assertEquals(studyClass.version, stored.version)
        assertEquals(emptyList(), stored.data.students.ids)
    }

    @Test
    fun `should not force increment a new root after clearing the persistence context`() {
        val stored = transactions.execute {
            val competition = fixtures.competition()
            entityManager.flush()
            entityManager.clear()
            saveParticipant(competition.id)
            assertNotNull(competitions.findById(competition.id))
        }

        assertEquals(0L, assertNotNull(stored.version).value)
        assertEquals(stored.version, assertNotNull(competitions.findById(stored.id)).version)
        assertEquals(1, stored.data.participants.ids.size)
    }

    @Test
    fun `should reject a token read before a guard write in a completed transaction`() {
        val competition = fixtures.competition()
        transactions.execute { saveParticipant(competition.id) }

        assertFailsWith<OptimisticLockingFailureException> {
            competitions.update(competition.withData { name = "Stale update" })
        }

        val stored = assertNotNull(competitions.findById(competition.id))
        assertEquals(assertNotNull(competition.version).value + 1, assertNotNull(stored.version).value)
        assertEquals(competition.data.name, stored.data.name)
    }

    @Test
    fun `should discard version state after rollback`() {
        val competition = fixtures.competition()
        transactions.execute { status ->
            competitions.update(competition.withData { name = "Rolled back name" })
            status.setRollbackOnly()
        }

        val updated = transactions.execute {
            saveParticipant(competition.id)
            competitions.update(competition.withData { name = "Committed name" })
        }

        assertEquals(assertNotNull(competition.version).value + 2, assertNotNull(updated.version).value)
        assertEquals("Committed name", updated.data.name)
        assertEquals(1, updated.data.participants.ids.size)
        assertEquals(updated.version, assertNotNull(competitions.findById(competition.id)).version)
    }

    @Test
    fun `should restore guard write state after a requires new transaction`() {
        val competition = fixtures.competition()
        val separate = fixtures.competition()
        val requiresNew = TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

        val updated = transactions.execute {
            saveParticipant(competition.id)
            requiresNew.executeWithoutResult { saveParticipant(separate.id) }
            entityManager.clear()
            competitions.update(competition.withData { name = "Renamed after nested transaction" })
        }

        assertEquals(assertNotNull(competition.version).value + 2, assertNotNull(updated.version).value)
        assertEquals(updated.version, assertNotNull(competitions.findById(competition.id)).version)
        assertEquals("Renamed after nested transaction", updated.data.name)
    }

    @Test
    fun `should not accept the initial token from a completed requires new transaction`() {
        val competition = fixtures.competition()
        val requiresNew = TransactionTemplate(transactionManager).apply {
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
        }

        assertFailsWith<OptimisticLockingFailureException> {
            TransactionTemplate(transactionManager).executeWithoutResult {
                requiresNew.executeWithoutResult { saveParticipant(competition.id) }
                competitions.update(competition.withData { name = "Stale outer update" })
            }
        }

        val stored = assertNotNull(competitions.findById(competition.id))
        assertEquals(assertNotNull(competition.version).value + 1, assertNotNull(stored.version).value)
        assertEquals(competition.data.name, stored.data.name)
    }

    private fun saveParticipant(competitionId: CompetitionId) {
        val hash = AccessTokenHash(value = fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
        participants.saveToCompetition(competitionId, listOf(hash)) { participantId -> "named-${participantId.value}" }
    }
}
