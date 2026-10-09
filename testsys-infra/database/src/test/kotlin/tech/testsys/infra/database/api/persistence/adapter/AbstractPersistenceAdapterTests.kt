package tech.testsys.infra.database.api.persistence.adapter

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.support.TransactionOperations
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

    private fun saveParticipant(competitionId: CompetitionId) {
        val hash = AccessTokenHash(value = fixtures.unique("token"), algorithm = HashAlgorithm.Identity)
        participants.saveToCompetition(competitionId, listOf(hash)) { participantId -> "named-${participantId.value}" }
    }
}
