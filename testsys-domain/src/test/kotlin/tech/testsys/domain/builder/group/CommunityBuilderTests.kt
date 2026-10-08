package tech.testsys.domain.builder.group

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.domain.builder.DomainEntityBuilderTests
import tech.testsys.domain.builder.api.communityData
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.group.CommunityData
import tech.testsys.domain.model.group.CommunityInviteId

class CommunityBuilderTests : DomainEntityBuilderTests<Community, CommunityData, CommunityDataBuilder>(
    CommunityBuilder(),
    CommunityDataBuilder(),
) {
    override fun buildDataWithAllFields() = listOf(
        communityData {
            owner(42)
            name = "Community"
            description = "Community description"
            managerInvite(10)
            developerInvite(20)
        },
    )

    @Test
    fun `should reference the invites of each role by id`() {
        val data = buildDataWithAllFields().single()

        assertEquals(CommunityInviteId(10), data.managerInvite.id)
        assertEquals(CommunityInviteId(20), data.developerInvite.id)
    }

    @Test
    fun `should throw IllegalArgumentException if the developer invite is not set`() {
        assertThrows(IllegalArgumentException::class.java) {
            communityData {
                owner(42)
                name = "Community"
                description = "Community description"
                managerInvite(10)
            }
        }
    }
}
