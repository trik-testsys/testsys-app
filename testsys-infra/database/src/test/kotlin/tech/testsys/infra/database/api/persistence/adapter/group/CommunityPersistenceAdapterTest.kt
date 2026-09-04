package tech.testsys.infra.database.api.persistence.adapter.group

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.builder.api.withData
import tech.testsys.domain.contract.persistence.repository.CommunityRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityId
import tech.testsys.infra.database.api.persistence.adapter.PersistenceAdapterContractTest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class CommunityPersistenceAdapterTest : PersistenceAdapterContractTest<CommunityData, CommunityId, Community>() {

    @Autowired
    override lateinit var repository: CommunityRepository

    override fun newData(): CommunityData {
        val ownerId = fixtures.developer().id.value
        return communityData {
            owner(ownerId)
            name = fixtures.unique("Community")
            description = "Community description"
        }
    }

    override fun modified(entity: Community) = entity.withData {
        name = fixtures.unique("Renamed community")
        description = "Updated description"
    }

    override fun idOf(value: Long) = CommunityId(value)

    override fun assertSameData(expected: Community, actual: Community) {
        assertEquals(expected.data.owner.id, actual.data.owner.id)
        assertEquals(expected.data.name, actual.data.name)
        assertEquals(expected.data.description, actual.data.description)
    }

    @Test
    fun `update hands the community over to another owner`() {
        val saved = repository.save(newData())
        val newOwnerId = fixtures.developer().id.value

        repository.update(saved.withData { owner(newOwnerId) })

        assertEquals(newOwnerId, assertNotNull(repository.findById(saved.id)).data.owner.id.value)
    }
}
