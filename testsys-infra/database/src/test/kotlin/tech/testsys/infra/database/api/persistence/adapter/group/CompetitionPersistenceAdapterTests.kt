package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.competition
import tech.testsys.domain.builder.api.competitionData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CompetitionRepository
import tech.testsys.domain.model.group.Competition
import tech.testsys.domain.model.group.CompetitionData
import tech.testsys.domain.model.group.CompetitionId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTests
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CompetitionPersistenceAdapterTests : PersistenceAdapterContractTests<CompetitionData, CompetitionId, Competition>() {

    @Autowired
    override lateinit var repository: CompetitionRepository

    override fun newData(): CompetitionData {
        val ownerId = fixtures.manager().id.value
        val contestIds = listOf(fixtures.contest().id.value, fixtures.contest().id.value)
        return competitionData {
            owner(ownerId)
            name = fixtures.unique("Competition")
            description = "Competition description"
            contests(contestIds)
        }
    }

    override fun modified(entity: Competition): Competition {
        val keptContest = entity.data.contests.ids.first()
        val newContest = fixtures.contest().id
        return entity.withData {
            name = fixtures.unique("Renamed competition")
            description = "Updated description"
            contests = mutableListOf(keptContest, newContest)
        }
    }

    override fun detached(entity: Competition) = competition {
        id = entity.id.value
        createdAt = entity.createdAt
        data = entity.data
    }

    override fun idOf(value: Long) = CompetitionId(value)

    override fun assertSameData(expected: Competition, actual: Competition) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
        assertEquals(expected.data.participants.ids.toSet(), actual.data.participants.ids.toSet())
        assertEquals(expected.data.contests.ids.toSet(), actual.data.contests.ids.toSet())
    }

    @Test
    fun `should project participants from the participant rows pointing at the competition`() {
        val saved = repository.save(newData())
        val first = fixtures.participant(saved)
        val second = fixtures.participant(saved)
        fixtures.participant()

        val found = assertNotNull(repository.findById(saved.id))

        assertEquals(setOf(first.id, second.id), found.data.participants.ids.toSet())
    }

    @Test
    fun `should ignore participants given on save and update`() {
        val ownerId = fixtures.manager().id.value
        val unrelated = fixtures.participant()

        val saved = repository.save(
            competitionData {
                owner(ownerId)
                name = fixtures.unique("Competition")
                description = "Competition description"
                participants = mutableListOf(unrelated.id)
            },
        )
        val updated = repository.update(saved.withData { participants = mutableListOf(unrelated.id) })

        assertEquals(emptyList(), saved.data.participants.ids)
        assertEquals(emptyList(), updated.data.participants.ids)
        assertEquals(emptyList(), assertNotNull(repository.findById(saved.id)).data.participants.ids)
    }
}
