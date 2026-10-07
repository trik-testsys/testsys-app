package tech.testsys.domain.contract.persistence

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tech.testsys.domain.model.group.CommunityId

class UserFilterTests {

    @Test
    fun `should leave all selection criteria unrestricted by default`() {
        val filter = UserFilter()

        assertEquals(null, filter.name)
        assertEquals(null, filter.roles)
        assertEquals(null, filter.communityId)
    }

    @Test
    fun `should keep provided name roles and community unchanged`() {
        val roles = setOf(UserFilter.Role.OBSERVER, UserFilter.Role.ADMINISTRATOR)

        val filter = UserFilter(name = "  Alpha%_  ", roles = roles, communityId = CommunityId(7))

        assertEquals("  Alpha%_  ", filter.name)
        assertEquals(roles, filter.roles)
        assertEquals(CommunityId(7), filter.communityId)
    }

    @Test
    fun `should reject an empty role set`() {
        assertThrows(IllegalArgumentException::class.java) { UserFilter(roles = emptySet()) }
    }
}
