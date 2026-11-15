package tech.testsys.infra.database.api.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import tech.testsys.domain.builder.api.developerData
import tech.testsys.domain.contract.persistence.repository.MultipleRoleUserRepository
import tech.testsys.domain.model.group.Community
import tech.testsys.domain.model.user.Developer
import tech.testsys.domain.model.user.MultipleRoleUser
import tech.testsys.domain.model.user.Supervisor
import tech.testsys.infra.database.DatabaseIntegrationTests
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class DatabaseServiceSupervisorRoleTests : DatabaseIntegrationTests() {

    @Autowired
    private lateinit var service: ServiceSupervisorRole

    @Autowired
    private lateinit var users: MultipleRoleUserRepository

    private fun publicCommunity(): Community = fixtures.community(owner = fixtures.multipleRoleUser {})

    private fun MultipleRoleUser.supervisorRoles(): List<Supervisor> = data.roles.filterIsInstance<Supervisor>()

    private fun stored(user: MultipleRoleUser): MultipleRoleUser = assertNotNull(users.findById(user.id))

    @Nested
    inner class GrantTests {

        @Test
        fun `should grant the role with the public community membership and keep other roles`() {
            val public = publicCommunity()
            val community = fixtures.community()
            val user = fixtures.multipleRoleUser {
                roles {
                    developer {
                        memberOf(listOf(community.id.value))
                        data = developerData {}
                    }
                }
            }

            val result = service.grant(ServiceUserId(user.id.value), public.id)

            val changed = assertIs<SupervisorRoleChange.Changed>(result).user
            assertEquals(listOf(public.id), stored(user).supervisorRoles().single().memberOf.ids)
            assertEquals(listOf(community.id), stored(user).data.roles.filterIsInstance<Developer>().single().memberOf.ids)
            assertEquals(stored(user).version, changed.version)
        }

        @Test
        fun `should leave a user already holding the role unchanged`() {
            val public = publicCommunity()
            val supervisor = fixtures.supervisor(public)

            val result = service.grant(ServiceUserId(supervisor.id.value), public.id)

            assertEquals(supervisor.version, assertIs<SupervisorRoleChange.Changed>(result).user.version)
            assertEquals(supervisor.version, stored(supervisor).version)
        }

        @Test
        fun `should refuse a missing user`() {
            val public = publicCommunity()

            assertEquals(SupervisorRoleChange.NotFound, service.grant(ServiceUserId(Long.MAX_VALUE), public.id))
        }

        @Test
        fun `should refuse a fixed-role user`() {
            val public = publicCommunity()
            val observer = fixtures.observer(public)

            assertEquals(SupervisorRoleChange.NotAllowed, service.grant(ServiceUserId(observer.id.value), public.id))
        }

        @Test
        fun `should refuse the owner of the public community without granting the role`() {
            val owner = fixtures.multipleRoleUser {}
            val public = fixtures.community(owner = owner)

            val result = service.grant(ServiceUserId(owner.id.value), public.id)

            assertEquals(SupervisorRoleChange.NotAllowed, result)
            assertEquals(emptyList<Supervisor>(), stored(owner).supervisorRoles())
        }
    }

    @Nested
    inner class RevokeTests {

        @Test
        fun `should revoke the role with its memberships and keep other roles`() {
            val public = publicCommunity()
            val community = fixtures.community()
            val user = fixtures.multipleRoleUser {
                roles {
                    developer {
                        memberOf(listOf(public.id.value, community.id.value))
                        data = developerData {}
                    }
                    supervisor { memberOf(listOf(public.id.value)) }
                }
            }

            val result = service.revoke(ServiceUserId(user.id.value), public.id)

            assertEquals(emptyList<Supervisor>(), assertIs<SupervisorRoleChange.Changed>(result).user.supervisorRoles())
            assertEquals(emptyList<Supervisor>(), stored(user).supervisorRoles())
            assertEquals(
                setOf(public.id, community.id),
                stored(user).data.roles.filterIsInstance<Developer>().single().memberOf.ids.toSet(),
            )
        }

        @Test
        fun `should leave a user without the role unchanged`() {
            val public = publicCommunity()
            val user = fixtures.developer()

            val result = service.revoke(ServiceUserId(user.id.value), public.id)

            assertEquals(user.version, assertIs<SupervisorRoleChange.Changed>(result).user.version)
            assertEquals(user.version, stored(user).version)
        }

        @Test
        fun `should refuse a missing user`() {
            val public = publicCommunity()

            assertEquals(SupervisorRoleChange.NotFound, service.revoke(ServiceUserId(Long.MAX_VALUE), public.id))
        }

        @Test
        fun `should refuse a fixed-role user`() {
            val public = publicCommunity()
            val participant = fixtures.participant()

            assertEquals(SupervisorRoleChange.NotAllowed, service.revoke(ServiceUserId(participant.id.value), public.id))
        }
    }
}
